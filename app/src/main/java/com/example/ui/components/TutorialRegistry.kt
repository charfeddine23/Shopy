package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.NeonAmber

enum class TutorialScreenTarget {
    HOME,
    PRICES,
    SUPERMARKET,
    STATISTICS,
    SETTINGS
}

enum class TutorialGestureType {
    HORIZONTAL_DOCK_SWIPE,
    SINGLE_FINGER_PULL_UP,
    BUDGET_PULSE,
    TWO_FINGER_VERTICAL_SWIPE,
    SETTINGS_POINTER,
    FEATURE_SPOTLIGHT_PULSE
}

data class TutorialFeatureStep(
    val id: String,
    val targetScreen: TutorialScreenTarget,
    val targetKeys: List<String>,
    val gestureType: TutorialGestureType,
    val badgeAr: String,
    val badgeEn: String,
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val tipAr: String,
    val tipEn: String,
    val accentColor: Color,
    val spotlightCornerRadiusDp: Float = 20f,
    val spotlightPaddingDp: Float = 6f
)

/**
 * Adaptive registry of guided spotlight steps.
 * Adding a new [TutorialFeatureStep] or bumping the app version automatically updates
 * [computeSignature], causing the in-app tutorial to appear once after update with
 * unseen steps automatically highlighted as NEW.
 */
object AppTutorialRegistry {

    val steps: List<TutorialFeatureStep> = listOf(
        TutorialFeatureStep(
            id = "welcome_dock",
            targetScreen = TutorialScreenTarget.HOME,
            targetKeys = listOf("dock_nav"),
            gestureType = TutorialGestureType.HORIZONTAL_DOCK_SWIPE,
            badgeAr = "شريط التنقل السائل",
            badgeEn = "LIQUID DOCK",
            titleAr = "تنقّل بانسيابية من الشريط السفلي",
            titleEn = "Glide Across App Screens",
            descriptionAr = "اسحب الشريط السفلي أفقياً أو اضغط على أي تبويب للتنقل الفوري بين قائمة التسوق، المنتجات، والإعدادات.",
            descriptionEn = "Swipe horizontally across the bottom dock or tap a tab to switch smoothly between List, Products, and Settings.",
            tipAr = "تستجيب الواجهة لحركة إصبعك بتأثير فيزيائي مباشر",
            tipEn = "Swipe horizontally across the glowing bottom bar",
            accentColor = ElectricCyan,
            spotlightCornerRadiusDp = 32f,
            spotlightPaddingDp = 4f
        ),
        TutorialFeatureStep(
            id = "pull_up_add",
            targetScreen = TutorialScreenTarget.HOME,
            targetKeys = listOf("empty_shopping_list_card", "add_item_action_button"),
            gestureType = TutorialGestureType.SINGLE_FINGER_PULL_UP,
            badgeAr = "قائمة التسوق",
            badgeEn = "SMART LIST",
            titleAr = "اسحب للأعلى بإصبع واحد للإضافة",
            titleEn = "Pull Up with One Finger to Add",
            descriptionAr = "عند فراغ القائمة، اسحب هذه البطاقة عمودياً للأعلى لفتح نافذة إضافة المنتجات فوراً أو استخدم زر الإضافة السريعة.",
            descriptionEn = "When your list is empty, pull this card straight up with one finger to open Quick Add immediately.",
            tipAr = "حساس السحب مضبوط على الاتجاه العمودي بدقة لتفادي اللمس العشوائي",
            tipEn = "Focused vertical pull prevents accidental diagonal triggers",
            accentColor = EmeraldSavings,
            spotlightCornerRadiusDp = 24f,
            spotlightPaddingDp = 6f
        ),
        TutorialFeatureStep(
            id = "cart_budget",
            targetScreen = TutorialScreenTarget.SUPERMARKET,
            targetKeys = listOf("supermarket_budget_hud", "supermarket_mode_header_pill"),
            gestureType = TutorialGestureType.BUDGET_PULSE,
            badgeAr = "وضع السوبرماركت",
            badgeEn = "CART & BUDGET",
            titleAr = "راقب سلتك وميزانيتك أثناء التسوق",
            titleEn = "Track Live Cart & Budget",
            descriptionAr = "يعرض هذا المؤشر مجموع ما تضعه في عربتك لحظياً مقارنة بالميزانية الشهرية المتبقية دون تجاوز الحد.",
            descriptionEn = "Watch your live cart spending update automatically against your remaining budget as you check off aisle items.",
            tipAr = "اضغط على أيقونة التعديل لتغيير سقف الميزانية في أي وقت",
            tipEn = "Tap the edit icon anytime to adjust your budget limit",
            accentColor = NeonAmber,
            spotlightCornerRadiusDp = 24f,
            spotlightPaddingDp = 6f
        ),
        TutorialFeatureStep(
            id = "two_finger_fullscreen",
            targetScreen = TutorialScreenTarget.HOME,
            targetKeys = emptyList(),
            gestureType = TutorialGestureType.TWO_FINGER_VERTICAL_SWIPE,
            badgeAr = "إيماءة ذكية",
            badgeEn = "FULL SCREEN",
            titleAr = "اسحب بإصبعين لوضع ملء الشاشة",
            titleEn = "Two-Finger Full Screen Swipe",
            descriptionAr = "اسحب بإصبعين للأعلى لإخفاء الشريط العلوي والتوسع للشاشة الكاملة، وللأسفل لإظهاره مع ثبات تام للصفحات.",
            descriptionEn = "Swipe up with two fingers to hide the status bar seamlessly, and swipe down with two fingers to restore it.",
            tipAr = "تبقى الخلفية ممتدة للحافة العلوية دون قفز العناصر",
            tipEn = "Content stays anchored smoothly while bars hide or show",
            accentColor = TutorialViolet,
            spotlightCornerRadiusDp = 24f,
            spotlightPaddingDp = 6f
        ),
        TutorialFeatureStep(
            id = "qr_sync_updates",
            targetScreen = TutorialScreenTarget.HOME,
            targetKeys = listOf("dock_tab_SETTINGS", "dock_nav"),
            gestureType = TutorialGestureType.SETTINGS_POINTER,
            badgeAr = "المشاركة والتحديثات",
            badgeEn = "SYNC & UPDATES",
            titleAr = "مشاركة QR، النسخ السحابي، والتحديثات",
            titleEn = "QR Share, Backup & Updates",
            descriptionAr = "من تبويب الإعدادات يمكنك مشاركة القائمة عبر QR دون إنترنت، حفظ نسخة على Google Drive، وتحديث التطبيق.",
            descriptionEn = "Open Settings to share lists offline via QR code, back up to Google Drive, download updates, or replay this guide.",
            tipAr = "يمكنك إعادة تشغيل هذا الدليل في أي وقت من الإعدادات",
            tipEn = "Replay this interactive guide anytime inside Settings",
            accentColor = EmeraldSavings,
            spotlightCornerRadiusDp = 22f,
            spotlightPaddingDp = 6f
        )
    )

    fun computeSignature(
        appVersion: String,
        featureSteps: List<TutorialFeatureStep> = steps
    ): String {
        val featureDigest = featureSteps.joinToString(separator = ",") { it.id }
        return "${appVersion.trim()}|$featureDigest"
    }

    fun parseSeenStepIds(serialized: String): Set<String> {
        if (serialized.isBlank()) return emptySet()
        return serialized.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    fun serializeSeenStepIds(ids: Collection<String>): String {
        return ids.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(",")
    }
}

/**
 * Tracks live screen-root bounds for tutorial target Composables so spotlights lock onto real UI components.
 */
class TutorialTargetRegistry {
    private val _boundsMap = mutableStateMapOf<String, Rect>()
    val boundsMap: Map<String, Rect> get() = _boundsMap

    fun update(key: String, rect: Rect) {
        if (rect.width > 0f && rect.height > 0f) {
            _boundsMap[key] = rect
        }
    }

    fun remove(key: String) {
        _boundsMap.remove(key)
    }

    fun resolveFirst(keys: List<String>): Rect? {
        for (key in keys) {
            val found = _boundsMap[key]
            if (found != null && found.width > 0f && found.height > 0f) {
                return found
            }
        }
        return null
    }
}

val LocalTutorialTargetRegistry = compositionLocalOf<TutorialTargetRegistry?> { null }

fun Modifier.tutorialTarget(key: String): Modifier = composed {
    val registry = LocalTutorialTargetRegistry.current
    if (registry == null) {
        this
    } else {
        DisposableEffect(registry, key) {
            onDispose {
                registry.remove(key)
            }
        }
        this.onGloballyPositioned { coords ->
            if (coords.isAttached) {
                registry.update(key, coords.boundsInRoot())
            }
        }
    }
}
