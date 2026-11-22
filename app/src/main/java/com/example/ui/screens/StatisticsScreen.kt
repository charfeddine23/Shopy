package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SpendingRecord
import com.example.ui.components.AppleLiquidGlassTopBarLayout
import com.example.ui.components.GlowingActionButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NeonBadge
import com.example.ui.components.ShopyDialog
import com.example.ui.components.SouqSectionHeader
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.CyanTintBg
import com.example.ui.theme.CyanTintBorder
import com.example.ui.theme.CyanTintText
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.MintTintBg
import com.example.ui.theme.MintTintBorder
import com.example.ui.theme.MintTintText
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletDark
import com.example.ui.theme.categoryAccentColor
import com.example.ui.viewmodel.ShoppingViewModel
import com.example.util.LocalizationUtil
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: ShoppingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isArabic by viewModel.isArabic.collectAsStateWithLifecycle()
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    val spendingRecords by viewModel.spendingRecords.collectAsStateWithLifecycle()

    var showManualAddDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<SpendingRecord?>(null) }

    val savedStatsMonth by viewModel.selectedStatsMonth.collectAsStateWithLifecycle()

    // Dynamic distinct month keys
    val availableMonths = remember(spendingRecords) {
        val keys = spendingRecords.map { it.monthKey }.distinct().sortedDescending()
        if (keys.isEmpty()) listOf("2026-11") else keys
    }

    val selectedMonth = remember(savedStatsMonth, availableMonths) {
        if (savedStatsMonth == "ALL" || availableMonths.contains(savedStatsMonth)) {
            savedStatsMonth
        } else {
            availableMonths.firstOrNull() ?: "2026-11"
        }
    }

    val filteredRecords = remember(spendingRecords, selectedMonth) {
        if (selectedMonth == "ALL") spendingRecords
        else spendingRecords.filter { it.monthKey == selectedMonth }
    }

    val totalSpent = remember(filteredRecords) {
        filteredRecords.sumOf { it.totalSpentDzd }
    }

    val tripCount = filteredRecords.size
    val totalItems = remember(filteredRecords) { filteredRecords.sumOf { it.itemsCount } }
    val avgBasket = if (tripCount > 0) totalSpent / tripCount else 0.0

    // Category breakdown
    val categoryBreakdown = remember(filteredRecords) {
        filteredRecords.groupBy { it.topCategory }
            .mapValues { (_, records) -> records.sumOf { it.totalSpentDzd } }
            .toList()
            .sortedByDescending { it.second }
    }

    val listState = rememberLazyListState()

    AppleLiquidGlassTopBarLayout(
        listState = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("statistics_screen"),
        defaultHeaderHeight = 72.dp,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isLight) Color.White else Color(0x28FFFFFF))
                            .border(
                                BorderStroke(1.dp, if (isLight) LightCardBorder else Color(0x28FFFFFF)),
                                CircleShape
                            )
                            .testTag("statistics_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isLight) CyanTintText else TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isArabic) "الإحصائيات والإنفاق" else "Spending Analytics",
                            color = TextPrimary,
                            fontSize = 18.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isArabic) "تحليل الميزانية وجولات التسوق" else "Budget insights & shopping trips",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Add Expense Quick Action
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isLight) MintTintBg else EmeraldSavings.copy(alpha = 0.18f))
                        .border(
                            BorderStroke(1.dp, if (isLight) MintTintBorder else EmeraldSavings.copy(alpha = 0.55f)),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { showManualAddDialog = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("add_manual_expense_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isLight) MintTintText else EmeraldSavings,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "تسجيل نفقة" else "Log Trip",
                            color = if (isLight) MintTintText else EmeraldSavings,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { topPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp, top = topPadding + 4.dp)
        ) {
            // Month Selector Filter Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val monthsList = listOf("ALL") + availableMonths
                    monthsList.forEach { mKey ->
                        val isSelected = selectedMonth == mKey
                        val label = when (mKey) {
                            "ALL" -> if (isArabic) "كافة الجولات" else "All Time"
                            "2026-11" -> if (isArabic) "نوفمبر 2026" else "Nov 2026"
                            "2026-10" -> if (isArabic) "أكتوبر 2026" else "Oct 2026"
                            "2026-09" -> if (isArabic) "سبتمبر 2026" else "Sep 2026"
                            else -> mKey
                        }

                        val pillBg = if (isLight) {
                            if (isSelected) CyanTintBg else Color.White
                        } else {
                            if (isSelected) ElectricCyan.copy(alpha = 0.22f) else Color(0x18FFFFFF)
                        }
                        val pillBorder = if (isLight) {
                            if (isSelected) BorderStroke(1.5.dp, CyanTintBorder) else BorderStroke(1.dp, LightCardBorder)
                        } else {
                            if (isSelected) BorderStroke(1.dp, ElectricCyan) else BorderStroke(1.dp, Color(0x20FFFFFF))
                        }
                        val pillTextColor = if (isLight) {
                            if (isSelected) CyanTintText else TextSecondary
                        } else {
                            if (isSelected) ElectricCyan else TextSecondary
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(pillBg)
                                .border(pillBorder, RoundedCornerShape(12.dp))
                                .clickable { viewModel.setSelectedStatsMonth(mKey) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = pillTextColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    color = pillTextColor,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Grand Total Spending Executive Bento Card
            item {
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    glowColor = if (isLight) VioletDark else VioletAccent
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "إجمالي الإنفاق للفترة المحددة" else "Total Period Spending",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = LocalizationUtil.formatDzd(totalSpent, isArabic),
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            NeonBadge(
                                text = if (isArabic) "$tripCount جولات" else "$tripCount Trips",
                                accentColor = ElectricCyan,
                                icon = Icons.Default.Receipt
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = if (isLight) Color(0xFFE2EAF2) else Color(0x1CFFFFFF))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick 3 Metrics Bento Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricPill(
                                label = if (isArabic) "متوسط السلة" else "Avg Basket",
                                value = LocalizationUtil.formatDzd(avgBasket, isArabic),
                                color = if (isLight) MintTintText else EmeraldSavings,
                                modifier = Modifier.weight(1f)
                            )
                            MetricPill(
                                label = if (isArabic) "المواد المشتراة" else "Items",
                                value = "$totalItems " + if (isArabic) "مادة" else "pcs",
                                color = if (isLight) CyanTintText else ElectricCyan,
                                modifier = Modifier.weight(1f)
                            )
                            MetricPill(
                                label = if (isArabic) "المتاجر" else "Stores",
                                value = "${filteredRecords.map { it.storeName }.distinct().size} " + if (isArabic) "متاجر" else "stores",
                                color = if (isLight) Color(0xFFD97706) else NeonAmber,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Visual Spending Trend (Weekly bars)
            item {
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    glowColor = EmeraldSavings
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SouqSectionHeader(
                            icon = Icons.Default.Insights,
                            title = if (isArabic) "توزيع الإنفاق الأسبوعي" else "Weekly Spending Trend",
                            accentColor = if (isLight) MintTintText else EmeraldSavings
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val weeklyTotals = remember(filteredRecords) {
                            val buckets = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
                            val cal = Calendar.getInstance()
                            filteredRecords.forEach { rec ->
                                cal.timeInMillis = rec.timestamp
                                val day = cal.get(Calendar.DAY_OF_MONTH)
                                val idx = when {
                                    day <= 7 -> 0
                                    day <= 14 -> 1
                                    day <= 21 -> 2
                                    else -> 3
                                }
                                buckets[idx] += rec.totalSpentDzd.coerceAtLeast(0.0)
                            }
                            buckets
                        }
                        val maxWeekVal = (weeklyTotals.maxOrNull() ?: 0.0).coerceAtLeast(1000.0)

                        val weeks = listOf(
                            Pair(if (isArabic) "الأسبوع 1" else "Week 1", weeklyTotals[0]),
                            Pair(if (isArabic) "الأسبوع 2" else "Week 2", weeklyTotals[1]),
                            Pair(if (isArabic) "الأسبوع 3" else "Week 3", weeklyTotals[2]),
                            Pair(if (isArabic) "الأسبوع 4" else "Week 4", weeklyTotals[3])
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            weeks.forEach { (weekLabel, amount) ->
                                val fraction = if (amount <= 0.0) 0.08f else (amount / maxWeekVal).toFloat().coerceIn(0.14f, 1f)
                                val animatedHeight by animateFloatAsState(
                                    targetValue = fraction,
                                    animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
                                    label = "bar_anim"
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Text(
                                        text = "${amount.toInt()}",
                                        color = if (amount > 0) TextPrimary else TextMuted,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(38.dp)
                                            .fillMaxHeight(animatedHeight)
                                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                            .background(
                                                if (amount > 0) {
                                                    Brush.verticalGradient(listOf(EmeraldSavings, ElectricCyan))
                                                } else {
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            if (isLight) Color(0xFFE2E8F0) else Color(0x22FFFFFF),
                                                            if (isLight) Color(0xFFE2E8F0) else Color(0x14FFFFFF)
                                                        )
                                                    )
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = weekLabel,
                                        color = TextSecondary,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Breakdown Section
            if (categoryBreakdown.isNotEmpty()) {
                item {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        glowColor = NeonAmber
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SouqSectionHeader(
                                icon = Icons.Default.Category,
                                title = if (isArabic) "أعلى الفئات إنفاقاً" else "Top Expense Categories",
                                accentColor = NeonAmber
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            categoryBreakdown.take(4).forEach { (category, amount) ->
                                val pct = if (totalSpent > 0) (amount / totalSpent * 100).toInt() else 0
                                val catLabel = LocalizationUtil.getCategoryLabel(category, isArabic)
                                val catColor = categoryAccentColor(category, isLight)
                                val animatedPct by animateFloatAsState(
                                    targetValue = (pct / 100f).coerceIn(0.05f, 1f),
                                    animationSpec = tween(durationMillis = 550),
                                    label = "cat_bar_$category"
                                )

                                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(catColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = catLabel,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = "${LocalizationUtil.formatDzd(amount, isArabic)} ($pct%)",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isLight) Color(0xFFE2E8F0) else Color(0x20FFFFFF))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(animatedPct)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(catColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // History Section Title
            item {
                SouqSectionHeader(
                    icon = Icons.Default.ShoppingBag,
                    title = if (isArabic) "جولات التسوق المكتملة (${filteredRecords.size})" else "Completed Trips (${filteredRecords.size})",
                    subtitle = if (isArabic) "تسجّل تلقائياً عند إنهاء التسوق" else "Recorded automatically on trip completion",
                    accentColor = if (isLight) CyanTintText else ElectricCyan,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Completed Shopping Trips List
            if (filteredRecords.isEmpty()) {
                item {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isArabic) "لا توجد جولات تسوق مسجلة لهذا الشهر" else "No shopping trips recorded for this month",
                                color = TextSecondary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isArabic) "اضغط على \"إنهاء التسوق\" في الرئيسية لنقل النفقات فوراً" else "Tap 'Finish Trip' on Home to log trips here",
                                color = TextMuted,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredRecords, key = { it.id }) { record ->
                    TripRecordCard(
                        record = record,
                        isArabic = isArabic,
                        isLight = isLight,
                        onDelete = { recordToDelete = record },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        // Manual Add Dialog
        if (showManualAddDialog) {
            ManualExpenseDialog(
                isArabic = isArabic,
                onDismiss = { showManualAddDialog = false },
                onConfirm = { storeName, storeNameAr, amount, count, cat, notes ->
                    viewModel.addManualSpendingTrip(storeName, storeNameAr, amount, count, cat, notes)
                    showManualAddDialog = false
                }
            )
        }

        // Confirm Delete Record Dialog
        recordToDelete?.let { rec ->
            ShopyDialog(
                onDismissRequest = { recordToDelete = null },
                accentColor = Color(0xFFEF4444),
                title = {
                    Text(
                        text = if (isArabic) "حذف جولة التسوق" else "Delete Trip",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (isArabic) "هل أنت متأكد من حذف هذه الجولة المسجلة بقيمة ${LocalizationUtil.formatDzd(rec.totalSpentDzd, isArabic)}؟"
                        else "Are you sure you want to delete this trip (${LocalizationUtil.formatDzd(rec.totalSpentDzd, isArabic)})?",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteSpendingRecord(rec.id)
                            recordToDelete = null
                        }
                    ) {
                        Text(if (isArabic) "حذف" else "Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recordToDelete = null }) {
                        Text(if (isArabic) "إلغاء" else "Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    val pillBg = if (isLight) Color(0xFFF8FAFC) else Color(0x18FFFFFF)
    val pillBorder = if (isLight) BorderStroke(1.dp, LightCardBorder) else BorderStroke(1.dp, Color(0x22FFFFFF))

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(pillBg)
            .border(pillBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TripRecordCard(
    record: SpendingRecord,
    isArabic: Boolean,
    isLight: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        glowColor = EmeraldSavings.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isLight) MintTintBg else EmeraldSavings.copy(alpha = 0.18f))
                        .border(
                            BorderStroke(1.dp, if (isLight) MintTintBorder else EmeraldSavings.copy(alpha = 0.55f)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isLight) MintTintText else EmeraldSavings,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isArabic) record.storeNameAr else record.storeName,
                            color = TextPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = record.dateStr,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    if (record.itemsSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${record.itemsCount} " + (if (isArabic) "عناصر: " else "items: ") + record.itemsSummary,
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isLight) MintTintBg else EmeraldSavings.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = LocalizationUtil.formatDzd(record.totalSpentDzd, isArabic),
                        color = if (isLight) MintTintText else EmeraldSavings,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualExpenseDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (storeName: String, storeNameAr: String, amount: Double, count: Int, cat: String, notes: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("Uno Hypermarket") }
    var storeNameAr by remember { mutableStateOf("هايبرماركت أونو") }
    var countText by remember { mutableStateOf("5") }
    var notes by remember { mutableStateOf("") }

    val parsedAmount = amountText.replace(",", ".").toDoubleOrNull()
    val parsedCount = countText.toIntOrNull()?.coerceIn(1, 999) ?: 1
    val activeStore = if (isArabic) storeNameAr else storeName
    val canSave = parsedAmount != null && parsedAmount > 0.0 && parsedAmount.isFinite() && activeStore.isNotBlank()

    ShopyDialog(
        onDismissRequest = onDismiss,
        accentColor = EmeraldSavings,
        title = {
            Text(
                text = if (isArabic) "تسجيل نفقة تسوق يدوية" else "Log Manual Expense",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(if (isArabic) "مجموع المبلغ بالدينار (د.ج)" else "Total Spent (DZD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = activeStore,
                    onValueChange = { value ->
                        if (isArabic) {
                            storeNameAr = value
                            if (storeName == "Uno Hypermarket" || storeName.isBlank()) {
                                storeName = value
                            }
                        } else {
                            storeName = value
                            if (storeNameAr == "هايبرماركت أونو" || storeNameAr.isBlank()) {
                                storeNameAr = value
                            }
                        }
                    },
                    label = { Text(if (isArabic) "اسم المتجر / السوبرماركت" else "Store Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it },
                    label = { Text(if (isArabic) "عدد السلع المشتراة" else "Item Count") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isArabic) "ملاحظات / أمثلة المشتريات" else "Notes / Items bought") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            GlowingActionButton(
                text = if (isArabic) "حفظ في الإحصائيات" else "Save Expense",
                color = EmeraldSavings,
                enabled = canSave,
                onClick = {
                    val safeAmount = parsedAmount ?: return@GlowingActionButton
                    val safeStoreEn = storeName.ifBlank { storeNameAr }.trim()
                    val safeStoreAr = storeNameAr.ifBlank { storeName }.trim()
                    onConfirm(safeStoreEn, safeStoreAr, safeAmount, parsedCount, "Pantry", notes.trim())
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel", color = TextSecondary)
            }
        }
    )
}
