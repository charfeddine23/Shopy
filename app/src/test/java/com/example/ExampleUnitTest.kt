package com.example

import com.example.data.local.DatabaseInitialData
import com.example.data.local.InMemoryPreferencesStorage
import com.example.data.local.MarketPriceDao
import com.example.data.local.ShoppingDao
import com.example.data.local.SpendingDao
import com.example.data.model.MarketPrice
import com.example.data.model.PriceHistory
import com.example.data.model.ShoppingItem
import com.example.data.model.SpendingRecord
import com.example.data.repository.ShoppingRepository
import com.example.ui.components.AppTutorialRegistry
import com.example.ui.components.SingleFingerPullUpDetector
import com.example.ui.components.TutorialFeatureStep
import com.example.ui.components.TutorialGestureType
import com.example.ui.components.TutorialInteractionBus
import com.example.ui.components.TutorialRequiredAction
import com.example.ui.components.TutorialScreenTarget
import com.example.ui.components.TwoFingerSwipeDetector
import com.example.ui.components.TwoFingerSwipeDirection
import com.example.ui.theme.ElectricCyan
import com.example.ui.viewmodel.ShoppingViewModel
import com.example.util.AppUpdateChecker
import com.example.util.LocalizationUtil
import com.example.util.ProductGroup
import com.example.util.ProductVariant
import com.example.util.SmartSearchUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCurrencyFormatting() {
        val formattedAr = LocalizationUtil.formatDzd(1250.0, isArabic = true)
        val formattedEn = LocalizationUtil.formatDzd(1250.0, isArabic = false)

        assertTrue(formattedAr.contains("1,250") && formattedAr.contains("د.ج"))
        assertTrue(formattedEn.contains("1,250") && formattedEn.contains("DA"))
    }

    @Test
    fun testCategoryLocalization() {
        assertEquals("خضر وفواكه", LocalizationUtil.getCategoryLabel("Produce", isArabic = true))
        assertEquals("ألبان وأجبان", LocalizationUtil.getCategoryLabel("Dairy", isArabic = true))
        assertEquals("Produce", LocalizationUtil.getCategoryLabel("Produce", isArabic = false))
        assertEquals("مواد تنظيف", LocalizationUtil.getCategoryLabel("Household", isArabic = true))
        assertEquals("لحوم ودواجن", LocalizationUtil.getCategoryLabel("Butchery", isArabic = true))
    }

    @Test
    fun testInitialSpendingCategoriesAreValid() {
        assertTrue(DatabaseInitialData.initialSpendingRecords.isNotEmpty())
        DatabaseInitialData.initialSpendingRecords.forEach { rec ->
            assertTrue(LocalizationUtil.allCategories.contains(rec.topCategory))
        }
    }

    @Test
    fun testQrSerializationAndDeserialization() {
        val originalItem = ShoppingItem(
            name = "Elio Cooking Oil 5L",
            nameAr = "زيت المائدة إيليو 5 لتر",
            category = "Pantry",
            quantity = 2,
            unit = "pcs",
            barcode = "6130002000456",
            estimatedPriceDzd = 620.0
        )
        val payload = "SOUK|${originalItem.name};${originalItem.nameAr};${originalItem.category};${originalItem.quantity};${originalItem.unit};${originalItem.barcode};${originalItem.estimatedPriceDzd}"

        assertTrue(payload.startsWith("SOUK|"))
        val itemsData = payload.removePrefix("SOUK|").split(";")
        assertEquals("Elio Cooking Oil 5L", itemsData[0])
        assertEquals("زيت المائدة إيليو 5 لتر", itemsData[1])
        assertEquals("Pantry", itemsData[2])
        assertEquals(2, itemsData[3].toInt())
    }

    @Test
    fun testDeveloperAndAppInfo() {
        assertEquals("Charfeddine Ka", com.example.ui.viewmodel.ShoppingViewModel.DEVELOPER_NAME)
        assertEquals("v1.0 Build 1", com.example.ui.viewmodel.ShoppingViewModel.APP_VERSION)
    }

    @Test
    fun testUpdateVersionComparison() {
        assertEquals("https://github.com/charfeddine23/Shopy", AppUpdateChecker.UPDATE_URL)
        assertTrue(AppUpdateChecker.isNewerVersion("v1.1", "1.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("2.0.0", "1.9.5"))
        assertTrue(AppUpdateChecker.isNewerVersion("Shopy-v1.2.0", "1.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("Release 1.3-beta", "1.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("v2", "1.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.0", "1.0", remoteBuildCode = 2, currentBuildCode = 1))
        assertTrue(AppUpdateChecker.isNewerVersion("Build 3", "1.0", remoteBuildCode = 3, currentBuildCode = 1))
        assertFalse(AppUpdateChecker.isNewerVersion("v1.0", "1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("0.9", "1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("Build 1", "1.0", remoteBuildCode = 1, currentBuildCode = 1))
        assertFalse(AppUpdateChecker.isNewerVersion("latest", "1.0"))

        // Lexicographical tags order trap: v1.10 must beat v1.9 and v1.2
        val highestTag = AppUpdateChecker.extractHighestTag(listOf("v1.0", "v1.9", "v1.10", "v1.2"))
        assertEquals("v1.10", highestTag)

        // Parse Kotlin DSL build.gradle.kts with System.getenv fallback (matches repo pattern)
        val gradleKts = """
            defaultConfig {
              versionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 4
              versionName = System.getenv("APP_VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.4.0"
            }
        """.trimIndent()
        val (parsedName, parsedCode) = AppUpdateChecker.parseBuildGradleVersion(gradleKts)
        assertEquals("1.4.0", parsedName)
        assertEquals(4, parsedCode)

        // Parse releases.atom feed (rate-limit-proof fallback)
        val atomXml = """
            <feed>
              <entry>
                <id>tag:github.com,2008:Repository/1/v1.2.1</id>
                <updated>2026-11-15T04:00:00Z</updated>
                <link rel="alternate" type="text/html" href="https://github.com/charfeddine23/Shopy/releases/tag/v1.2.1"/>
                <title>Shopy v1.2.1</title>
                <content type="html">&lt;p&gt;New update ready&lt;/p&gt;</content>
              </entry>
            </feed>
        """.trimIndent()
        val atomCandidates = AppUpdateChecker.parseReleasesAtom(atomXml)
        assertEquals(1, atomCandidates.size)
        assertEquals("v1.2.1", atomCandidates.first().versionLabel)
        assertEquals("New update ready", atomCandidates.first().releaseNotes)
        assertEquals("2026-11-15", atomCandidates.first().publishedAt)

        // Extract .apk URL from expanded_assets HTML fragment
        val html = """<a href="/charfeddine23/Shopy/releases/download/v1.2.1/Shopy-release.apk" rel="nofollow">Shopy-release.apk</a>"""
        val apkLinks = AppUpdateChecker.extractApkUrlsFromExpandedAssetsHtml(html)
        assertEquals(listOf("https://github.com/charfeddine23/Shopy/releases/download/v1.2.1/Shopy-release.apk"), apkLinks)
    }

    @Test
    fun testTrustedUpdateUrlAllowlist() {
        assertTrue(AppUpdateChecker.isTrustedUpdateUrl("https://github.com/charfeddine23/Shopy"))
        assertTrue(AppUpdateChecker.isTrustedUpdateUrl("https://api.github.com/repos/charfeddine23/Shopy/releases/latest"))
        assertTrue(AppUpdateChecker.isTrustedUpdateUrl("https://objects.githubusercontent.com/some/release.apk"))
        assertTrue(AppUpdateChecker.isTrustedUpdateUrl("https://release-assets.githubusercontent.com/some/release.apk"))
        assertTrue(AppUpdateChecker.isTrustedUpdateUrl("https://github-production-release-asset-2e65be.s3.amazonaws.com/12345/app.apk"))

        assertFalse(AppUpdateChecker.isTrustedUpdateUrl("http://github.com/charfeddine23/Shopy"))
        assertFalse(AppUpdateChecker.isTrustedUpdateUrl("https://evil.com/Shopy.apk"))
        assertFalse(AppUpdateChecker.isTrustedUpdateUrl("https://mybucket.s3.amazonaws.com/evil.apk"))
        assertFalse(AppUpdateChecker.isTrustedUpdateUrl("https://github.com.evil.com/app.apk"))
        assertFalse(AppUpdateChecker.isTrustedUpdateUrl("javascript:alert(1)"))
    }

    @Test
    fun testTwoFingerSwipeDetectorAndPreference() {
        assertEquals(
            TwoFingerSwipeDirection.UP,
            TwoFingerSwipeDetector.evaluateVerticalSwipe(
                dy1 = -140f,
                dy2 = -130f,
                dx1 = 10f,
                dx2 = -5f,
                swipeThresholdPx = 100f,
                minPerFingerDyPx = 40f
            )
        )
        assertEquals(
            TwoFingerSwipeDirection.DOWN,
            TwoFingerSwipeDetector.evaluateVerticalSwipe(
                dy1 = 150f,
                dy2 = 125f,
                dx1 = 12f,
                dx2 = 8f,
                swipeThresholdPx = 100f,
                minPerFingerDyPx = 40f
            )
        )
        // Opposing vertical directions (pinch) must not trigger
        assertEquals(
            null,
            TwoFingerSwipeDetector.evaluateVerticalSwipe(
                dy1 = -130f,
                dy2 = 130f,
                dx1 = 0f,
                dx2 = 0f,
                swipeThresholdPx = 100f,
                minPerFingerDyPx = 40f
            )
        )
        // Primarily horizontal swipe must not trigger
        assertEquals(
            null,
            TwoFingerSwipeDetector.evaluateVerticalSwipe(
                dy1 = -110f,
                dy2 = -110f,
                dx1 = 180f,
                dx2 = 190f,
                swipeThresholdPx = 100f,
                minPerFingerDyPx = 40f
            )
        )
        // Too small movement must not trigger
        assertEquals(
            null,
            TwoFingerSwipeDetector.evaluateVerticalSwipe(
                dy1 = -50f,
                dy2 = -55f,
                dx1 = 0f,
                dx2 = 0f,
                swipeThresholdPx = 100f,
                minPerFingerDyPx = 40f
            )
        )

        val repo = ShoppingRepository(
            shoppingDao = FakeShoppingDao(mutableListOf()),
            marketPriceDao = FakeMarketPriceDao(),
            spendingDao = FakeSpendingDao()
        )
        val vm = ShoppingViewModel(repo)
        assertTrue(vm.isTwoFingerFullScreenSwipeEnabled.value)
        vm.setTwoFingerFullScreenSwipeEnabled(false)
        assertFalse(vm.isTwoFingerFullScreenSwipeEnabled.value)
        vm.setTwoFingerFullScreenSwipeEnabled(true)
        assertTrue(vm.isTwoFingerFullScreenSwipeEnabled.value)
    }

    @Test
    fun testFullScreenPreservedTopInsetStability() {
        // Normal mode -> Full screen (status bar hidden) -> Back to normal: inset stays locked
        val normalTop = resolvePreservedTopInsetPx(
            visibleStatusTopPx = 132,
            stableStatusTopPx = 132,
            cutoutTopPx = 132,
            cachedTopPx = 0,
            fallbackStatusBarTopPx = 96
        )
        assertEquals(132, normalTop)

        val fullScreenTop = resolvePreservedTopInsetPx(
            visibleStatusTopPx = 0,
            stableStatusTopPx = 132,
            cutoutTopPx = 132,
            cachedTopPx = normalTop,
            fallbackStatusBarTopPx = 96
        )
        assertEquals(132, fullScreenTop)

        // Legacy API < 30 cold start directly in Full Screen uses resource status_bar_height fallback
        val legacyColdStartFullScreen = resolvePreservedTopInsetPx(
            visibleStatusTopPx = 0,
            stableStatusTopPx = 0,
            cutoutTopPx = 0,
            cachedTopPx = 0,
            fallbackStatusBarTopPx = 96
        )
        assertEquals(96, legacyColdStartFullScreen)

        // Larger display cutout locks safely
        val cutoutTop = resolvePreservedTopInsetPx(
            visibleStatusTopPx = 0,
            stableStatusTopPx = 132,
            cutoutTopPx = 148,
            cachedTopPx = 132,
            fallbackStatusBarTopPx = 96
        )
        assertEquals(148, cutoutTop)
    }

    @Test
    fun testSingleFingerPullUpDetector() {
        val slop = 18f
        // Straight up and slight <=14 deg deviation pass
        assertTrue(SingleFingerPullUpDetector.isDominantUpwardDrag(dx = 12f, dy = -60f, touchSlopPx = slop))
        // Diagonal (~22 deg) must NOT trigger strict pull-up
        assertFalse(SingleFingerPullUpDetector.isDominantUpwardDrag(dx = 25f, dy = -60f, touchSlopPx = slop))
        assertFalse(SingleFingerPullUpDetector.isDominantUpwardDrag(dx = 0f, dy = -10f, touchSlopPx = slop))
        assertFalse(SingleFingerPullUpDetector.isDominantUpwardDrag(dx = 0f, dy = 60f, touchSlopPx = slop))
        assertFalse(SingleFingerPullUpDetector.isDominantUpwardDrag(dx = 90f, dy = -60f, touchSlopPx = slop))

        assertTrue(SingleFingerPullUpDetector.isAbandonedForOtherGesture(dx = 5f, dy = 50f, touchSlopPx = slop))
        assertTrue(SingleFingerPullUpDetector.isAbandonedForOtherGesture(dx = 60f, dy = -30f, touchSlopPx = slop))
        assertTrue(SingleFingerPullUpDetector.isAbandonedForOtherGesture(dx = 25f, dy = -60f, touchSlopPx = slop))
        assertFalse(SingleFingerPullUpDetector.isAbandonedForOtherGesture(dx = 10f, dy = -60f, touchSlopPx = slop))

        assertFalse(SingleFingerPullUpDetector.isTriggerThresholdReached(dx = 0f, dy = -80f, thresholdPx = 120f))
        assertFalse(SingleFingerPullUpDetector.isTriggerThresholdReached(dx = 45f, dy = -130f, thresholdPx = 120f))
        assertTrue(SingleFingerPullUpDetector.isTriggerThresholdReached(dx = 15f, dy = -120f, thresholdPx = 120f))
        assertTrue(SingleFingerPullUpDetector.isTriggerThresholdReached(dx = 0f, dy = -180f, thresholdPx = 120f))

        assertEquals(0f, SingleFingerPullUpDetector.computeVisualPullOffsetPx(dy = 40f, maxVisualPullPx = 90f), 0.01f)
        assertEquals(42f, SingleFingerPullUpDetector.computeVisualPullOffsetPx(dy = -100f, maxVisualPullPx = 90f), 0.01f)
        assertEquals(90f, SingleFingerPullUpDetector.computeVisualPullOffsetPx(dy = -350f, maxVisualPullPx = 90f), 0.01f)
    }

    @Test
    fun testTutorialStateAndCompletion() = runBlocking {
        // Verify removed basket-comparison tutorial window is completely gone
        val steps = AppTutorialRegistry.steps
        assertTrue(steps.isNotEmpty())
        assertFalse(steps.any {
            it.id == "price_comparison" ||
                it.titleAr.contains("اكتشف أرخص متجر لسلت") ||
                it.titleEn.contains("Lowest Basket Price", ignoreCase = true)
        })

        // Verify signature adapts automatically when app version changes OR when a new feature step is added
        val sigV1 = AppTutorialRegistry.computeSignature("v1.0 Build 1", steps)
        val sigV2 = AppTutorialRegistry.computeSignature("v1.1 Build 2", steps)
        val extraStep = TutorialFeatureStep(
            id = "ai_scanner",
            targetScreen = TutorialScreenTarget.HOME,
            targetKeys = listOf("dock_nav"),
            gestureType = TutorialGestureType.FEATURE_SPOTLIGHT_PULSE,
            requiredAction = TutorialRequiredAction.TAP_TO_CONTINUE,
            badgeAr = "جديد",
            badgeEn = "NEW",
            titleAr = "ميزة جديدة",
            titleEn = "New Feature",
            descriptionAr = "وصف",
            descriptionEn = "Desc",
            instructionAr = "نفذ الإجراء",
            instructionEn = "Perform action",
            tipAr = "تلميح",
            tipEn = "Tip",
            accentColor = ElectricCyan
        )
        val sigNewFeature = AppTutorialRegistry.computeSignature("v1.0 Build 1", steps + extraStep)
        assertNotEquals(sigV1, sigV2)
        assertNotEquals(sigV1, sigNewFeature)

        val repo = ShoppingRepository(
            shoppingDao = FakeShoppingDao(mutableListOf()),
            marketPriceDao = FakeMarketPriceDao(),
            spendingDao = FakeSpendingDao()
        )
        val storage = InMemoryPreferencesStorage()

        // Launch 1 on v1 -> shows once
        val vmV1First = ShoppingViewModel(repo, storage, tutorialSignature = sigV1)
        assertFalse(vmV1First.hasSeenTutorial.value)
        assertTrue(vmV1First.showTutorialOverlay.value)

        vmV1First.completeTutorial()
        assertTrue(vmV1First.hasSeenTutorial.value)
        assertFalse(vmV1First.showTutorialOverlay.value)
        assertEquals(steps.map { it.id }.toSet(), vmV1First.seenTutorialStepIds.value)

        // Wait briefly for ViewModel coroutine to persist into storage
        kotlinx.coroutines.delay(50)

        // Launch 2 on same version & features -> stays hidden automatically
        val vmV1Second = ShoppingViewModel(repo, storage, tutorialSignature = sigV1)
        assertTrue(vmV1Second.hasSeenTutorial.value)
        assertFalse(vmV1Second.showTutorialOverlay.value)

        // Manual replay from Settings still works
        vmV1Second.openTutorial()
        assertTrue(vmV1Second.showTutorialOverlay.value)
        vmV1Second.completeTutorial()
        assertFalse(vmV1Second.showTutorialOverlay.value)

        // Simulate app update OR newly added feature -> tutorial appears ONCE automatically again
        val vmUpdated = ShoppingViewModel(repo, storage, tutorialSignature = sigNewFeature)
        assertTrue(vmUpdated.showTutorialOverlay.value)
        assertEquals(steps.map { it.id }.toSet(), vmUpdated.seenTutorialStepIds.value)
        vmUpdated.completeTutorial()
        assertFalse(vmUpdated.showTutorialOverlay.value)
    }

    @Test
    fun testTutorialInteractionBusAndMandatoryStepMetadata() {
        val steps = AppTutorialRegistry.steps
        steps.forEach { step ->
            assertTrue("Step ${step.id} missing instructionAr", step.instructionAr.isNotBlank())
            assertTrue("Step ${step.id} missing instructionEn", step.instructionEn.isNotBlank())
            assertNotEquals(TutorialRequiredAction.TAP_TO_CONTINUE, step.requiredAction)
        }

        val bus = TutorialInteractionBus()
        assertFalse(bus.isExpecting(TutorialRequiredAction.PULL_UP_ADD_ITEM))
        assertFalse(bus.reportAction(TutorialRequiredAction.PULL_UP_ADD_ITEM))
        assertEquals(0, bus.completedActionSequence)

        bus.activeRequiredAction = TutorialRequiredAction.PULL_UP_ADD_ITEM
        assertTrue(bus.isExpecting(TutorialRequiredAction.PULL_UP_ADD_ITEM))

        // Unrelated action must be ignored while PULL_UP_ADD_ITEM is active
        assertFalse(bus.reportAction(TutorialRequiredAction.SWITCH_DOCK_TAB))
        assertEquals(0, bus.completedActionSequence)

        // Matching action triggers completion and increments sequence
        assertTrue(bus.reportAction(TutorialRequiredAction.PULL_UP_ADD_ITEM))
        assertEquals(1, bus.completedActionSequence)
        assertEquals(TutorialRequiredAction.PULL_UP_ADD_ITEM, bus.lastCompletedAction)

        bus.clear()
        assertEquals(null, bus.activeRequiredAction)
        assertEquals(null, bus.lastCompletedAction)
    }

    @Test
    fun testSmartSearchRankingAndFuzzyMatching() {
        val candiaPlain = ProductVariant(
            barcode = "6130001",
            itemName = "Candia Milk",
            itemNameAr = "حليب كونديا",
            storeName = "Uno",
            storeNameAr = "أونو",
            priceDzd = 135.0,
            allPrices = emptyList()
        )
        val chocoDrink = ProductVariant(
            barcode = "6130002",
            itemName = "Kids Dessert with Candia Milk Powder",
            itemNameAr = "حلوى بالحليب المجفف كونديا",
            storeName = "Ardis",
            storeNameAr = "أرديس",
            priceDzd = 220.0,
            allPrices = emptyList()
        )
        val riceVariant = ProductVariant(
            barcode = "6130003",
            itemName = "Basmati Rice 1kg",
            itemNameAr = "أرز بسمتي 1 كلغ",
            storeName = "Uno",
            storeNameAr = "أونو",
            priceDzd = 380.0,
            allPrices = emptyList()
        )
        val oilVariant = ProductVariant(
            barcode = "6130004",
            itemName = "Elio Cooking Oil 5L",
            itemNameAr = "زيت المائدة إيليو 5 لتر",
            storeName = "Carrefour",
            storeNameAr = "كارفور",
            priceDzd = 620.0,
            allPrices = emptyList()
        )

        val dessertGroup = ProductGroup(
            id = "dessert",
            nameAr = "حلويات بالحليب",
            nameEn = "Milk Desserts",
            category = "Dairy",
            variants = listOf(chocoDrink),
            minPrice = 220.0,
            maxPrice = 220.0
        )
        val candiaGroup = ProductGroup(
            id = "milk",
            nameAr = "حليب كونديا",
            nameEn = "Candia Milk",
            category = "Dairy",
            variants = listOf(chocoDrink, candiaPlain),
            minPrice = 135.0,
            maxPrice = 220.0
        )
        val riceGroup = ProductGroup(
            id = "rice",
            nameAr = "أرز بسمتي",
            nameEn = "Basmati Rice",
            category = "Pantry",
            variants = listOf(riceVariant),
            minPrice = 380.0,
            maxPrice = 380.0
        )
        val oilGroup = ProductGroup(
            id = "oil",
            nameAr = "زيت المائدة",
            nameEn = "Cooking Oil",
            category = "Pantry",
            variants = listOf(oilVariant),
            minPrice = 620.0,
            maxPrice = 620.0
        )

        val allGroups = listOf(dessertGroup, riceGroup, oilGroup, candiaGroup)

        // Exact match must appear FIRST even if placed last in input list
        val exactResults = SmartSearchUtil.searchAndRankGroups(allGroups, "Candia Milk")
        assertEquals("milk", exactResults.first().id)
        // Best matching variant inside group is also ordered first
        assertEquals("6130001", exactResults.first().variants.first().barcode)

        // Arabic normalization ("ارز" without hamza matches "أرز")
        val arabicNormResults = SmartSearchUtil.searchAndRankGroups(allGroups, "ارز")
        assertEquals(1, arabicNormResults.size)
        assertEquals("rice", arabicNormResults.first().id)

        // Typo tolerance ("canida" transposed letters matches "Candia Milk" first)
        val fuzzyResults = SmartSearchUtil.searchAndRankGroups(allGroups, "canida")
        assertTrue(fuzzyResults.isNotEmpty())
        assertEquals("milk", fuzzyResults.first().id)

        // Multi-token search ("زيت 5 لتر" out of adjacent order)
        val tokenResults = SmartSearchUtil.searchAndRankGroups(allGroups, "زيت 5 لتر")
        assertEquals(1, tokenResults.size)
        assertEquals("oil", tokenResults.first().id)

        // Exact variant ranking directly
        val rankedVariants = SmartSearchUtil.rankVariants(listOf(chocoDrink, candiaPlain), "Candia Milk")
        assertEquals("6130001", rankedVariants.first().barcode)
    }

    @Test
    fun testQrSanitizationAndImportValidation() = runBlocking {
        val inserted = mutableListOf<ShoppingItem>()
        val repo = ShoppingRepository(
            shoppingDao = FakeShoppingDao(inserted),
            marketPriceDao = FakeMarketPriceDao(),
            spendingDao = FakeSpendingDao()
        )

        val malicious = ShoppingItem(
            name = "Milk;Drop~Table|Hack",
            nameAr = "حليب;تجربة|اختبار",
            category = "Dairy",
            quantity = 12000,
            unit = "pcs",
            barcode = "123;456",
            estimatedPriceDzd = -50.0
        )
        val encoded = repo.createQrShareString(listOf(malicious))
        assertFalse(encoded.removePrefix("SOUK|").contains("|"))
        assertFalse(encoded.contains("Milk;Drop"))

        val imported = repo.importFromQrString(encoded)
        assertEquals(1, imported)
        assertEquals(999, inserted.first().quantity)
        assertEquals(0.0, inserted.first().estimatedPriceDzd, 0.001)

        val invalidHeader = repo.importFromQrString("BAD|Milk;حليب;Dairy;2")
        assertEquals(0, invalidHeader)

        val emptyNameImport = repo.importFromQrString("SOUK|   ;حليب;Dairy;2")
        assertEquals(0, emptyNameImport)
    }

    private class FakeShoppingDao(private val store: MutableList<ShoppingItem>) : ShoppingDao {
        override fun getAllShoppingItems(): Flow<List<ShoppingItem>> = flowOf(store)
        override suspend fun insertItem(item: ShoppingItem): Long {
            store.add(item)
            return store.size.toLong()
        }
        override suspend fun insertItems(items: List<ShoppingItem>) {
            store.addAll(items)
        }
        override suspend fun updateItem(item: ShoppingItem) {}
        override suspend fun deleteItem(item: ShoppingItem) {}
        override suspend fun updateCheckedStatus(id: Long, isChecked: Boolean) {}
        override suspend fun updateQuantity(id: Long, quantity: Int) {}
        override suspend fun deleteCheckedItems() {}
        override suspend fun clearAll() {
            store.clear()
        }
    }

    private class FakeMarketPriceDao : MarketPriceDao {
        override fun getPricesForBarcode(barcode: String): Flow<List<MarketPrice>> = flowOf(emptyList())
        override fun getAllMarketPrices(): Flow<List<MarketPrice>> = flowOf(emptyList())
        override suspend fun getMarketPriceCount(): Int = 0
        override suspend fun getBestPriceForBarcode(barcode: String): MarketPrice? = null
        override suspend fun getPriceForBarcodeAndStore(barcode: String, storeName: String): MarketPrice? = null
        override suspend fun insertMarketPrice(price: MarketPrice): Long = 1L
        override suspend fun insertMarketPrices(prices: List<MarketPrice>) {}
        override suspend fun updatePriceReport(barcode: String, storeName: String, newPrice: Double, updatedAt: Long) {}
        override suspend fun updatePriceForBarcode(barcode: String, newPrice: Double, updatedAt: Long) {}
        override suspend fun insertPriceHistories(histories: List<PriceHistory>) {}
    }

    private class FakeSpendingDao : SpendingDao {
        override fun getAllSpendingRecords(): Flow<List<SpendingRecord>> = flowOf(emptyList())
        override suspend fun getSpendingRecordCount(): Int = 0
        override suspend fun insertSpendingRecord(record: SpendingRecord): Long = 1L
        override suspend fun insertSpendingRecords(records: List<SpendingRecord>) {}
        override suspend fun deleteSpendingRecord(id: Long) {}
    }
}
