package com.example.util

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed class AppUpdateStatus {
    data object Idle : AppUpdateStatus()
    data object Checking : AppUpdateStatus()
    data class UpToDate(
        val currentVersion: String,
        val directUrl: String = AppUpdateChecker.UPDATE_URL
    ) : AppUpdateStatus()

    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val publishedAt: String = ""
    ) : AppUpdateStatus()

    data class ReadyDirectLink(
        val currentVersion: String,
        val downloadUrl: String = AppUpdateChecker.UPDATE_URL
    ) : AppUpdateStatus()

    data class Error(
        val messageAr: String,
        val messageEn: String,
        val fallbackUrl: String = AppUpdateChecker.UPDATE_URL
    ) : AppUpdateStatus()
}

sealed class ApkDownloadState {
    data object Idle : ApkDownloadState()
    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : ApkDownloadState()

    data class Downloaded(val apkFile: File) : ApkDownloadState()
    data class RequiresInstallPermission(val apkFile: File) : ApkDownloadState()
    data class Failed(
        val messageAr: String,
        val messageEn: String,
        val fallbackUrl: String
    ) : ApkDownloadState()
}

internal data class RemoteVersionCandidate(
    val versionLabel: String,
    val buildCode: Int? = null,
    val releaseNotes: String = "",
    val downloadUrl: String? = null,
    val releasePageUrl: String? = null,
    val publishedAt: String = "",
    val tagName: String? = null,
    val isPreRelease: Boolean = false
)

object AppUpdateChecker {

    const val UPDATE_URL = "https://github.com/charfeddine23/Shopy"
    private val REPO_SLUGS = listOf(
        "charfeddine23/Shopy"
    )

    private const val MAX_APK_SIZE_BYTES = 200L * 1024L * 1024L

    private val DOTTED_VERSION_REGEX = Regex("""\d+(?:\.\d+)+""")
    private val SINGLE_V_VERSION_REGEX = Regex("""(?i)(?:^|[^0-9a-z])v(\d{1,3})(?=$|[^0-9a-z.])|^\s*(\d{1,3})\s*$""")
    private val BUILD_CODE_REGEX = Regex("""(?i)(?:\+|build[\s._-]*|code[\s._-]*)(\d+)""")
    private val ENTRY_REGEX = Regex("""<entry>([\s\S]*?)</entry>""", RegexOption.IGNORE_CASE)
    private val EXPANDED_ASSET_APK_REGEX = Regex(
        """href=["'](/[^"']+/releases/download/[^"']+\.apk(?:\?[^"']*)?)["']""",
        RegexOption.IGNORE_CASE
    )

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Validates that update/download URLs strictly use HTTPS and belong to GitHub-owned hosts.
     * Prevents HTTP downgrade, arbitrary third-party redirects, or non-HTTPS scheme injection.
     */
    fun isTrustedUpdateUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (!trimmed.startsWith("https://", ignoreCase = true)) return false
        val host = try {
            java.net.URI(trimmed).host?.lowercase() ?: return false
        } catch (_: Exception) {
            return false
        }
        return host == "github.com" ||
            host == "api.github.com" ||
            host.endsWith(".github.com") ||
            host == "githubusercontent.com" ||
            host.endsWith(".githubusercontent.com") ||
            (host.startsWith("github-production-release-asset-") && host.endsWith(".amazonaws.com"))
    }

    private fun buildRequest(url: String, accept: String? = null): Request {
        return Request.Builder()
            .url(url)
            .header("User-Agent", "ShopyPulse-Android")
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .apply {
                if (!accept.isNullOrBlank()) {
                    header("Accept", accept)
                }
            }
            .build()
    }

    suspend fun checkForUpdate(
        currentVersionName: String,
        currentVersionCode: Int = 1
    ): AppUpdateStatus = withContext(Dispatchers.IO) {
        var anyNetworkReachable = false
        var bestUpToDateUrl: String? = null
        val candidates = mutableListOf<RemoteVersionCandidate>()

        for (slug in REPO_SLUGS) {
            // 1. GitHub Releases API (captures both stable releases AND pre-releases)
            val apiReleasesResult = fetchReleasesFromApi(slug)
            if (apiReleasesResult.reachable) anyNetworkReachable = true
            candidates.addAll(apiReleasesResult.candidates)

            // 2. Also probe /releases/latest API in case of pagination or pinned latest
            val apiLatestResult = fetchLatestReleaseFromApi(slug)
            if (apiLatestResult.reachable) anyNetworkReachable = true
            candidates.addAll(apiLatestResult.candidates)

            // 3. Rate-limit-proof GitHub Atom feed + /releases/latest web redirect (no REST API quota)
            if (apiReleasesResult.candidates.isEmpty() && apiLatestResult.candidates.isEmpty()) {
                val atomResult = fetchReleasesFromAtom(slug)
                if (atomResult.reachable) anyNetworkReachable = true
                candidates.addAll(atomResult.candidates)

                val redirectCandidate = fetchLatestReleaseViaWebRedirect(slug)
                if (redirectCandidate.reachable) anyNetworkReachable = true
                candidates.addAll(redirectCandidate.candidates)
            }

            // Pick highest-version release candidate so far
            val bestRelease = selectBestCandidate(candidates)
            if (bestRelease != null) {
                val apkOrPage = bestRelease.downloadUrl
                    ?: bestRelease.tagName?.let { resolveApkFromExpandedAssets(slug, it) }
                    ?: bestRelease.releasePageUrl
                if (!apkOrPage.isNullOrBlank()) bestUpToDateUrl = apkOrPage

                if (isNewerVersion(bestRelease.versionLabel, currentVersionName, bestRelease.buildCode, currentVersionCode)) {
                    val resolvedDownload = bestRelease.downloadUrl
                        ?: bestRelease.tagName?.let { resolveApkFromExpandedAssets(slug, it) }
                        ?: resolveApkUrlFromGithub(preferredTag = bestRelease.tagName)
                        ?: bestRelease.releasePageUrl?.takeIf { isTrustedUpdateUrl(it) }
                        ?: UPDATE_URL
                    return@withContext AppUpdateStatus.UpdateAvailable(
                        currentVersion = currentVersionName,
                        latestVersion = bestRelease.versionLabel,
                        releaseNotes = bestRelease.releaseNotes,
                        downloadUrl = resolvedDownload,
                        publishedAt = bestRelease.publishedAt
                    )
                }
            }

            // 4. Check ALL GitHub Tags (API + tags.atom fallback) and compare semantically across ALL tags
            val tagCandidates = mutableListOf<String>()
            val apiTags = fetchTagsFromApi(slug)
            if (apiTags.reachable) anyNetworkReachable = true
            tagCandidates.addAll(apiTags.tags)

            if (tagCandidates.isEmpty()) {
                val atomTags = fetchTagsFromAtom(slug)
                if (atomTags.reachable) anyNetworkReachable = true
                tagCandidates.addAll(atomTags.tags)
            }

            val highestTag = extractHighestTag(tagCandidates)
            if (!highestTag.isNullOrBlank()) {
                val tagBuildCode = extractBuildCode(highestTag)
                if (isNewerVersion(highestTag, currentVersionName, tagBuildCode, currentVersionCode)) {
                    val resolvedApk = resolveApkFromExpandedAssets(slug, highestTag)
                        ?: resolveApkUrlFromGithub(preferredTag = highestTag)
                        ?: "https://github.com/$slug/releases/tag/${encodePathSegment(highestTag)}"
                    return@withContext AppUpdateStatus.UpdateAvailable(
                        currentVersion = currentVersionName,
                        latestVersion = highestTag,
                        releaseNotes = "",
                        downloadUrl = resolvedApk
                    )
                }
            }

            // 5. Repo metadata fallback (version.json, app/build.gradle.kts, committed versioned .apk)
            val repoCandidate = fetchRepoMetadataCandidate(slug)
            if (repoCandidate.reachable) anyNetworkReachable = true
            val meta = repoCandidate.candidate
            if (meta != null && isNewerVersion(meta.versionLabel, currentVersionName, meta.buildCode, currentVersionCode)) {
                val resolvedApk = meta.downloadUrl?.takeIf { isTrustedUpdateUrl(it) }
                    ?: resolveApkUrlFromGithub()
                    ?: UPDATE_URL
                return@withContext AppUpdateStatus.UpdateAvailable(
                    currentVersion = currentVersionName,
                    latestVersion = meta.versionLabel,
                    releaseNotes = meta.releaseNotes,
                    downloadUrl = resolvedApk,
                    publishedAt = meta.publishedAt
                )
            }
        }

        if (anyNetworkReachable) {
            return@withContext AppUpdateStatus.UpToDate(
                currentVersion = currentVersionName,
                directUrl = bestUpToDateUrl ?: UPDATE_URL
            )
        }

        // Last connectivity probe against github.com repository URL directly
        try {
            val headReq = buildRequest(UPDATE_URL).newBuilder().head().build()
            client.newCall(headReq).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext AppUpdateStatus.UpToDate(
                        currentVersion = currentVersionName,
                        directUrl = UPDATE_URL
                    )
                }
            }
        } catch (_: Exception) {
            // Fall through to error
        }

        return@withContext AppUpdateStatus.Error(
            messageAr = "تعذر الاتصال بخادم التحديثات. تحقق من اتصال الإنترنت وحاول مجدداً.",
            messageEn = "Unable to reach update server. Check your connection and try again.",
            fallbackUrl = UPDATE_URL
        )
    }

    private data class CandidateFetchResult(
        val reachable: Boolean,
        val candidates: List<RemoteVersionCandidate> = emptyList()
    )

    private data class TagsFetchResult(
        val reachable: Boolean,
        val tags: List<String> = emptyList()
    )

    private data class SingleCandidateResult(
        val reachable: Boolean,
        val candidate: RemoteVersionCandidate? = null
    )

    private fun fetchReleasesFromApi(slug: String): CandidateFetchResult {
        return try {
            val req = buildRequest(
                url = "https://api.github.com/repos/$slug/releases?per_page=15",
                accept = "application/vnd.github+json"
            )
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return CandidateFetchResult(reachable = resp.code == 404)
                }
                val body = resp.body?.string().orEmpty().trim()
                if (!body.startsWith("[")) return CandidateFetchResult(reachable = true)
                val arr = JSONArray(body)
                val out = mutableListOf<RemoteVersionCandidate>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    if (obj.optBoolean("draft", false)) continue
                    parseReleaseJsonObject(obj)?.let(out::add)
                }
                CandidateFetchResult(reachable = true, candidates = out)
            }
        } catch (_: Exception) {
            CandidateFetchResult(reachable = false)
        }
    }

    private fun fetchLatestReleaseFromApi(slug: String): CandidateFetchResult {
        return try {
            val req = buildRequest(
                url = "https://api.github.com/repos/$slug/releases/latest",
                accept = "application/vnd.github+json"
            )
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return CandidateFetchResult(reachable = resp.code == 404)
                }
                val body = resp.body?.string().orEmpty().trim()
                if (!body.startsWith("{")) return CandidateFetchResult(reachable = true)
                val candidate = parseReleaseJsonObject(JSONObject(body))
                CandidateFetchResult(
                    reachable = true,
                    candidates = listOfNotNull(candidate)
                )
            }
        } catch (_: Exception) {
            CandidateFetchResult(reachable = false)
        }
    }

    private fun parseReleaseJsonObject(json: JSONObject): RemoteVersionCandidate? {
        val tagName = json.optString("tag_name", "").trim()
        val releaseName = json.optString("name", "").trim()
        val releaseNotes = json.optString("body", "").trim()
        val publishedAt = json.optString("published_at", "").take(10)
        val htmlUrl = json.optString("html_url", "").trim().takeIf { isTrustedUpdateUrl(it) }
        val isPreRelease = json.optBoolean("prerelease", false)
        val apkAsset = extractApkAssetInfo(json)
        val apkUrl = apkAsset?.second
        val assetName = apkAsset?.first.orEmpty()

        val bestVersionLabel = pickBestVersionLabel(tagName, releaseName, assetName) ?: return null
        val buildCode = extractBuildCode(tagName) ?: extractBuildCode(releaseName) ?: extractBuildCode(assetName)

        return RemoteVersionCandidate(
            versionLabel = bestVersionLabel,
            buildCode = buildCode,
            releaseNotes = releaseNotes,
            downloadUrl = apkUrl,
            releasePageUrl = htmlUrl,
            publishedAt = publishedAt,
            tagName = tagName.ifBlank { null },
            isPreRelease = isPreRelease
        )
    }

    private fun fetchReleasesFromAtom(slug: String): CandidateFetchResult {
        return try {
            val req = buildRequest(
                url = "https://github.com/$slug/releases.atom",
                accept = "application/atom+xml, application/xml, text/xml"
            )
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return CandidateFetchResult(reachable = resp.code == 404)
                }
                val xml = resp.body?.string().orEmpty()
                CandidateFetchResult(
                    reachable = true,
                    candidates = parseReleasesAtom(xml)
                )
            }
        } catch (_: Exception) {
            CandidateFetchResult(reachable = false)
        }
    }

    private fun fetchLatestReleaseViaWebRedirect(slug: String): CandidateFetchResult {
        return try {
            val req = buildRequest("https://github.com/$slug/releases/latest")
            client.newCall(req).execute().use { resp ->
                val finalUrl = resp.request.url.toString()
                if (!isTrustedUpdateUrl(finalUrl)) return CandidateFetchResult(reachable = false)
                val tagMarker = "/releases/tag/"
                val idx = finalUrl.indexOf(tagMarker)
                if (idx >= 0) {
                    val rawTag = finalUrl.substring(idx + tagMarker.length).substringBefore("?").substringBefore("#")
                    val decodedTag = try {
                        URLDecoder.decode(rawTag, "UTF-8")
                    } catch (_: Exception) {
                        rawTag
                    }.trim()
                    if (decodedTag.isNotBlank() && parseVersionNumbers(decodedTag).isNotEmpty()) {
                        return CandidateFetchResult(
                            reachable = true,
                            candidates = listOf(
                                RemoteVersionCandidate(
                                    versionLabel = decodedTag,
                                    buildCode = extractBuildCode(decodedTag),
                                    releasePageUrl = finalUrl,
                                    tagName = decodedTag
                                )
                            )
                        )
                    }
                }
                CandidateFetchResult(reachable = resp.isSuccessful)
            }
        } catch (_: Exception) {
            CandidateFetchResult(reachable = false)
        }
    }

    private fun fetchTagsFromApi(slug: String): TagsFetchResult {
        return try {
            val req = buildRequest(
                url = "https://api.github.com/repos/$slug/tags?per_page=30",
                accept = "application/vnd.github+json"
            )
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return TagsFetchResult(reachable = resp.code == 404)
                }
                val body = resp.body?.string().orEmpty().trim()
                if (!body.startsWith("[")) return TagsFetchResult(reachable = true)
                val arr = JSONArray(body)
                val tags = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val name = arr.optJSONObject(i)?.optString("name", "")?.trim().orEmpty()
                    if (name.isNotBlank()) tags.add(name)
                }
                TagsFetchResult(reachable = true, tags = tags)
            }
        } catch (_: Exception) {
            TagsFetchResult(reachable = false)
        }
    }

    private fun fetchTagsFromAtom(slug: String): TagsFetchResult {
        return try {
            val req = buildRequest(
                url = "https://github.com/$slug/tags.atom",
                accept = "application/atom+xml, application/xml, text/xml"
            )
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return TagsFetchResult(reachable = resp.code == 404)
                }
                val xml = resp.body?.string().orEmpty()
                TagsFetchResult(reachable = true, tags = parseTagsAtom(xml))
            }
        } catch (_: Exception) {
            TagsFetchResult(reachable = false)
        }
    }

    private fun fetchRepoMetadataCandidate(slug: String): SingleCandidateResult {
        var reachable = false
        // 1. Check optional version.json / update.json in repo root
        for (fileName in listOf("version.json", "update.json")) {
            try {
                val req = buildRequest("https://raw.githubusercontent.com/$slug/HEAD/$fileName")
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        reachable = true
                        val parsed = parseVersionJson(resp.body?.string().orEmpty())
                        if (parsed != null) return SingleCandidateResult(reachable = true, candidate = parsed)
                    } else if (resp.code == 404) {
                        reachable = true
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Check app/build.gradle.kts or app/build.gradle on default branch (or main/master)
        val gradlePaths = listOf(
            "HEAD/app/build.gradle.kts",
            "HEAD/app/build.gradle",
            "main/app/build.gradle.kts",
            "master/app/build.gradle.kts"
        )
        for (path in gradlePaths) {
            try {
                val req = buildRequest("https://raw.githubusercontent.com/$slug/$path")
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        reachable = true
                        val content = resp.body?.string().orEmpty()
                        val (vName, vCode) = parseBuildGradleVersion(content)
                        val cleanLabel = vName?.takeIf { it.isNotBlank() } ?: vCode?.let { "Build $it" }
                        if (!cleanLabel.isNullOrBlank()) {
                            return SingleCandidateResult(
                                reachable = true,
                                candidate = RemoteVersionCandidate(
                                    versionLabel = cleanLabel,
                                    buildCode = vCode
                                )
                            )
                        }
                    } else if (resp.code == 404) {
                        reachable = true
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Check committed versioned APK filename in repository tree
        val apkFromRepo = resolveApkInfoFromRepoTree(slug)
        if (apkFromRepo != null) {
            reachable = true
            val (fileName, rawUrl) = apkFromRepo
            if (parseVersionNumbers(fileName).isNotEmpty()) {
                return SingleCandidateResult(
                    reachable = true,
                    candidate = RemoteVersionCandidate(
                        versionLabel = fileName.removeSuffix(".apk"),
                        buildCode = extractBuildCode(fileName),
                        downloadUrl = rawUrl
                    )
                )
            }
        }

        return SingleCandidateResult(reachable = reachable, candidate = null)
    }

    internal fun parseReleasesAtom(xml: String): List<RemoteVersionCandidate> {
        if (xml.isBlank()) return emptyList()
        val out = mutableListOf<RemoteVersionCandidate>()
        for (match in ENTRY_REGEX.findAll(xml)) {
            val block = match.groupValues[1]
            val idText = extractTagText(block, "id").orEmpty()
            val titleText = extractTagText(block, "title").orEmpty()
            val updatedText = extractTagText(block, "updated").orEmpty().take(10)
            val linkHref = Regex("""<link[^>]*href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(block)?.groupValues?.getOrNull(1).orEmpty()

            val tagFromLink = linkHref.substringAfter("/releases/tag/", "")
                .substringBefore("?")
                .substringBefore("#")
                .let { raw ->
                    if (raw.isBlank()) "" else try {
                        URLDecoder.decode(raw, "UTF-8")
                    } catch (_: Exception) {
                        raw
                    }
                }
            val tagFromId = idText.substringAfterLast("/", "")
            val rawTag = tagFromLink.ifBlank { tagFromId }.trim()

            val rawContent = extractTagText(block, "content").orEmpty()
            val cleanNotes = unescapeXmlEntities(rawContent)
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            val label = pickBestVersionLabel(rawTag, titleText) ?: continue
            out.add(
                RemoteVersionCandidate(
                    versionLabel = label,
                    buildCode = extractBuildCode(rawTag) ?: extractBuildCode(titleText),
                    releaseNotes = cleanNotes,
                    releasePageUrl = linkHref.takeIf { isTrustedUpdateUrl(it) },
                    publishedAt = updatedText,
                    tagName = rawTag.ifBlank { null }
                )
            )
        }
        return out
    }

    internal fun parseTagsAtom(xml: String): List<String> {
        if (xml.isBlank()) return emptyList()
        val tags = mutableListOf<String>()
        for (match in ENTRY_REGEX.findAll(xml)) {
            val block = match.groupValues[1]
            val titleText = extractTagText(block, "title").orEmpty().trim()
            val idTag = extractTagText(block, "id").orEmpty().substringAfterLast("/", "").trim()
            val chosen = titleText.ifBlank { idTag }
            if (chosen.isNotBlank()) tags.add(chosen)
        }
        return tags
    }

    internal fun parseVersionJson(content: String): RemoteVersionCandidate? {
        if (content.isBlank() || !content.trim().startsWith("{")) return null
        return try {
            val obj = JSONObject(content)
            val vName = obj.optString("versionName", "")
                .ifBlank { obj.optString("latestVersion", "") }
                .ifBlank { obj.optString("version", "") }
                .trim()
            val vCode = obj.optInt("versionCode", -1).takeIf { it > 0 }
            val notes = obj.optString("releaseNotes", "")
                .ifBlank { obj.optString("notes", "") }
                .trim()
            val rawApkUrl = obj.optString("apkUrl", "")
                .ifBlank { obj.optString("downloadUrl", "") }
                .trim()
                .takeIf { isTrustedUpdateUrl(it) }
            val publishedAt = obj.optString("publishedAt", "").take(10)
            val label = vName.ifBlank { vCode?.let { "Build $it" } ?: return null }
            RemoteVersionCandidate(
                versionLabel = label,
                buildCode = vCode ?: extractBuildCode(label),
                releaseNotes = notes,
                downloadUrl = rawApkUrl,
                publishedAt = publishedAt
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseBuildGradleVersion(content: String): Pair<String?, Int?> {
        if (content.isBlank()) return null to null
        var versionName: String? = null
        var versionCode: Int? = null

        val nameAssignRegex = Regex("""^\s*versionName\s*(?:=|\s)\s*(.+)$""")
        val codeAssignRegex = Regex("""^\s*versionCode\s*(?:=|\s)\s*(.+)$""")
        val quotedRegex = Regex("""["']([^"']+)["']""")
        val intRegex = Regex("""\b(\d+)\b""")

        for (rawLine in content.lineSequence()) {
            val line = rawLine.substringBefore("//").trim()
            if (line.isEmpty() || line.startsWith("/*") || line.startsWith("*")) continue

            if (versionName == null) {
                nameAssignRegex.find(line)?.let { m ->
                    val rhs = m.groupValues[1]
                    val targetSegment = if (rhs.contains("?:")) rhs.substringAfterLast("?:") else rhs
                    val matches = quotedRegex.findAll(targetSegment).map { it.groupValues[1].trim() }.toList()
                    val candidate = matches.lastOrNull {
                        it.isNotBlank() &&
                            !it.equals("APP_VERSION_NAME", ignoreCase = true) &&
                            parseVersionNumbers(it).isNotEmpty()
                    } ?: matches.lastOrNull { it.isNotBlank() && !it.equals("APP_VERSION_NAME", ignoreCase = true) }
                    if (!candidate.isNullOrBlank()) {
                        versionName = candidate
                    }
                }
            }

            if (versionCode == null) {
                codeAssignRegex.find(line)?.let { m ->
                    val rhs = m.groupValues[1]
                    val targetSegment = if (rhs.contains("?:")) rhs.substringAfterLast("?:") else rhs
                    val code = intRegex.findAll(targetSegment).lastOrNull()?.groupValues?.get(1)?.toIntOrNull()
                    if (code != null && code > 0) {
                        versionCode = code
                    }
                }
            }
        }
        return versionName to versionCode
    }

    internal fun extractApkUrlsFromExpandedAssetsHtml(html: String): List<String> {
        if (html.isBlank()) return emptyList()
        return EXPANDED_ASSET_APK_REGEX.findAll(html)
            .map { "https://github.com${it.groupValues[1]}" }
            .filter { isTrustedUpdateUrl(it) }
            .distinct()
            .toList()
    }

    internal fun extractHighestTag(tags: Collection<String>): String? {
        var bestTag: String? = null
        var bestParts: List<Int> = emptyList()
        var bestCode: Int = -1

        for (rawTag in tags) {
            val tag = rawTag.trim()
            if (tag.isBlank()) continue
            val parts = parseVersionNumbers(tag)
            val code = extractBuildCode(tag) ?: -1
            if (parts.isEmpty() && code <= 0) continue

            if (bestTag == null) {
                bestTag = tag
                bestParts = parts
                bestCode = code
            } else {
                val cmp = compareVersionParts(parts, bestParts)
                if (cmp > 0 || (cmp == 0 && code > bestCode)) {
                    bestTag = tag
                    bestParts = parts
                    bestCode = code
                }
            }
        }
        return bestTag
    }

    private fun selectBestCandidate(candidates: List<RemoteVersionCandidate>): RemoteVersionCandidate? {
        var best: RemoteVersionCandidate? = null
        for (c in candidates) {
            val cParts = parseVersionNumbers(c.versionLabel)
            val cCode = c.buildCode ?: -1
            if (cParts.isEmpty() && cCode <= 0) continue

            val curBest = best
            if (curBest == null) {
                best = c
                continue
            }
            val bParts = parseVersionNumbers(curBest.versionLabel)
            val bCode = curBest.buildCode ?: -1
            val cmp = compareVersionParts(cParts, bParts)
            when {
                cmp > 0 -> best = c
                cmp == 0 && cCode > bCode -> best = c
                cmp == 0 && cCode == bCode -> {
                    val preferNew = (!c.isPreRelease && curBest.isPreRelease) ||
                        (c.isPreRelease == curBest.isPreRelease && c.downloadUrl != null && curBest.downloadUrl == null)
                    if (preferNew) best = c
                }
            }
        }
        return best
    }

    private fun pickBestVersionLabel(vararg rawOptions: String): String? {
        for (opt in rawOptions) {
            val trimmed = opt.trim()
            if (trimmed.isNotBlank() && parseVersionNumbers(trimmed).isNotEmpty()) {
                return trimmed
            }
        }
        return rawOptions.firstOrNull { it.isNotBlank() }?.trim()
    }

    private fun extractTagText(xml: String, tagName: String): String? {
        val regex = Regex("""<$tagName(?:\s[^>]*)?>([\s\S]*?)</$tagName>""", RegexOption.IGNORE_CASE)
        val raw = regex.find(xml)?.groupValues?.getOrNull(1) ?: return null
        return unescapeXmlEntities(raw.removePrefix("<![CDATA[").removeSuffix("]]>")).trim()
    }

    private fun unescapeXmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
    }

    private fun encodePathSegment(segment: String): String {
        return URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
    }

    private fun resolveApkFromExpandedAssets(slug: String, tag: String): String? {
        if (tag.isBlank()) return null
        return try {
            val encodedTag = encodePathSegment(tag)
            val req = buildRequest("https://github.com/$slug/releases/expanded_assets/$encodedTag")
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val html = resp.body?.string().orEmpty()
                extractApkUrlsFromExpandedAssetsHtml(html).firstOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractApkAssetUrl(releaseJson: JSONObject): String? {
        return extractApkAssetInfo(releaseJson)?.second
    }

    private fun extractApkAssetInfo(releaseJson: JSONObject): Pair<String, String>? {
        val assets = releaseJson.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val assetName = asset.optString("name", "")
            val browserUrl = asset.optString("browser_download_url", "")
            if (assetName.endsWith(".apk", ignoreCase = true) && isTrustedUpdateUrl(browserUrl)) {
                return assetName to browserUrl
            }
        }
        return null
    }

    /**
     * Resolves a direct downloadable .apk binary URL from GitHub Releases or repo contents
     * so downloading happens strictly inside the app without ever opening a browser.
     */
    private fun resolveApkUrlFromGithub(preferredTag: String? = null): String? {
        for (slug in REPO_SLUGS) {
            if (!preferredTag.isNullOrBlank()) {
                resolveApkFromExpandedAssets(slug, preferredTag)?.let { return it }
            }

            // Try recent releases list
            try {
                val req = buildRequest(
                    url = "https://api.github.com/repos/$slug/releases?per_page=10",
                    accept = "application/vnd.github+json"
                )
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank() && body.trim().startsWith("[")) {
                            val arr = JSONArray(body)
                            if (!preferredTag.isNullOrBlank()) {
                                for (i in 0 until arr.length()) {
                                    val rel = arr.optJSONObject(i) ?: continue
                                    if (rel.optString("tag_name", "").equals(preferredTag, ignoreCase = true)) {
                                        extractApkAssetUrl(rel)?.let { return it }
                                    }
                                }
                            }
                            for (i in 0 until arr.length()) {
                                val rel = arr.optJSONObject(i) ?: continue
                                if (rel.optBoolean("draft", false)) continue
                                extractApkAssetUrl(rel)?.let { return it }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            // Try root repository contents for any committed .apk binary
            try {
                val req = buildRequest(
                    url = "https://api.github.com/repos/$slug/contents",
                    accept = "application/vnd.github+json"
                )
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank() && body.trim().startsWith("[")) {
                            val arr = JSONArray(body)
                            for (i in 0 until arr.length()) {
                                val item = arr.optJSONObject(i) ?: continue
                                val name = item.optString("name", "")
                                val rawUrl = item.optString("download_url", "")
                                if (name.endsWith(".apk", ignoreCase = true) && isTrustedUpdateUrl(rawUrl)) {
                                    return rawUrl
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            resolveApkInfoFromRepoTree(slug)?.second?.let { return it }
        }
        return null
    }

    private fun resolveApkInfoFromRepoTree(slug: String): Pair<String, String>? {
        return try {
            val req = buildRequest(
                url = "https://api.github.com/repos/$slug/git/trees/HEAD?recursive=1",
                accept = "application/vnd.github+json"
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val obj = JSONObject(body)
                        val tree = obj.optJSONArray("tree") ?: return null
                        for (i in 0 until tree.length()) {
                            val entry = tree.optJSONObject(i) ?: continue
                            val type = entry.optString("type", "")
                            val path = entry.optString("path", "")
                            if (type == "blob" && path.endsWith(".apk", ignoreCase = true)) {
                                val candidate = "https://raw.githubusercontent.com/$slug/HEAD/$path"
                                if (isTrustedUpdateUrl(candidate)) {
                                    val name = path.substringAfterLast("/")
                                    return name to candidate
                                }
                            }
                        }
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Streams the APK directly inside the app into cache/updates/ with real-time progress.
     */
    fun downloadApkInApp(
        context: Context,
        downloadUrl: String,
        versionLabel: String
    ): Flow<ApkDownloadState> = flow {
        emit(ApkDownloadState.Downloading(progressPercent = 0, downloadedBytes = 0L, totalBytes = -1L))

        val updatesDir = File(context.cacheDir, "updates").apply {
            if (!exists()) mkdirs()
            // Clean previous cached APKs
            listFiles()?.forEach { oldFile -> oldFile.delete() }
        }

        val safeVersion = versionLabel.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "latest" }
        val targetFile = File(updatesDir, "Shopy.Pulse.$safeVersion.apk")

        try {
            if (!targetFile.canonicalPath.startsWith(updatesDir.canonicalPath + File.separator)) {
                emit(
                    ApkDownloadState.Failed(
                        messageAr = "مسار حفظ ملف التحديث غير صالح.",
                        messageEn = "Invalid update destination path.",
                        fallbackUrl = UPDATE_URL
                    )
                )
                return@flow
            }

            // If URL is a repository/tag web page rather than a direct .apk download, resolve the real APK asset
            val looksLikeDirectApk = downloadUrl.substringBefore("?").endsWith(".apk", ignoreCase = true) ||
                downloadUrl.contains("/releases/download/", ignoreCase = true)
            val effectiveUrl = if (!looksLikeDirectApk) {
                resolveApkUrlFromGithub(preferredTag = versionLabel)
            } else {
                downloadUrl
            }

            if (effectiveUrl.isNullOrBlank()) {
                val safePageFallback = if (isTrustedUpdateUrl(downloadUrl)) downloadUrl else UPDATE_URL
                emit(
                    ApkDownloadState.Failed(
                        messageAr = "لم يتم العثور على ملف APK مباشر في مستودع التحديثات بعد.",
                        messageEn = "No direct APK file was found in the update repository yet.",
                        fallbackUrl = safePageFallback
                    )
                )
                return@flow
            }

            if (!isTrustedUpdateUrl(effectiveUrl)) {
                emit(
                    ApkDownloadState.Failed(
                        messageAr = "رابط التحديث غير موثوق أو لا يستخدم HTTPS.",
                        messageEn = "Untrusted or non-HTTPS update URL rejected.",
                        fallbackUrl = UPDATE_URL
                    )
                )
                return@flow
            }

            val request = Request.Builder()
                .url(effectiveUrl)
                .header("User-Agent", "ShopyPulse-Android")
                .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                .header("Accept-Encoding", "identity")
                .build()

            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                if (!isTrustedUpdateUrl(finalUrl)) {
                    emit(
                        ApkDownloadState.Failed(
                            messageAr = "تم حظر إعادة توجيه التحديث إلى مصدر غير موثوق.",
                            messageEn = "Blocked redirect to untrusted update host.",
                            fallbackUrl = UPDATE_URL
                        )
                    )
                    return@flow
                }

                if (!response.isSuccessful) {
                    emit(
                        ApkDownloadState.Failed(
                            messageAr = "تعذر تحميل ملف التحديث (رمز ${response.code})",
                            messageEn = "Failed to download update (HTTP ${response.code})",
                            fallbackUrl = effectiveUrl
                        )
                    )
                    return@flow
                }

                val body = response.body
                if (body == null) {
                    emit(
                        ApkDownloadState.Failed(
                            messageAr = "ملف التحديث فارغ أو غير متاح",
                            messageEn = "Update package is empty or unavailable",
                            fallbackUrl = effectiveUrl
                        )
                    )
                    return@flow
                }

                val contentType = response.header("Content-Type") ?: ""
                if (contentType.contains("text/html", ignoreCase = true)) {
                    emit(
                        ApkDownloadState.Failed(
                            messageAr = "لم يتم العثور على ملف APK مباشر في هذا الإصدار بعد.",
                            messageEn = "No direct APK release asset was found yet.",
                            fallbackUrl = effectiveUrl
                        )
                    )
                    return@flow
                }

                val totalBytes = body.contentLength()
                if (totalBytes > MAX_APK_SIZE_BYTES) {
                    emit(
                        ApkDownloadState.Failed(
                            messageAr = "حجم ملف التحديث يتجاوز الحد الآمن المسموح.",
                            messageEn = "Update file exceeds safe maximum size.",
                            fallbackUrl = effectiveUrl
                        )
                    )
                    return@flow
                }

                var downloadedBytes = 0L
                var lastEmittedPercent = -1
                var lastEmittedBytes = 0L
                val buffer = ByteArray(16 * 1024)
                var checkedMagic = false

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break

                            if (downloadedBytes + read > MAX_APK_SIZE_BYTES) {
                                targetFile.delete()
                                emit(
                                    ApkDownloadState.Failed(
                                        messageAr = "تجاوز التحميل الحد الأقصى المسموح به.",
                                        messageEn = "Download exceeded maximum allowed APK size.",
                                        fallbackUrl = effectiveUrl
                                    )
                                )
                                return@flow
                            }

                            // Validate APK/ZIP magic bytes ('P', 'K') on first chunk
                            if (!checkedMagic && read >= 2) {
                                checkedMagic = true
                                if (buffer[0] != 0x50.toByte() || buffer[1] != 0x4B.toByte()) {
                                    targetFile.delete()
                                    emit(
                                        ApkDownloadState.Failed(
                                            messageAr = "الملف المستلم ليس حزمة APK صالحة.",
                                            messageEn = "Downloaded file is not a valid APK package.",
                                            fallbackUrl = effectiveUrl
                                        )
                                    )
                                    return@flow
                                }
                            }

                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            val percent = if (totalBytes > 0) {
                                ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }

                            val shouldEmit = if (percent >= 0) {
                                percent != lastEmittedPercent
                            } else {
                                downloadedBytes - lastEmittedBytes >= 131_072L
                            }

                            if (shouldEmit) {
                                lastEmittedPercent = percent
                                lastEmittedBytes = downloadedBytes
                                emit(
                                    ApkDownloadState.Downloading(
                                        progressPercent = percent,
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalBytes
                                    )
                                )
                            }
                        }
                        output.flush()
                    }
                }

                // Validate that the downloaded file is a parseable Android APK archive
                val archiveInfo = context.packageManager.getPackageArchiveInfo(targetFile.absolutePath, 0)
                if (archiveInfo == null || archiveInfo.packageName.isNullOrBlank()) {
                    targetFile.delete()
                    emit(
                        ApkDownloadState.Failed(
                            messageAr = "الملف المحمل تالف أو ليس حزمة تطبيق أندرويد صالحة.",
                            messageEn = "Downloaded file is corrupted or not a valid Android APK.",
                            fallbackUrl = effectiveUrl
                        )
                    )
                    return@flow
                }

                emit(
                    ApkDownloadState.Downloading(
                        progressPercent = 100,
                        downloadedBytes = downloadedBytes,
                        totalBytes = if (totalBytes > 0) totalBytes else downloadedBytes
                    )
                )
                emit(ApkDownloadState.Downloaded(targetFile))
            }
        } catch (e: Exception) {
            if (targetFile.exists()) targetFile.delete()
            emit(
                ApkDownloadState.Failed(
                    messageAr = "تعذر إكمال التحميل: ${e.localizedMessage ?: "خطأ في الشبكة"}",
                    messageEn = "Download failed: ${e.localizedMessage ?: "Network error"}",
                    fallbackUrl = if (isTrustedUpdateUrl(downloadUrl)) downloadUrl else UPDATE_URL
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    fun openInstallPermissionSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val permIntent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return try {
            context.startActivity(permIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Triggers Android's package installer for the downloaded APK using FileProvider.
     * Returns true if installer intent was launched, false if launch failed or permission is needed.
     */
    fun triggerApkInstall(context: Context, apkFile: File): Boolean {
        val updatesDir = File(context.cacheDir, "updates")
        val isInsideUpdatesDir = try {
            apkFile.exists() &&
                apkFile.isFile &&
                apkFile.canonicalPath.startsWith(updatesDir.canonicalPath + File.separator)
        } catch (_: Exception) {
            false
        }
        if (!isInsideUpdatesDir) return false

        val archiveInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
        if (archiveInfo == null || archiveInfo.packageName.isNullOrBlank()) {
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            openInstallPermissionSettings(context)
            return false
        }

        val apkUri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } catch (_: Exception) {
            return false
        }

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            clipData = ClipData.newRawUri("", apkUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            context.startActivity(viewIntent)
            return true
        } catch (_: Exception) {
            @Suppress("DEPRECATION")
            val installPkgIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = apkUri
                clipData = ClipData.newRawUri("", apkUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, false)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            return try {
                context.startActivity(installPkgIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Compares semantic versions like "v1.2.0" vs "1.0", "Shopy-v1.1" vs "1.0",
     * or optional build numbers when version names match.
     */
    @JvmOverloads
    fun isNewerVersion(
        remote: String,
        current: String,
        remoteBuildCode: Int? = null,
        currentBuildCode: Int? = null
    ): Boolean {
        val remoteParts = parseVersionNumbers(remote)
        val currentParts = parseVersionNumbers(current)

        if (remoteParts.isNotEmpty() && currentParts.isNotEmpty()) {
            val cmp = compareVersionParts(remoteParts, currentParts)
            if (cmp > 0) return true
            if (cmp < 0) return false
        } else if (remoteParts.isNotEmpty()) {
            return true
        }

        val rCode = remoteBuildCode ?: extractBuildCode(remote)
        val cCode = currentBuildCode ?: extractBuildCode(current)
        if (rCode != null && cCode != null && rCode > cCode) {
            return true
        }

        return false
    }

    internal fun compareVersionParts(a: List<Int>, b: List<Int>): Int {
        if (a.isEmpty() && b.isEmpty()) return 0
        val maxLen = maxOf(a.size, b.size)
        for (i in 0 until maxLen) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (av != bv) return av.compareTo(bv)
        }
        return 0
    }

    internal fun extractBuildCode(version: String): Int? {
        return BUILD_CODE_REGEX.find(version)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    internal fun parseVersionNumbers(version: String): List<Int> {
        val trimmed = version.trim()
        if (trimmed.isEmpty()) return emptyList()

        val dottedMatch = DOTTED_VERSION_REGEX.find(trimmed)
        if (dottedMatch != null) {
            return dottedMatch.value.split(".").mapNotNull { it.toIntOrNull() }
        }

        val singleMatch = SINGLE_V_VERSION_REGEX.find(trimmed)
        if (singleMatch != null) {
            val digitToken = singleMatch.groupValues[1].ifBlank { singleMatch.groupValues.getOrNull(2).orEmpty() }
            val num = digitToken.toIntOrNull()
            if (num != null) return listOf(num)
        }

        return emptyList()
    }
}
