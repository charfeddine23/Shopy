package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ShoppingItem
import com.example.ui.components.AppleLiquidGlassTopBarLayout
import com.example.ui.components.GlowingActionButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NeonBadge
import com.example.ui.components.ShopyDialog
import com.example.ui.components.SwipeToDeleteContainer
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.CheckedGreen
import com.example.ui.theme.CyanTintBg
import com.example.ui.theme.CyanTintBorder
import com.example.ui.theme.CyanTintText
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.MintTintBg
import com.example.ui.theme.MintTintText
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.categoryAccentColor
import com.example.ui.viewmodel.ShoppingViewModel
import com.example.util.LocalizationUtil

@Composable
fun SupermarketModeScreen(
    viewModel: ShoppingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.shoppingItems.collectAsStateWithLifecycle()
    val isArabic by viewModel.isArabic.collectAsStateWithLifecycle()
    val budgetLimit by viewModel.budgetLimitDzd.collectAsStateWithLifecycle()
    val isLight = androidx.compose.material3.MaterialTheme.colorScheme.background == BackgroundLight

    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var hideChecked by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    val stats = remember(items) { viewModel.calculateStats(items) }

    val displayItems by remember(items, hideChecked) {
        derivedStateOf {
            if (hideChecked) items.filter { !it.isChecked } else items
        }
    }

    val budgetProgress by animateFloatAsState(
        targetValue = if (budgetLimit > 0) {
            (stats.checkedTotalDzd / budgetLimit).toFloat().coerceIn(0f, 1f)
        } else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "budget_progress"
    )

    val progressColor by animateColorAsState(
        targetValue = when {
            stats.checkedTotalDzd > budgetLimit -> NeonRed
            stats.checkedTotalDzd >= budgetLimit * 0.85 -> NeonAmber
            else -> EmeraldSavings
        },
        label = "progress_color"
    )

    val listState = rememberLazyListState()

    AppleLiquidGlassTopBarLayout(
        listState = listState,
        modifier = modifier.fillMaxSize(),
        defaultHeaderHeight = 74.dp,
        topBar = {
            SupermarketModeHeader(
                isArabic = isArabic,
                onBack = onBack,
                hideChecked = hideChecked,
                onToggleHideChecked = { hideChecked = !hideChecked }
            )
        }
    ) { topPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topPadding + 4.dp, bottom = 114.dp)
        ) {
            // Live Dual Telemetry Budget HUD Card
            item {
                val remaining = budgetLimit - stats.checkedTotalDzd
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("supermarket_budget_hud"),
                    glowColor = progressColor
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "في السلة الآن" else "In Cart Now",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = LocalizationUtil.formatDzd(stats.checkedTotalDzd, isArabic),
                                    color = progressColor,
                                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 29.sp),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            // Edit Budget Pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isLight) CyanTintBg else ElectricCyan.copy(alpha = 0.16f))
                                    .border(
                                        BorderStroke(1.dp, if (isLight) CyanTintBorder else ElectricCyan.copy(alpha = 0.5f)),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        showEditBudgetDialog = true
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Budget",
                                    tint = if (isLight) CyanTintText else ElectricCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = LocalizationUtil.formatDzd(budgetLimit, isArabic),
                                    color = if (isLight) CyanTintText else ElectricCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Gradient Animated Budget Gauge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isLight) Color(0xFFE2E8F0) else Color(0x28FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(budgetProgress.coerceAtLeast(0.02f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(progressColor.copy(alpha = 0.85f), progressColor)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider(color = if (isLight) Color(0xFFE2EAF2) else Color(0x1CFFFFFF))

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isLight) MintTintBg else EmeraldSavings.copy(alpha = 0.16f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${stats.checkedCount}/${stats.totalCount}",
                                        color = if (isLight) MintTintText else EmeraldSavings,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "في العربة" else "items picked",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (remaining >= 0) {
                                Text(
                                    text = if (isArabic) {
                                        "المتبقي: ${LocalizationUtil.formatDzd(remaining, true)}"
                                    } else {
                                        "Left: ${LocalizationUtil.formatDzd(remaining, false)}"
                                    },
                                    color = if (isLight) MintTintText else EmeraldSavings,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = if (isArabic) {
                                        "تجاوز بـ ${LocalizationUtil.formatDzd(-remaining, true)}"
                                    } else {
                                        "Over by ${LocalizationUtil.formatDzd(-remaining, false)}"
                                    },
                                    color = NeonRed,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Swipe hint
            if (displayItems.isNotEmpty()) {
                item {
                    Text(
                        text = if (isArabic) "👈 اسحب أي عنصر لحذفه من السلة • اضغط على البطاقة لتحديد الشراء" else "👉 Swipe to remove • Tap card to check off",
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }

            // High Contrast Aisle Items List with Swipe-to-Delete
            items(displayItems, key = { it.id }) { item ->
                SwipeToDeleteContainer(
                    onDelete = { viewModel.deleteItem(item) },
                    isArabic = isArabic,
                    deleteLabel = if (isArabic) "حذف من السلة" else "Remove from cart",
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                ) {
                    SupermarketItemCard(
                        item = item,
                        isArabic = isArabic,
                        onToggleChecked = { viewModel.toggleChecked(item.id, !item.isChecked) },
                        onIncrement = { viewModel.updateQuantity(item.id, 1) },
                        onDecrement = { viewModel.updateQuantity(item.id, -1) }
                    )
                }
            }
        }

        // Floating Liquid Glass Bottom Checkout Bar
        val bottomFadeColors = if (isLight) {
            listOf(Color.Transparent, Color(0xEBF2F6F9), BackgroundLight)
        } else {
            listOf(Color.Transparent, Color(0xEB0B0E14), Color(0xFF0B0E14))
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(bottomFadeColors))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            GlowingActionButton(
                text = if (isArabic) "إنهاء التسوق وتفريغ المكتمل (${stats.checkedCount}/${stats.totalCount})" else "Finish Shopping (${stats.checkedCount}/${stats.totalCount})",
                icon = Icons.Default.DoneAll,
                color = EmeraldSavings,
                onClick = { showFinishDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("finish_shopping_bottom_button")
            )
        }
    }

    // Edit Budget Dialog
    if (showEditBudgetDialog) {
        var newBudgetText by remember { mutableStateOf(budgetLimit.toInt().toString()) }
        ShopyDialog(
            onDismissRequest = { showEditBudgetDialog = false },
            accentColor = ElectricCyan,
            title = {
                Text(
                    text = if (isArabic) "تحديد ميزانية التسوق" else "Set Shopping Budget",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newBudgetText,
                    onValueChange = { newBudgetText = it },
                    label = { Text(if (isArabic) "الميزانية بالدينار الجزائري (د.ج)" else "Budget (DZD)") },
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldSavings,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                GlowingActionButton(
                    text = if (isArabic) "حفظ" else "Save",
                    color = EmeraldSavings,
                    onClick = {
                        val parsed = newBudgetText.toDoubleOrNull() ?: budgetLimit
                        viewModel.setBudgetLimit(parsed)
                        showEditBudgetDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showEditBudgetDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Finish Shopping Summary Celebration Dialog
    if (showFinishDialog) {
        ShopyDialog(
            onDismissRequest = { showFinishDialog = false },
            accentColor = EmeraldSavings,
            title = {
                Text(
                    text = if (isArabic) "اكتملت جولة التسوق بنجاح!" else "Shopping Completed!",
                    color = EmeraldSavings,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isArabic) {
                            "قمت بشراء ${stats.checkedCount} عنصر بتكلفة إجمالية قدرها:"
                        } else {
                            "You purchased ${stats.checkedCount} items for a total of:"
                        },
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = LocalizationUtil.formatDzd(stats.checkedTotalDzd, isArabic),
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                GlowingActionButton(
                    text = if (isArabic) "حذف العناصر المكتملة" else "Clear Purchased Items",
                    color = EmeraldSavings,
                    onClick = {
                        viewModel.clearChecked()
                        showFinishDialog = false
                        onBack()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text(if (isArabic) "إغلاق" else "Close", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SupermarketModeHeader(
    isArabic: Boolean,
    onBack: () -> Unit,
    hideChecked: Boolean,
    onToggleHideChecked: () -> Unit
) {
    val isLight = androidx.compose.material3.MaterialTheme.colorScheme.background == BackgroundLight
    val filterActiveColor = if (isLight) CyanTintText else ElectricCyan

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isLight) Color.White else Color(0x33FFFFFF))
                    .border(
                        BorderStroke(1.dp, if (isLight) LightCardBorder else Color.Transparent),
                        CircleShape
                    )
                    .size(42.dp)
                    .testTag("supermarket_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isArabic) "وضع السوبرماركت" else "Supermarket Mode",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    NeonBadge(
                        text = "LIVE HUD",
                        accentColor = EmeraldSavings
                    )
                }
                Text(
                    text = if (isArabic) "أزرار كبيرة للتحكم بيد واحدة أثناء التسوق" else "One-thumb reachability inside the aisles",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Filter Toggle
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (hideChecked) {
                        if (isLight) CyanTintBg else ElectricCyan.copy(alpha = 0.25f)
                    } else {
                        if (isLight) Color.White else Color(0x20FFFFFF)
                    }
                )
                .border(
                    BorderStroke(
                        1.dp,
                        if (hideChecked) {
                            if (isLight) CyanTintBorder else ElectricCyan
                        } else {
                            GlassBorder
                        }
                    ),
                    RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onToggleHideChecked)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (hideChecked) {
                    if (isArabic) "عرض الكل" else "Show All"
                } else {
                    if (isArabic) "إخفاء المكتمل" else "Hide Done"
                },
                color = if (hideChecked) filterActiveColor else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SupermarketItemCard(
    item: ShoppingItem,
    isArabic: Boolean,
    onToggleChecked: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    val catColor = categoryAccentColor(item.category, isLight)
    val checkColor by animateColorAsState(
        targetValue = if (item.isChecked) CheckedGreen else Color.Transparent,
        label = "big_check_color"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (item.isChecked) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "big_check_scale"
    )

    val cardBackground = if (item.isChecked) {
        if (isLight) Color(0xFFF3F6FA) else Color(0xFF0E1628)
    } else {
        if (isLight) Color(0xFFFFFFFF) else Color(0xFF131D31)
    }

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("supermarket_item_${item.id}"),
        backgroundColor = cardBackground,
        glowColor = if (item.isChecked) null else catColor.copy(alpha = 0.5f),
        onClick = onToggleChecked
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical Category Stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (item.isChecked) CheckedGreen.copy(alpha = 0.5f) else catColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Extra-Large 54dp High-Contrast Check Button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .scale(checkScale)
                    .clip(CircleShape)
                    .background(checkColor)
                    .border(
                        BorderStroke(
                            2.5.dp,
                            if (item.isChecked) CheckedGreen else if (isLight) Color(0xFFCBD5E1) else Color(0x80FFFFFF)
                        ),
                        CircleShape
                    )
                    .clickable(onClick = onToggleChecked)
                    .testTag("supermarket_toggle_btn_${item.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (item.isChecked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Checked",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Oversized Title and Subtotal Info
            Column(modifier = Modifier.weight(1f)) {
                val rawTitle = if (isArabic) item.nameAr else item.name
                val displayTitle = if (rawTitle.contains("(") && rawTitle.contains(")")) {
                    val main = rawTitle.substringBefore("(").trim()
                    val details = rawTitle.substringAfter("(").substringBefore(")").trim()
                    "$main - $details"
                } else if (!rawTitle.contains(" - ")) {
                    val catLabel = LocalizationUtil.getCategoryLabel(item.category, isArabic)
                    "$rawTitle - $catLabel"
                } else {
                    rawTitle
                }

                Text(
                    text = displayTitle,
                    color = if (item.isChecked) TextMuted else TextPrimary,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.quantity} ${item.unit}",
                        color = if (isLight) CyanTintText else ElectricCyan,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (item.estimatedPriceDzd > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLight) MintTintBg else EmeraldSavings.copy(alpha = 0.16f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = LocalizationUtil.formatDzd(item.estimatedPriceDzd * item.quantity, isArabic),
                                color = if (isLight) MintTintText else EmeraldSavings,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Quantity Stepper with live quantity display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isLight) Color(0xFFF1F5F9) else Color(0x381C283E))
                    .border(
                        BorderStroke(1.dp, if (isLight) LightCardBorder else Color(0x18FFFFFF)),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDecrement),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "–",
                        color = if (isLight) CyanTintText else ElectricCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${item.quantity}",
                    color = TextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onIncrement),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+",
                        color = if (isLight) CyanTintText else ElectricCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
