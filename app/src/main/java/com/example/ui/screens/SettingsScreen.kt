package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SwipeVertical
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.components.AppleLiquidGlassTopBarLayout
import com.example.ui.components.CircularRevealThemeOverlay
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.QrShareDialog
import com.example.ui.components.ShopyDialog
import com.example.ui.components.SouqSectionHeader
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.CyanTintBg
import com.example.ui.theme.CyanTintBorder
import com.example.ui.theme.CyanTintText
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.MintTintText
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletDark
import com.example.ui.viewmodel.ShoppingViewModel
import com.example.util.ApkDownloadState
import com.example.util.AppUpdateStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun SettingsScreen(
    viewModel: ShoppingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isArabic by viewModel.isArabic.collectAsStateWithLifecycle()
    val isDarkModePref by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isAutoThemeEnabled by viewModel.isAutoThemeEnabled.collectAsStateWithLifecycle()
    val systemInDark = isSystemInDarkTheme()
    val isEffectiveDarkMode = if (isAutoThemeEnabled) systemInDark else isDarkModePref
    val lastBackupDate by viewModel.lastBackupDate.collectAsStateWithLifecycle()
    val isDynamicGlowEnabled by viewModel.isDynamicGlowEnabled.collectAsStateWithLifecycle()
    val isLiquidDockSwipeEnabled by viewModel.isLiquidDockSwipeEnabled.collectAsStateWithLifecycle()
    val isFullScreenEnabled by viewModel.isFullScreenEnabled.collectAsStateWithLifecycle()
    val isTwoFingerFullScreenSwipeEnabled by viewModel.isTwoFingerFullScreenSwipeEnabled.collectAsStateWithLifecycle()
    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()
    val apkDownloadState by viewModel.apkDownloadState.collectAsStateWithLifecycle()

    var showQrDialog by remember { mutableStateOf(false) }
    var qrDialogInitialMode by remember { mutableStateOf(false) }
    var showDriveDialog by remember { mutableStateOf(false) }
    val ioScope = rememberCoroutineScope()

    var revealTargetDark by remember { mutableStateOf<Boolean?>(null) }
    var revealTargetAuto by remember { mutableStateOf<Boolean?>(null) }
    var switchCenterOffset by remember { mutableStateOf(Offset.Zero) }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            ioScope.launch {
                val payload = viewModel.generateGoogleDriveBackupPayload()
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(payload.toByteArray(Charsets.UTF_8))
                            outputStream.flush()
                        } ?: error("Output stream unavailable")
                    }.isSuccess
                }
                if (saved) {
                    viewModel.markGoogleDriveBackupDone()
                } else {
                    viewModel.emitUserMessage(
                        if (isArabic) "تعذر حفظ ملف النسخ الاحتياطي" else "Unable to save backup file"
                    )
                }
            }
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            ioScope.launch {
                val readResult = withContext(Dispatchers.IO) {
                    runCatching {
                        val maxBytes = 2 * 1024 * 1024
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val buffer = ByteArray(8192)
                            val out = ByteArrayOutputStream()
                            var totalRead = 0
                            while (true) {
                                val read = inputStream.read(buffer)
                                if (read == -1) break
                                totalRead += read
                                if (totalRead > maxBytes) {
                                    throw IllegalArgumentException("Backup file too large")
                                }
                                out.write(buffer, 0, read)
                            }
                            out.toString(Charsets.UTF_8.name())
                        }
                    }.getOrNull()
                }
                if (readResult != null) {
                    viewModel.restoreFromBackupPayload(readResult)
                } else {
                    viewModel.emitUserMessage(
                        if (isArabic) "ملف النسخة الاحتياطية غير صالح أو كبير جداً" else "Backup file is invalid or too large"
                    )
                }
            }
        }
    }

    val listState = rememberLazyListState()
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight

    AppleLiquidGlassTopBarLayout(
        listState = listState,
        modifier = modifier.fillMaxSize(),
        defaultHeaderHeight = 68.dp,
        topBar = {
            SettingsScreenHeader(isArabic = isArabic)
        }
    ) { topPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding + 4.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Section 1: Language & Theme Mode (Unified Apple Glass Card)
            item {
                val themeAccent = when {
                    isAutoThemeEnabled -> EmeraldSavings
                    isEffectiveDarkMode -> ElectricCyan
                    else -> NeonAmber
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SouqSectionHeader(
                        icon = Icons.Default.Palette,
                        title = if (isArabic) "اللغة والمظهر" else "Language & Appearance",
                        subtitle = if (isArabic) "تخصيص لغة التطبيق والسمة البصرية" else "Display language and color theme",
                        accentColor = if (isLight) CyanTintText else ElectricCyan
                    )

                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        glowColor = themeAccent.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CompactLanguagePill(
                                    title = "العربية",
                                    isSelected = isArabic,
                                    onClick = { viewModel.setLanguage(true) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("language_arabic")
                                )
                                CompactLanguagePill(
                                    title = "English",
                                    isSelected = !isArabic,
                                    onClick = { viewModel.setLanguage(false) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("language_english")
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            SettingsGroupDivider(isLight = isLight)
                            Spacer(modifier = Modifier.height(10.dp))

                            ThemeModeRow(
                                isDarkMode = isEffectiveDarkMode,
                                isAutoTheme = isAutoThemeEnabled,
                                isArabic = isArabic,
                                accentColor = themeAccent,
                                onDarkModeChange = { darkSelected ->
                                    if (revealTargetDark == null) {
                                        if (darkSelected != isEffectiveDarkMode) {
                                            revealTargetAuto = false
                                            revealTargetDark = darkSelected
                                        } else {
                                            viewModel.setAutoThemeEnabled(false)
                                            viewModel.setDarkMode(darkSelected)
                                        }
                                    }
                                },
                                onAutoToggle = {
                                    if (revealTargetDark == null) {
                                        val nextAuto = !isAutoThemeEnabled
                                        if (nextAuto) {
                                            if (systemInDark != isEffectiveDarkMode) {
                                                revealTargetAuto = true
                                                revealTargetDark = systemInDark
                                            } else {
                                                viewModel.setAutoThemeEnabled(true)
                                            }
                                        } else {
                                            viewModel.setDarkMode(isEffectiveDarkMode)
                                            viewModel.setAutoThemeEnabled(false)
                                        }
                                    }
                                },
                                onSwitchPositionChanged = { center ->
                                    switchCenterOffset = center
                                }
                            )
                        }
                    }
                }
            }

            // Section 2: Visual Effects & Smart Gestures (All 4 toggles in one card)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SouqSectionHeader(
                        icon = Icons.Default.AutoAwesome,
                        title = if (isArabic) "التأثيرات والإيماءات الذكية" else "Visuals & Smart Gestures",
                        subtitle = if (isArabic) "تخصيص الحركة والانسيابية وملء الشاشة" else "Fluid animations, water physics & gestures",
                        accentColor = if (isLight) VioletDark else VioletAccent
                    )

                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        glowColor = if (isLight) VioletDark.copy(alpha = 0.35f) else ElectricCyan.copy(alpha = 0.35f)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingsToggleRow(
                                icon = Icons.Default.AutoAwesome,
                                title = if (isArabic) "التأثير الحركي لاسم التطبيق" else "App Title Dynamic Glow",
                                subtitle = if (isArabic) "تدرّج ضوئي متدفق في الشريط العلوي" else "Subtle animated specular header glow",
                                checked = isDynamicGlowEnabled,
                                accentColor = ElectricCyan,
                                onCheckedChange = { viewModel.setDynamicGlowEnabled(it) },
                                switchTestTag = "dynamic_glow_switch"
                            )

                            SettingsGroupDivider(isLight = isLight, horizontalPadding = 14.dp)

                            SettingsToggleRow(
                                icon = Icons.Default.WaterDrop,
                                title = if (isArabic) "فيزياء المياه في الدوك السفلي" else "Bottom Dock Fluid Water",
                                subtitle = if (isArabic) "تموجات المياه والتنقل بالسحب عبر الدوك" else "Water ripples & fluid swipe navigation",
                                checked = isLiquidDockSwipeEnabled,
                                accentColor = ElectricCyan,
                                onCheckedChange = { viewModel.setLiquidDockSwipeEnabled(it) },
                                switchTestTag = "liquid_dock_swipe_switch"
                            )

                            SettingsGroupDivider(isLight = isLight, horizontalPadding = 14.dp)

                            SettingsToggleRow(
                                icon = if (isFullScreenEnabled) Icons.Default.Fullscreen else Icons.Default.FullscreenExit,
                                title = if (isArabic) "وضع ملء الشاشة" else "Full Screen Mode",
                                subtitle = if (isArabic) "إخفاء شريط الهاتف العلوي بالكامل" else "Hide top status bar completely",
                                checked = isFullScreenEnabled,
                                accentColor = EmeraldSavings,
                                onCheckedChange = { viewModel.setFullScreenEnabled(it) },
                                switchTestTag = "full_screen_switch"
                            )

                            SettingsGroupDivider(isLight = isLight, horizontalPadding = 14.dp)

                            SettingsToggleRow(
                                icon = Icons.Default.SwipeVertical,
                                title = if (isArabic) "السحب بإصبعين لملء الشاشة" else "Two-Finger Full Screen Swipe",
                                subtitle = if (isArabic) "اسحب بإصبعين للأعلى للتفعيل وللأسفل للإلغاء" else "Swipe 2 fingers up to enable, down to exit",
                                checked = isTwoFingerFullScreenSwipeEnabled,
                                accentColor = EmeraldSavings,
                                onCheckedChange = { viewModel.setTwoFingerFullScreenSwipeEnabled(it) },
                                switchTestTag = "two_finger_fullscreen_swipe_switch"
                            )
                        }
                    }
                }
            }

            // Section 3: Sync, Cloud Backup, Updates & Tutorial
            item {
                val isChecking = updateStatus is AppUpdateStatus.Checking
                val availableUpdate = updateStatus as? AppUpdateStatus.UpdateAvailable
                val cleanVersion = availableUpdate?.latestVersion?.trim()?.removePrefix("v")?.removePrefix("V").orEmpty()
                val updateTitle = when (apkDownloadState) {
                    is ApkDownloadState.Downloading -> {
                        val s = apkDownloadState as ApkDownloadState.Downloading
                        val pct = if (s.progressPercent >= 0) "${s.progressPercent}%" else "..."
                        if (isArabic) "جاري تحميل التحديث ($pct)" else "Downloading Update ($pct)"
                    }
                    is ApkDownloadState.Downloaded,
                    is ApkDownloadState.RequiresInstallPermission ->
                        if (isArabic) "التحديث جاهز للتثبيت" else "Update Ready to Install"
                    else -> when (updateStatus) {
                        is AppUpdateStatus.Checking -> if (isArabic) "جاري التحقق..." else "Checking..."
                        is AppUpdateStatus.UpdateAvailable ->
                            if (isArabic) "تحديث جديد متاح ($cleanVersion)" else "New Update Available ($cleanVersion)"
                        is AppUpdateStatus.Error ->
                            if (isArabic) "تعذر التحقق • اضغط للمحاولة" else "Check failed • Tap to retry"
                        is AppUpdateStatus.Idle ->
                            if (isArabic) "التحقق من وجود تحديثات" else "Check for Updates"
                        else ->
                            if (isArabic) "التطبيق محدث إلى آخر نسخة" else "App is up to date"
                    }
                }
                val updateAccent = when {
                    isChecking -> ElectricCyan
                    availableUpdate != null || updateStatus is AppUpdateStatus.Error -> NeonAmber
                    updateStatus is AppUpdateStatus.Idle -> ElectricCyan
                    else -> EmeraldSavings
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SouqSectionHeader(
                        icon = Icons.Default.CloudDone,
                        title = if (isArabic) "المزامنة، النسخ السحابي والتحديثات" else "Sync, Backup & Updates",
                        subtitle = if (isArabic) "مشاركة القوائم والنسخ الاحتياطي وإصدار التطبيق" else "Share lists, cloud backup & app version",
                        accentColor = if (isLight) MintTintText else EmeraldSavings
                    )

                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        glowColor = EmeraldSavings.copy(alpha = 0.35f)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingsActionRow(
                                icon = Icons.Default.QrCode,
                                title = if (isArabic) "نقل ومشاركة القوائم (QR Code)" else "Transfer & Share Lists via QR",
                                subtitle = if (isArabic) "مشاركة فورية بدون إنترنت بين الأجهزة" else "Instant offline transfer between phones",
                                accentColor = if (isLight) MintTintText else EmeraldSavings,
                                onClick = {
                                    qrDialogInitialMode = false
                                    showQrDialog = true
                                },
                                testTag = "open_qr_transfer_button"
                            )

                            SettingsGroupDivider(isLight = isLight, horizontalPadding = 14.dp)

                            SettingsActionRow(
                                icon = Icons.Default.CloudDone,
                                title = if (isArabic) "النسخ الاحتياطي (Google Drive)" else "Google Drive Backup",
                                subtitle = if (lastBackupDate.isNotBlank()) {
                                    if (isArabic) "آخر نسخة: $lastBackupDate" else "Last backup: $lastBackupDate"
                                } else {
                                    if (isArabic) "حفظ واستعادة القوائم والأسعار" else "Export or restore lists & prices"
                                },
                                accentColor = if (isLight) CyanTintText else ElectricCyan,
                                onClick = { showDriveDialog = true },
                                testTag = "open_google_drive_button"
                            )

                            SettingsGroupDivider(isLight = isLight, horizontalPadding = 14.dp)

                            SettingsActionRow(
                                icon = Icons.Default.SystemUpdate,
                                title = updateTitle,
                                subtitle = if (isArabic) "الإصدار الحالي v${BuildConfig.VERSION_NAME}" else "Current version v${BuildConfig.VERSION_NAME}",
                                accentColor = updateAccent,
                                isLoading = isChecking,
                                onClick = {
                                    when {
                                        apkDownloadState is ApkDownloadState.Downloaded ||
                                            apkDownloadState is ApkDownloadState.RequiresInstallPermission -> {
                                            viewModel.installDownloadedApk(context)
                                        }
                                        updateStatus is AppUpdateStatus.UpdateAvailable -> {
                                            viewModel.openUpdateOverlayIfAvailable()
                                        }
                                        else -> {
                                            viewModel.checkForUpdates(autoOpenOverlay = true, userInitiated = true)
                                        }
                                    }
                                },
                                testTag = "open_app_update_button"
                            )

                            SettingsGroupDivider(isLight = isLight, horizontalPadding = 14.dp)

                            SettingsActionRow(
                                icon = Icons.Default.AutoAwesome,
                                title = if (isArabic) "الدليل التفاعلي لشرح التطبيق" else "Interactive App Tutorial",
                                subtitle = if (isArabic) "جولة سريعة لاستكشاف جميع المزايا" else "Guided walkthrough of all features",
                                accentColor = if (isLight) VioletDark else VioletAccent,
                                onClick = { viewModel.openTutorial() },
                                testTag = "open_tutorial_button"
                            )
                        }
                    }
                }
            }

            // Section 4: App & Developer Info Section
            item {
                DeveloperInfoSection(isArabic = isArabic)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showQrDialog) {
            QrShareDialog(
                payload = viewModel.getQrSharePayload(),
                isArabic = isArabic,
                initialImportMode = qrDialogInitialMode,
                onDismiss = { showQrDialog = false },
                onImportPayload = { payload ->
                    viewModel.importListFromQr(payload)
                }
            )
        }

        if (showDriveDialog) {
            GoogleDriveDialog(
                isArabic = isArabic,
                lastBackupDate = lastBackupDate,
                onDismiss = { showDriveDialog = false },
                onBackup = {
                    createDocLauncher.launch("SouqFlow_Backup_${System.currentTimeMillis()}.json")
                },
                onRestore = {
                    openDocLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                }
            )
        }

        val pendingTargetDark = revealTargetDark
        if (pendingTargetDark != null) {
            val pendingTargetAuto = revealTargetAuto
            CircularRevealThemeOverlay(
                isTargetDark = pendingTargetDark,
                center = switchCenterOffset,
                onMiddlePoint = {
                    if (pendingTargetAuto != null) {
                        viewModel.setAutoThemeEnabled(pendingTargetAuto)
                    }
                    viewModel.setDarkMode(pendingTargetDark)
                },
                onAnimationEnd = {
                    revealTargetDark = null
                    revealTargetAuto = null
                }
            )
        }
    }
}

@Composable
private fun SettingsScreenHeader(isArabic: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = if (isArabic) "الإعدادات" else "Settings",
            color = TextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
        Text(
            text = if (isArabic) "تخصيص المظهر، المزامنة، والإيماءات" else "Preferences, sync & gestures",
            color = TextSecondary,
            fontSize = 11.5.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingsGroupDivider(
    isLight: Boolean,
    horizontalPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = horizontalPadding),
        thickness = 1.dp,
        color = if (isLight) Color(0xFFE6EDF5) else Color(0x18FFFFFF)
    )
}

@Composable
private fun CompactLanguagePill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    val pillBg by animateColorAsState(
        targetValue = if (isLight) {
            if (isSelected) CyanTintBg else Color(0xFFF8FAFC)
        } else {
            if (isSelected) Color(0x2800E5FF) else Color(0x181E293B)
        },
        label = "lang_pill_bg"
    )
    val pillBorder = if (isLight) {
        if (isSelected) BorderStroke(1.5.dp, CyanTintBorder) else BorderStroke(1.dp, LightCardBorder)
    } else {
        if (isSelected) BorderStroke(1.5.dp, ElectricCyan) else BorderStroke(1.dp, Color(0x2AFFFFFF))
    }
    val titleColor = if (isLight) {
        if (isSelected) CyanTintText else TextPrimary
    } else {
        if (isSelected) ElectricCyan else TextPrimary
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(pillBg)
            .border(pillBorder, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = ElectricCyan),
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 14.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = titleColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ThemeModeRow(
    isDarkMode: Boolean,
    isAutoTheme: Boolean,
    isArabic: Boolean,
    accentColor: Color,
    onDarkModeChange: (Boolean) -> Unit,
    onAutoToggle: () -> Unit,
    onSwitchPositionChanged: (Offset) -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isAutoTheme -> Icons.Default.BrightnessAuto
                        isDarkMode -> Icons.Default.DarkMode
                        else -> Icons.Default.LightMode
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = when {
                        isAutoTheme && isArabic -> "تلقائي"
                        isAutoTheme -> "Auto Theme"
                        isDarkMode && isArabic -> "الوضع الليلي"
                        isDarkMode -> "Night Mode"
                        isArabic -> "الوضع النهاري"
                        else -> "Day Mode"
                    },
                    color = TextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isAutoTheme) {
                        if (isArabic) "يتبع سمة الهاتف تلقائياً" else "Follows system appearance"
                    } else {
                        if (isArabic) "تبديل يدوي فوري للمظهر" else "Manual theme override"
                    },
                    color = if (isAutoTheme) EmeraldSavings else TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = EmeraldSavings),
                        onClick = onAutoToggle
                    )
                    .testTag("theme_auto_button"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isAutoTheme) {
                                EmeraldSavings.copy(alpha = 0.22f)
                            } else if (isLight) {
                                Color(0xFFE2E8F0)
                            } else {
                                Color(0x330B111E)
                            }
                        )
                        .border(
                            BorderStroke(
                                width = if (isAutoTheme) 1.5.dp else 1.dp,
                                color = if (isAutoTheme) {
                                    EmeraldSavings
                                } else if (isLight) {
                                    Color(0xFFCBD5E1)
                                } else {
                                    Color(0x44FFFFFF)
                                }
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Auto",
                        color = if (isAutoTheme) {
                            if (isLight) Color(0xFF047857) else EmeraldSavings
                        } else {
                            TextSecondary
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isAutoTheme) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            NeumorphicGlowingSwitch(
                checked = isDarkMode,
                onCheckedChange = onDarkModeChange,
                activeColor = ElectricCyan,
                inactiveColor = NeonAmber,
                checkedIcon = Icons.Default.DarkMode,
                uncheckedIcon = Icons.Default.LightMode,
                onPositionChanged = onSwitchPositionChanged,
                modifier = Modifier.testTag("theme_switch_toggle")
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onCheckedChange(!checked) })
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (checked) accentColor.copy(alpha = 0.16f) else Color(0x18777777)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) accentColor else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = if (checked) accentColor else TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        NeumorphicGlowingSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            activeColor = accentColor,
            inactiveColor = if (isLight) Color(0xFFCBD5E1) else Color(0xFF475569),
            checkedIcon = icon,
            uncheckedIcon = icon,
            modifier = Modifier.testTag(switchTestTag)
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    isLoading: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accentColor),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accentColor.copy(alpha = 0.14f))
                    .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = accentColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun NeumorphicGlowingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = ElectricCyan,
    inactiveColor: Color = NeonAmber,
    checkedIcon: ImageVector = Icons.Default.DarkMode,
    uncheckedIcon: ImageVector = Icons.Default.LightMode,
    onPositionChanged: (Offset) -> Unit = {}
) {
    val trackWidth = 58.dp
    val trackHeight = 32.dp
    val thumbSize = 24.dp
    val padding = 4.dp

    val currentColor by animateColorAsState(
        targetValue = if (checked) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 300),
        label = "switch_accent_color"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - padding else padding,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "switch_thumb_offset"
    )

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val pos = coordinates.positionInWindow()
                val center = Offset(
                    pos.x + coordinates.size.width / 2f,
                    pos.y + coordinates.size.height / 2f
                )
                onPositionChanged(center)
            }
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x330B111E))
            .border(
                BorderStroke(
                    1.2.dp,
                    Brush.horizontalGradient(
                        listOf(currentColor.copy(alpha = 0.7f), currentColor.copy(alpha = 0.25f))
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = currentColor),
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = currentColor,
                    spotColor = currentColor
                )
                .clip(CircleShape)
                .background(currentColor)
                .border(BorderStroke(1.5.dp, Color.White), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (checked) checkedIcon else uncheckedIcon,
                contentDescription = null,
                tint = if (checked) Color(0xFF071422) else Color(0xFF1E1405),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun GoogleDriveDialog(
    isArabic: Boolean,
    lastBackupDate: String,
    onDismiss: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    ShopyDialog(
        onDismissRequest = onDismiss,
        accentColor = ElectricCyan,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isArabic) "إغلاق" else "Close",
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElectricCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isArabic) "Google Drive" else "Google Drive Sync",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (lastBackupDate.isNotBlank()) {
                        if (isArabic) "آخر نسخ احتياطي: $lastBackupDate" else "Last Backup: $lastBackupDate"
                    } else {
                        if (isArabic) "لم يتم عمل نسخ احتياطي بعد" else "No backup recorded yet"
                    },
                    color = if (lastBackupDate.isNotBlank()) EmeraldSavings else TextMuted,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldSavings.copy(alpha = 0.18f))
                        .border(BorderStroke(1.2.dp, EmeraldSavings), RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = EmeraldSavings),
                            onClick = {
                                onBackup()
                                onDismiss()
                            }
                        )
                        .padding(horizontal = 14.dp)
                        .testTag("dialog_drive_backup_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = EmeraldSavings,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "نسخ احتياطي جديد" else "New Backup to Drive",
                            color = EmeraldSavings,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricCyan.copy(alpha = 0.18f))
                        .border(BorderStroke(1.2.dp, ElectricCyan), RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = ElectricCyan),
                            onClick = {
                                onRestore()
                                onDismiss()
                            }
                        )
                        .padding(horizontal = 14.dp)
                        .testTag("dialog_drive_restore_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "استعادة نسخة سابقة" else "Restore from Drive",
                            color = ElectricCyan,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun DeveloperInfoSection(isArabic: Boolean) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("developer_info_card"),
        glowColor = ElectricCyan
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(ElectricCyan, EmeraldSavings, VioletAccent, ElectricCyan)
                                )
                            )
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(if (isLight) Color.White else Color(0xFF09101D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CK",
                            color = if (isLight) CyanTintText else ElectricCyan,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Developed By Charfeddine Ka",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp,
                                maxLines = 2,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = EmeraldSavings,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Souq Flow Official",
                            color = if (isLight) CyanTintText else ElectricCyan.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isLight) CyanTintBg else Color(0x2200E5FF))
                        .border(
                            BorderStroke(1.dp, if (isLight) CyanTintBorder else ElectricCyan.copy(alpha = 0.5f)),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        color = if (isLight) CyanTintText else ElectricCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsGroupDivider(isLight = isLight)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DeveloperInfoMicroCard(
                    title = if (isArabic) "التطبيق" else "App",
                    value = "Souq Flow",
                    modifier = Modifier.weight(1f)
                )
                DeveloperInfoMicroCard(
                    title = if (isArabic) "المحرك" else "Stack",
                    value = "Compose • Room",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DeveloperInfoMicroCard(
                    title = if (isArabic) "الإصدار" else "Version",
                    value = ShoppingViewModel.APP_VERSION,
                    modifier = Modifier.weight(1f)
                )
                DeveloperInfoMicroCard(
                    title = if (isArabic) "التحديث" else "Updated",
                    value = if (isArabic) ShoppingViewModel.LAST_UPDATE_DATE_AR else ShoppingViewModel.LAST_UPDATE_DATE_EN,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DeveloperInfoMicroCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    val itemBg = if (isLight) Color(0xFFF8FAFC) else Color(0x181E293B)
    val itemBorder = if (isLight) BorderStroke(1.dp, Color(0xFFE2E8F0)) else BorderStroke(1.dp, Color(0x22475569))

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(itemBg)
            .border(itemBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
