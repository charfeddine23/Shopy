package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

enum class TutorialRequiredAction {
    SWITCH_DOCK_TAB,
    PULL_UP_ADD_ITEM,
    INTERACT_BUDGET_HUD,
    TWO_FINGER_FULLSCREEN_SWIPE,
    TAP_SETTINGS_TAB,
    TAP_TO_CONTINUE
}

data class TutorialFeatureStep(
    val id: String,
    val targetScreen: TutorialScreenTarget,
    val targetKeys: List<String>,
    val gestureType: TutorialGestureType,
    val requiredAction: TutorialRequiredAction,
    val badgeAr: String,
    val badgeEn: String,
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val instructionAr: String,
    val instructionEn: String,
    val tipAr: String,
    val tipEn: String,
    val accentColor: Color,
    val spotlightCornerRadiusDp: Float = 20f,
    val spotlightPaddingDp: Float = 6f
)

/**
 * Coordinates mandatory interactive verification between live UI controls and [AppTutorialOverlay].
 */
class TutorialInteractionBus {
    var activeRequiredAction: TutorialRequiredAction? by mutableStateOf(null)
    var lastCompletedAction: TutorialRequiredAction? by mutableStateOf(null)
    var completedActionSequence: Int by mutableIntStateOf(0)

    fun isExpecting(action: TutorialRequiredAction): Boolean = activeRequiredAction == action

    fun reportAction(action: TutorialRequiredAction): Boolean {
        if (activeRequiredAction == action) {
            lastCompletedAction = action
            completedActionSequence += 1
            return true
        }
        return false
    }

    fun clear() {
        activeRequiredAction = null
        lastCompletedAction = null
    }
}

val LocalTutorialInteractionBus = compositionLocalOf<TutorialInteractionBus?> { null }

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
            requiredAction = TutorialRequiredAction.SWITCH_DOCK_TAB,
            badgeAr = "شريط التنقل السائل",
            badgeEn = "LIQUID DOCK",
            titleAr = "تنقّل بانسيابية من الشريط السفلي",
            titleEn = "Glide Across App Screens",
            descriptionAr = "اسحب الشريط السفلي أفقياً أو اضغط على تبويب المنتجات للتنقل الفوري بين شاشات التطبيق.",
            descriptionEn = "Swipe horizontally across the bottom dock or tap another tab to glide between screens.",
            instructionAr = "جرّب الآن: اسحب الشريط السفلي أو اختر تبويباً آخر",
            instructionEn = "Try it now: Swipe the bottom bar or tap another tab",
            tipAr = "المس داخل الإطار المضيء بالأسفل لتجربة التفاعل",
            tipEn = "Interact inside the glowing spotlight below to continue",
            accentColor = ElectricCyan,
            spotlightCornerRadiusDp = 32f,
            spotlightPaddingDp = 4f
        ),
        TutorialFeatureStep(
            id = "pull_up_add",
            targetScreen = TutorialScreenTarget.HOME,
            targetKeys = listOf("empty_shopping_list_card", "add_item_action_button"),
            gestureType = TutorialGestureType.SINGLE_FINGER_PULL_UP,
            requiredAction = TutorialRequiredAction.PULL_UP_ADD_ITEM,
            badgeAr = "قائمة التسوق",
            badgeEn = "SMART LIST",
            titleAr = "اسحب للأعلى بإصبع واحد للإضافة",
            titleEn = "Pull Up with One Finger to Add",
            descriptionAr = "اسحب البطاقة المضيئة عمودياً للأعلى بإصبع واحد لفتح الإضافة السريعة (أو اضغط زر الإضافة إذا كانت القائمة ممتلئة).",
            descriptionEn = "Pull the highlighted card straight up with one finger to trigger Quick Add.",
            instructionAr = "جرّب الآن: اسحب البطاقة عمودياً للأعلى بإصبعك",
            instructionEn = "Try it now: Pull straight upward with one finger",
            tipAr = "حساس السحب عمودي بدقة لتجنب التشغيل العرضي",
            tipEn = "Strict straight-up sensor avoids accidental triggers",
            accentColor = EmeraldSavings,
            spotlightCornerRadiusDp = 24f,
            spotlightPaddingDp = 6f
        ),
        TutorialFeatureStep(
            id = "cart_budget",
            targetScreen = TutorialScreenTarget.SUPERMARKET,
            targetKeys = listOf("supermarket_budget_hud", "supermarket_mode_header_pill"),
            gestureType = TutorialGestureType.BUDGET_PULSE,
            requiredAction = TutorialRequiredAction.INTERACT_BUDGET_HUD,
            badgeAr = "وضع السوبرماركت",
            badgeEn = "CART & BUDGET",
            titleAr = "راقب سلتك وميزانيتك أثناء التسوق",
            titleEn = "Track Live Cart & Budget",
            descriptionAr = "يعرض مؤشر الميزانية المجموع الحي لعربتك مقارنة بالحد الأقصى المتبقي أثناء التبضع.",
            descriptionEn = "This HUD monitors live cart spending against your monthly limit in real time.",
            instructionAr = "جرّب الآن: اضغط على بطاقة الميزانية أو أيقونة التعديل",
            instructionEn = "Try it now: Tap the highlighted budget card or edit icon",
            tipAr = "تساعدك ألوان المؤشر على عدم تجاوز سقف الميزانية",
            tipEn = "Progress colors warn you before exceeding your budget",
            accentColor = NeonAmber,
            spotlightCornerRadiusDp = 24f,
            spotlightPaddingDp = 6f
        ),
        TutorialFeatureStep(
            id = "two_finger_fullscreen",
            targetScreen = TutorialScreenTarget.HOME,
            targetKeys = emptyList(),
            gestureType = TutorialGestureType.TWO_FINGER_VERTICAL_SWIPE,
            requiredAction = TutorialRequiredAction.TWO_FINGER_FULLSCREEN_SWIPE,
            badgeAr = "إيماءة ذكية",
            badgeEn = "FULL SCREEN",
            titleAr = "اسحب بإصبعين لوضع ملء الشاشة",
            titleEn = "Two-Finger Full Screen Swipe",
            descriptionAr = "اسحب بإصبعين معاً للأعلى لإخفاء الشريط العلوي، وللأسفل لاستعادته مع بقاء المحتوى ثابتاً تماماً.",
            descriptionEn = "Swipe up with two fingers together to hide the status bar, or down to restore it.",
            instructionAr = "جرّب الآن: اسحب بإصبعين للأعلى أو للأسفل على الشاشة",
            instructionEn = "Try it now: Swipe up or down using two fingers",
            tipAr = "تمتد الخلفية للحافة العلوية بسلاسة دون تحرك العناصر",
            tipEn = "Background fills edge-to-edge while layout stays locked",
            accentColor = TutorialViolet,
            spotlightCornerRadiusDp = 24f,
            spotlightPaddingDp = 6f
        ),
        TutorialFeatureStep(
            id = "qr_sync_updates",
            targetScreen = TutorialScreenTarget.HOME,
            targetKeys = listOf("dock_tab_SETTINGS", "dock_nav"),
            gestureType = TutorialGestureType.SETTINGS_POINTER,
            requiredAction = TutorialRequiredAction.TAP_SETTINGS_TAB,
            badgeAr = "المشاركة والتحديثات",
            badgeEn = "SYNC & UPDATES",
            titleAr = "مشاركة QR، النسخ السحابي، والتحديثات",
            titleEn = "QR Share, Backup & Updates",
            descriptionAr = "افتح الإعدادات الآن لمشاركة القائمة بـ QR دون إنترنت، حفظ نسخة Google Drive، أو إعادة هذا الدليل.",
            descriptionEn = "Tap Settings to access offline QR sync, Google Drive backups, updates, and replay this guide.",
            instructionAr = "الخطوة الأخيرة: اضغط على تبويب الإعدادات المضيء بالأسفل",
            instructionEn = "Final step: Tap the highlighted Settings tab below",
            tipAr = "يمكنك دائماً إعادة تشغيل الدليل من قسم الإعدادات",
            tipEn = "You can replay this guide anytime inside Settings",
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
