package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ShoppingItem
import com.example.ui.components.AppleLiquidGlassTopBarLayout
import com.example.ui.components.GlowingActionButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidSheetShape
import com.example.ui.components.PullUpGestureState
import com.example.ui.components.ShopyDialog
import com.example.ui.components.SouqSectionHeader
import com.example.ui.components.SwipeToDeleteContainer
import com.example.ui.components.WindGlowAppNameBanner
import com.example.ui.components.disableParentPagerSwipe
import com.example.ui.components.horizontalFadingEdge
import com.example.ui.components.liquidWindowContainerColor
import com.example.ui.components.singleFingerPullUpToAdd
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.CheckedGreen
import com.example.ui.theme.CyanTintBg
import com.example.ui.theme.CyanTintBorder
import com.example.ui.theme.CyanTintText
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSavings
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurfaceLight
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
import com.example.util.ProductGroup
import com.example.util.ProductVariant
import com.example.util.SmartSearchUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ShoppingViewModel,
    onNavigateToPriceComparison: () -> Unit,
    onNavigateToSupermarketMode: () -> Unit,
    onNavigateToStatistics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val items by viewModel.shoppingItems.collectAsStateWithLifecycle()
    val isArabic by viewModel.isArabic.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isDynamicGlowEnabled by viewModel.isDynamicGlowEnabled.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showFinishTripDialog by remember { mutableStateOf(false) }
    var showListSearch by remember { mutableStateOf(false) }
    var listSearchQuery by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val filteredItems by remember(items, selectedCategory, listSearchQuery) {
        derivedStateOf {
            val byCategory = if (selectedCategory == "All") {
                items
            } else {
                items.filter { it.category.equals(selectedCategory, ignoreCase = true) }
            }
            val q = listSearchQuery.trim()
            if (q.isEmpty()) {
                byCategory
            } else {
                byCategory.filter {
                    it.name.contains(q, ignoreCase = true) ||
                        it.nameAr.contains(q, ignoreCase = true) ||
                        it.category.contains(q, ignoreCase = true)
                }
            }
        }
    }

    LaunchedEffect(items) {
        if (selectedCategory != "All" && items.none { it.category.equals(selectedCategory, ignoreCase = true) }) {
            viewModel.setSelectedCategory("All")
        }
    }

    val stats = remember(items) { viewModel.calculateStats(items) }
    val listState = rememberLazyListState()
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight

    AppleLiquidGlassTopBarLayout(
        listState = listState,
        modifier = modifier.fillMaxSize(),
        defaultHeaderHeight = 82.dp,
        topBar = { blurProgress ->
            HomeHeader(
                isArabic = isArabic,
                isDynamicGlowEnabled = isDynamicGlowEnabled,
                blurProgress = blurProgress,
                itemCount = stats.totalCount,
                onStartSupermarketMode = onNavigateToSupermarketMode
            )
        }
    ) { topPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 104.dp, top = topPadding + 4.dp)
        ) {
            // Artistic Split-Hero Summary Card + Quick Action Dock
            item {
                HomeSummaryHud(
                    stats = stats,
                    isArabic = isArabic,
                    onFinishShopping = { showFinishTripDialog = true },
                    onQuickAdd = { showAddDialog = true },
                    onOpenProducts = onNavigateToPriceComparison,
                    onOpenAnalytics = onNavigateToStatistics,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Frequent Essentials (One-tap carousel)
            item {
                FrequentItemsSection(
                    presets = viewModel.frequentPresets,
                    isArabic = isArabic,
                    onAddPreset = { viewModel.addFrequentPreset(it) }
                )
            }

            // Category Filter Chips
            item {
                CategoryChipsSection(
                    items = items,
                    selectedCategory = selectedCategory,
                    isArabic = isArabic,
                    onSelectCategory = { category ->
                        viewModel.setSelectedCategory(category)
                    }
                )
            }

            // Section Header with Search Toggle & Clear Done Pill
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    SouqSectionHeader(
                        icon = Icons.Default.ShoppingBag,
                        title = if (isArabic) "قائمة المشتريات (${filteredItems.size})" else "Shopping List (${filteredItems.size})",
                        subtitle = if (stats.totalCount > 0) {
                            if (isArabic) "${stats.totalCount - stats.checkedCount} متبقٍ في السلة" else "${stats.totalCount - stats.checkedCount} remaining"
                        } else null,
                        accentColor = if (isLight) CyanTintText else ElectricCyan,
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AnimatedVisibility(visible = stats.checkedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(NeonAmber.copy(alpha = if (isLight) 0.14f else 0.16f))
                                            .border(
                                                BorderStroke(1.dp, NeonAmber.copy(alpha = 0.45f)),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { viewModel.clearChecked() }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("clear_checked_button")
                                    ) {
                                        Text(
                                            text = if (isArabic) "حذف المكتمل (${stats.checkedCount})" else "Clear (${stats.checkedCount})",
                                            color = if (isLight) Color(0xFFB45309) else NeonAmber,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (items.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            showListSearch = !showListSearch
                                            if (!showListSearch) listSearchQuery = ""
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (showListSearch) {
                                                    if (isLight) CyanTintBg else ElectricCyan.copy(alpha = 0.2f)
                                                } else {
                                                    if (isLight) Color.White else Color(0x1EFFFFFF)
                                                }
                                            )
                                            .border(
                                                BorderStroke(1.dp, if (showListSearch) CyanTintBorder else GlassBorder),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = if (showListSearch) Icons.Default.Close else Icons.Default.Search,
                                            contentDescription = if (isArabic) "بحث في القائمة" else "Filter list",
                                            tint = if (showListSearch) (if (isLight) CyanTintText else ElectricCyan) else TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )

                    AnimatedVisibility(
                        visible = showListSearch && items.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isLight) Color.White else Color(0xFF131D31))
                                .border(
                                    BorderStroke(1.dp, if (isLight) CyanTintBorder else ElectricCyan.copy(alpha = 0.45f)),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (isLight) CyanTintText else ElectricCyan,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = listSearchQuery,
                                    onValueChange = { listSearchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = TextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(ElectricCyan),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 10.dp),
                                    decorationBox = { innerTextField ->
                                        if (listSearchQuery.isEmpty()) {
                                            Text(
                                                text = if (isArabic) "تصفية عناصر القائمة..." else "Filter list items...",
                                                color = TextMuted,
                                                fontSize = 13.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (listSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { listSearchQuery = "" },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Shopping Items List
            if (filteredItems.isEmpty()) {
                item {
                    EmptyListCard(
                        isArabic = isArabic,
                        hasSearchFilter = listSearchQuery.isNotBlank(),
                        onClearFilter = { listSearchQuery = "" },
                        onAddItem = { showAddDialog = true }
                    )
                }
            } else {
                items(
                    items = filteredItems,
                    key = { it.id }
                ) { item ->
                    SwipeToDeleteContainer(
                        onDelete = { viewModel.deleteItem(item) },
                        isArabic = isArabic,
                        deleteLabel = if (isArabic) "حذف" else "Delete",
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        ShoppingItemRow(
                            item = item,
                            isArabic = isArabic,
                            onToggleChecked = { viewModel.toggleChecked(item.id, !item.isChecked) },
                            onIncrement = { viewModel.updateQuantity(item.id, 1) },
                            onDecrement = { viewModel.updateQuantity(item.id, -1) },
                            onDelete = { viewModel.deleteItem(item) },
                            onItemClick = onNavigateToPriceComparison
                        )
                    }
                }
            }
        }
    }

    // Add Item Modal Bottom Sheet
    if (showAddDialog) {
        val productGroups by viewModel.productGroups.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = { showAddDialog = false },
            sheetState = sheetState,
            shape = LiquidSheetShape,
            containerColor = liquidWindowContainerColor(isLight),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(44.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isLight) Color(0xFFCBD5E1) else Color(0x40FFFFFF))
                )
            }
        ) {
            AddItemSheetContent(
                isArabic = isArabic,
                productGroups = productGroups,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showAddDialog = false }
                },
                onAutoCategorize = { n, na -> viewModel.autoCategorize(n, na) },
                onAddItem = { name, nameAr, category, qty, unit, barcode, price ->
                    viewModel.addItem(name, nameAr, category, qty, unit, barcode, price)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showAddDialog = false }
                }
            )
        }
    }

    // Finish Shopping Trip & Transfer to Analytics Dialog
    if (showFinishTripDialog) {
        val stores = LocalizationUtil.defaultAlgerianStores
        var selectedStoreIndex by remember { mutableStateOf(0) }

        ShopyDialog(
            onDismissRequest = { showFinishTripDialog = false },
            accentColor = EmeraldSavings,
            title = {
                Text(
                    text = if (isArabic) "إنهاء التسوق ونقل المجموع للإنفاق" else "Finish Shopping Trip",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isArabic)
                            "سيتم إنهاء التسوق ونقل مجموع السلة المقدر بـ ${LocalizationUtil.formatDzd(stats.grandTotalDzd, isArabic)} فورياً إلى صفحة الإحصائيات والإنفاق وتفريغ السلة."
                        else
                            "This will transfer ${LocalizationUtil.formatDzd(stats.grandTotalDzd, isArabic)} to your monthly spending analytics and clear the cart.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isArabic) "اختر متجر الشراء:" else "Select store:",
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalFadingEdge(fadeWidth = 24.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        stores.forEachIndexed { index, (en, ar) ->
                            val isSelected = selectedStoreIndex == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) (if (isLight) CyanTintBg else ElectricCyan.copy(alpha = 0.25f)) else (if (isLight) Color(0xFFF1F5F9) else Color(0x18FFFFFF)))
                                    .border(BorderStroke(1.dp, if (isSelected) ElectricCyan else Color.Transparent), RoundedCornerShape(10.dp))
                                    .clickable { selectedStoreIndex = index }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = if (isArabic) ar else en,
                                    color = if (isSelected) (if (isLight) CyanTintText else ElectricCyan) else TextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                GlowingActionButton(
                    text = if (isArabic) "تأكيد ونقل للإحصائيات" else "Confirm & Transfer",
                    color = EmeraldSavings,
                    onClick = {
                        val store = stores[selectedStoreIndex]
                        viewModel.finishShoppingTrip(storeName = store.first, storeNameAr = store.second)
                        showFinishTripDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showFinishTripDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun HomeHeader(
    isArabic: Boolean,
    isDynamicGlowEnabled: Boolean,
    blurProgress: Float = 0f,
    itemCount: Int = 0,
    onStartSupermarketMode: () -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    val shopPillBg = if (isLight) MintTintBg else EmeraldSavings.copy(alpha = 0.18f)
    val shopPillBorder = if (isLight) MintTintBorder else EmeraldSavings.copy(alpha = 0.8f)
    val shopPillContent = if (isLight) MintTintText else EmeraldSavings

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        WindGlowAppNameBanner(
            isArabic = isArabic,
            isGlowEnabled = isDynamicGlowEnabled,
            blurProgress = blurProgress,
            trailingAction = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(shopPillBg)
                        .border(BorderStroke(1.2.dp, shopPillBorder), RoundedCornerShape(20.dp))
                        .clickable(onClick = onStartSupermarketMode)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("supermarket_mode_header_pill"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = if (isArabic) "وضع السوبرماركت" else "Supermarket Mode",
                            tint = shopPillContent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "السلة" else "Cart",
                            color = shopPillContent,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (itemCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(shopPillContent.copy(alpha = if (isLight) 0.16f else 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$itemCount",
                                    color = shopPillContent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun HomeSummaryHud(
    stats: com.example.ui.viewmodel.ShoppingStats,
    isArabic: Boolean,
    onFinishShopping: () -> Unit = {},
    onQuickAdd: () -> Unit = {},
    onOpenProducts: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    val progressTarget = if (stats.totalCount > 0) {
        (stats.checkedCount.toFloat() / stats.totalCount.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "home_progress_ring"
    )
    val ringColor = if (stats.totalCount > 0 && stats.checkedCount == stats.totalCount) {
        EmeraldSavings
    } else if (isLight) CyanTintText else ElectricCyan

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_summary_hud"),
        glowColor = ringColor
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(ringColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "المجموع التقديري للسلة" else "Estimated Basket Total",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = LocalizationUtil.formatDzd(stats.grandTotalDzd, isArabic),
                        color = TextPrimary,
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (stats.totalCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isLight) MintTintBg else EmeraldSavings.copy(alpha = 0.18f))
                                    .border(
                                        BorderStroke(1.dp, if (isLight) MintTintBorder else EmeraldSavings.copy(alpha = 0.55f)),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable(onClick = onFinishShopping)
                                    .padding(horizontal = 11.dp, vertical = 6.dp)
                                    .testTag("finish_shopping_hud_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isLight) MintTintText else EmeraldSavings,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isArabic) "إنهاء ونقل للإنفاق" else "Finish Trip",
                                        color = if (isLight) MintTintText else EmeraldSavings,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Circular Animated Completion Ring
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = if (isLight) Color(0xFFE2E8F0) else Color(0x26FFFFFF)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 6.5.dp.toPx()
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                        if (animatedProgress > 0f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(ringColor, EmeraldSavings, ringColor)
                                ),
                                startAngle = -90f,
                                sweepAngle = animatedProgress * 360f,
                                useCenter = false,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${stats.checkedCount}/${stats.totalCount}",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isArabic) "جاهز" else "done",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = if (isLight) Color(0xFFE2EAF2) else Color(0x1CFFFFFF),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3-Button Quick Action Dock for effortless navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroQuickActionButton(
                    icon = Icons.Default.Add,
                    label = if (isArabic) "إضافة عنصر" else "Quick Add",
                    accentColor = if (isLight) CyanTintText else ElectricCyan,
                    bgColor = if (isLight) CyanTintBg else ElectricCyan.copy(alpha = 0.16f),
                    borderColor = if (isLight) CyanTintBorder else ElectricCyan.copy(alpha = 0.55f),
                    onClick = onQuickAdd,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("add_item_action_button")
                )

                HeroQuickActionButton(
                    icon = Icons.Default.Storefront,
                    label = if (isArabic) "المنتجات" else "Products",
                    accentColor = if (isLight) MintTintText else EmeraldSavings,
                    bgColor = if (isLight) MintTintBg else EmeraldSavings.copy(alpha = 0.14f),
                    borderColor = if (isLight) MintTintBorder else EmeraldSavings.copy(alpha = 0.45f),
                    onClick = onOpenProducts,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_products_shortcut")
                )

                HeroQuickActionButton(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = if (isArabic) "الإحصائيات" else "Analytics",
                    accentColor = if (isLight) VioletDark else VioletAccent,
                    bgColor = if (isLight) VioletDark.copy(alpha = 0.10f) else VioletAccent.copy(alpha = 0.15f),
                    borderColor = if (isLight) VioletDark.copy(alpha = 0.35f) else VioletAccent.copy(alpha = 0.48f),
                    onClick = onOpenAnalytics,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_analytics_shortcut")
                )
            }
        }
    }
}

@Composable
private fun HeroQuickActionButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    bgColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = accentColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FrequentItemsSection(
    presets: List<ShoppingItem>,
    isArabic: Boolean,
    onAddPreset: (ShoppingItem) -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    val presetCardBg = if (isLight) Color(0xF5FFFFFF) else Color(0xB8131D31)
    val presetCardBorder = if (isLight) BorderStroke(1.dp, LightCardBorder) else BorderStroke(1.dp, Color(0x2AFFFFFF))
    val presetPriceColor = if (isLight) MintTintText else EmeraldSavings

    Column(
        modifier = Modifier
            .disableParentPagerSwipe()
            .padding(top = 10.dp, bottom = 6.dp)
    ) {
        SouqSectionHeader(
            icon = Icons.Default.AutoAwesome,
            title = if (isArabic) "الأساسيات اليومية" else "One-Tap Essentials",
            subtitle = if (isArabic) "اضغط لإضافة فورية للسلة" else "Tap to drop straight into your list",
            accentColor = if (isLight) CyanTintText else ElectricCyan,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalFadingEdge(fadeWidth = 26.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(presets) { preset ->
                val catColor = categoryAccentColor(preset.category, isLight)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(presetCardBg)
                        .border(presetCardBorder, RoundedCornerShape(16.dp))
                        .clickable { onAddPreset(preset) }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(catColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isArabic) preset.nameAr else preset.name,
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = LocalizationUtil.formatDzd(preset.estimatedPriceDzd, isArabic),
                            color = presetPriceColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = if (isLight) 0.14f else 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = catColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChipsSection(
    items: List<ShoppingItem>,
    selectedCategory: String,
    isArabic: Boolean,
    onSelectCategory: (String) -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight

    val presentCategories = remember(items) {
        items.map { it.category }.filter { it.isNotBlank() }.distinct()
    }

    val categoriesToShow = remember(presentCategories) {
        if (presentCategories.isEmpty()) listOf("All") else listOf("All") + presentCategories
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .disableParentPagerSwipe()
            .horizontalFadingEdge(fadeWidth = 26.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categoriesToShow.forEach { category ->
            val isSelected = selectedCategory == category
            val count = if (category == "All") items.size else items.count { it.category.equals(category, ignoreCase = true) }
            val accent = if (category == "All") {
                if (isLight) CyanTintText else ElectricCyan
            } else {
                categoryAccentColor(category, isLight)
            }

            val baseLabel = if (category == "All") {
                if (isArabic) "الكل" else "All"
            } else {
                LocalizationUtil.getCategoryLabel(category, isArabic)
            }
            val label = "$baseLabel ($count)"

            val chipBg by animateColorAsState(
                targetValue = if (isSelected) accent.copy(alpha = if (isLight) 0.16f else 0.24f) else (if (isLight) Color.White else Color(0x1F1E283A)),
                label = "chip_bg"
            )
            val chipBorder = if (isSelected) {
                BorderStroke(1.3.dp, accent)
            } else {
                BorderStroke(1.dp, if (isLight) LightCardBorder else Color(0x20FFFFFF))
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(chipBg)
                    .border(chipBorder, RoundedCornerShape(14.dp))
                    .clickable { onSelectCategory(category) }
                    .padding(horizontal = 13.dp, vertical = 7.dp)
                    .testTag("category_chip_$category"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (category != "All") {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    color = if (isSelected) accent else TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    isArabic: Boolean,
    onToggleChecked: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    val catColor = categoryAccentColor(item.category, isLight)
    val checkColor by animateColorAsState(
        targetValue = if (item.isChecked) CheckedGreen else Color.Transparent,
        animationSpec = tween(250),
        label = "check_color"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (item.isChecked) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "check_scale"
    )

    // Solid 100% opaque backgrounds so swipe-to-delete red layer stays hidden until swiped
    val cardBackground = if (item.isChecked) {
        if (isLight) Color(0xFFF3F6FA) else Color(0xFF0E1628)
    } else {
        if (isLight) Color(0xFFFFFFFF) else Color(0xFF131D31)
    }
    val uncheckBorder = if (isLight) Color(0xFF94A3B8) else Color(0x60FFFFFF)
    val itemPriceColor = if (isLight) MintTintText else EmeraldSavings
    val stepperBg = if (isLight) Color(0xFFF1F5F9) else Color(0x381E293B)
    val stepperBorder = if (isLight) BorderStroke(1.dp, LightCardBorder) else BorderStroke(1.dp, Color(0x18FFFFFF))

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shopping_item_${item.id}"),
        backgroundColor = cardBackground,
        glowColor = if (item.isChecked) null else catColor.copy(alpha = 0.45f),
        onClick = onItemClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Accent Stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (item.isChecked) CheckedGreen.copy(alpha = 0.5f) else catColor)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // 48dp accessible touch target wrapping 32dp checkbox visual
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleChecked)
                    .testTag("toggle_item_${item.id}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .scale(checkScale)
                        .clip(CircleShape)
                        .background(checkColor)
                        .border(
                            BorderStroke(
                                2.dp,
                                if (item.isChecked) CheckedGreen else uncheckBorder
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isChecked) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Checked",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Item Name & Category Details
            Column(modifier = Modifier.weight(1f)) {
                val rawTitle = if (isArabic) item.nameAr else item.name
                val cleanedTitle = rawTitle
                    .replace("(مجموعة)", "")
                    .replace("- مجموعة", "")
                    .replace(" - مجموعة", "")
                    .trim()
                val isGroupItem = item.barcode?.startsWith("GRP-") == true ||
                    item.unit.equals("مجموعة", ignoreCase = true) ||
                    item.unit.equals("group", ignoreCase = true)
                val displayTitle = if (isGroupItem) {
                    cleanedTitle
                } else if (cleanedTitle.contains("(") && cleanedTitle.contains(")")) {
                    val main = cleanedTitle.substringBefore("(").trim()
                    val details = cleanedTitle.substringAfter("(").substringBefore(")").trim()
                    if (details.equals("مجموعة", ignoreCase = true) || details.equals("group", ignoreCase = true)) {
                        main
                    } else {
                        "$main - $details"
                    }
                } else {
                    cleanedTitle
                }

                Text(
                    text = displayTitle,
                    color = if (item.isChecked) TextMuted else TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(catColor.copy(alpha = if (isLight) 0.12f else 0.16f))
                            .padding(horizontal = 6.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = LocalizationUtil.getCategoryLabel(item.category, isArabic),
                            color = catColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (item.estimatedPriceDzd > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LocalizationUtil.formatDzd(item.estimatedPriceDzd * item.quantity, isArabic),
                            color = itemPriceColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Quantity Stepper Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(stepperBg)
                    .border(stepperBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 3.dp, vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDecrement),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "–",
                        color = if (isLight) CyanTintText else ElectricCyan,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${item.quantity}",
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 5.dp)
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onIncrement),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = if (isLight) CyanTintText else ElectricCyan,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyListCard(
    isArabic: Boolean,
    hasSearchFilter: Boolean = false,
    onClearFilter: () -> Unit = {},
    onAddItem: () -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    val haptic = LocalHapticFeedback.current
    var pullState by remember { mutableStateOf(PullUpGestureState()) }
    val animatedPullPx by animateFloatAsState(
        targetValue = if (pullState.isDragging) pullState.pullOffsetPx else 0f,
        animationSpec = if (pullState.isDragging) {
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "empty_list_pull_up_offset"
    )
    val hintTransition = rememberInfiniteTransition(label = "pull_up_hint")
    val arrowNudgeDp by hintTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pull_up_arrow_nudge"
    )

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .singleFingerPullUpToAdd(
                enabled = !hasSearchFilter,
                onStateChanged = { pullState = it },
                onThresholdReachedHaptic = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                },
                onTriggered = onAddItem
            )
            .graphicsLayer {
                translationY = -animatedPullPx
            }
            .testTag("empty_shopping_list_card"),
        glowColor = if (pullState.isThresholdReached) EmeraldSavings else ElectricCyan
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val emblemColor = if (pullState.isThresholdReached) EmeraldSavings else if (isLight) CyanTintText else ElectricCyan
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(emblemColor.copy(alpha = if (isLight) 0.12f else 0.16f))
                    .border(BorderStroke(1.2.dp, emblemColor.copy(alpha = 0.4f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasSearchFilter) Icons.Default.Search else Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = emblemColor,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when {
                    hasSearchFilter && isArabic -> "لا توجد نتائج مطابقة"
                    hasSearchFilter -> "No Matching Items"
                    isArabic -> "قائمة التسوق فارغة"
                    else -> "Shopping List is Empty"
                },
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when {
                    hasSearchFilter && isArabic -> "جرّب كلمة بحث أخرى أو امسح التصفية"
                    hasSearchFilter -> "Try another keyword or clear the active filter"
                    isArabic -> "اسحب للأعلى بإصبع واحد لإضافة المنتجات أو اختر من الأساسيات أعلاه"
                    else -> "Pull up with one finger to add items or tap an essential above"
                },
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            if (!hasSearchFilter) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = if (pullState.isThresholdReached) EmeraldSavings else ElectricCyan,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            translationY = if (pullState.isDragging) {
                                (-pullState.progress * 6f).dp.toPx()
                            } else {
                                arrowNudgeDp.dp.toPx()
                            }
                        }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            GlowingActionButton(
                text = when {
                    hasSearchFilter && isArabic -> "مسح البحث"
                    hasSearchFilter -> "Clear Filter"
                    pullState.isThresholdReached && isArabic -> "أفلت لإضافة المنتجات"
                    pullState.isThresholdReached -> "Release to add items"
                    isArabic -> "اسحب للأعلى لإضافة المنتجات"
                    else -> "Pull up to add items"
                },
                icon = if (hasSearchFilter) Icons.Default.Clear else Icons.Default.KeyboardArrowUp,
                onClick = {
                    if (hasSearchFilter) onClearFilter() else onAddItem()
                },
                color = if (pullState.isThresholdReached) EmeraldSavings else ElectricCyan,
                modifier = Modifier.testTag("pull_up_add_items_button")
            )
        }
    }
}

@Composable
fun AddItemSheetContent(
    isArabic: Boolean,
    productGroups: List<ProductGroup> = emptyList(),
    onDismiss: () -> Unit,
    onAutoCategorize: (name: String, nameAr: String) -> String,
    onAddItem: (name: String, nameAr: String, category: String, qty: Int, unit: String, barcode: String?, price: Double) -> Unit
) {
    var productSearchQuery by remember { mutableStateOf("") }
    var isManualEntryExpanded by remember { mutableStateOf(false) }

    // Manual item entry states
    var manualName by remember { mutableStateOf("") }
    var manualNameAr by remember { mutableStateOf("") }
    var manualCategory by remember { mutableStateOf("") }
    var manualQuantity by remember { mutableIntStateOf(1) }
    var manualUnit by remember { mutableStateOf("pcs") }
    var manualPrice by remember { mutableDoubleStateOf(0.0) }

    // Filter & rank product groups by search relevance (exact & closest matches first)
    val filteredGroups = remember(productGroups, productSearchQuery) {
        SmartSearchUtil.searchAndRankGroups(productGroups, productSearchQuery)
    }

    val isLight = MaterialTheme.colorScheme.background == BackgroundLight

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 680.dp)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Row: Title & Close Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "إضافة عنصر إلى القائمة" else "Add Item to Shopping List",
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isArabic) {
                            "تصفح المجموعات أو ابحث لإضافة سريعة للسلة"
                        } else {
                            "Browse groups or search for quick cart additions"
                        },
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Compact Elegant Glowing Search Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isLight) Color(0xFFF8FAFC) else Color(0x350F1829)
                    )
                    .border(
                        BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    ElectricCyan.copy(alpha = 0.9f),
                                    EmeraldSavings.copy(alpha = 0.7f),
                                    ElectricCyan.copy(alpha = 0.9f)
                                )
                            )
                        ),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = productSearchQuery,
                        onValueChange = { productSearchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(ElectricCyan),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp)
                            .testTag("catalog_group_search_input"),
                        decorationBox = { innerTextField ->
                            if (productSearchQuery.isEmpty()) {
                                Text(
                                    text = if (isArabic) "البحث عن منتج أو صنف أو مجموعة" else "Search product, variant or group",
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (productSearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { productSearchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Search Status / Count Pill
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (productSearchQuery.isNotBlank()) {
                        if (isArabic) "المجموعات المطابقة (${filteredGroups.size})" else "Matching Groups (${filteredGroups.size})"
                    } else {
                        if (isArabic) "المجموعات الرئيسية (${filteredGroups.size})" else "All Product Groups (${filteredGroups.size})"
                    },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (productSearchQuery.isNotBlank()) {
                    Text(
                        text = if (isArabic) "تظهر المجموعات فقط (توسيع للاطلاع على الصنف)" else "Groups only (Expand to view variants)",
                        color = if (isLight) CyanTintText else ElectricCyan,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Empty Search Results State
        if (filteredGroups.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isLight) Color(0xFFF8FAFC) else Color(0x18FFFFFF))
                        .border(BorderStroke(1.dp, if (isLight) LightCardBorder else Color(0x20FFFFFF)), RoundedCornerShape(12.dp))
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isArabic) "لم يتم العثور على مجموعة مطابقة لـ \"$productSearchQuery\"" else "No groups matching \"$productSearchQuery\"",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLight) CyanTintBg else ElectricCyan.copy(alpha = 0.18f))
                                .border(BorderStroke(1.dp, if (isLight) CyanTintBorder else ElectricCyan.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                .clickable {
                                    manualName = productSearchQuery
                                    manualNameAr = productSearchQuery
                                    manualCategory = onAutoCategorize(manualName, manualNameAr)
                                    isManualEntryExpanded = true
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (isLight) CyanTintText else ElectricCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "إضافة \"$productSearchQuery\" كمنتج يدوي" else "Add \"$productSearchQuery\" manually",
                                    color = if (isLight) CyanTintText else ElectricCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Groups Listing (Show groups only, each expandable to show its variants)
        items(
            items = filteredGroups,
            key = { it.id }
        ) { group ->
            AddItemGroupCard(
                group = group,
                isArabic = isArabic,
                onAddGroup = { grp ->
                    val finalCat = grp.category.ifBlank { onAutoCategorize(grp.nameEn, grp.nameAr) }
                    onAddItem(
                        grp.nameEn,
                        grp.nameAr,
                        finalCat,
                        1,
                        if (isArabic) "مجموعة" else "group",
                        "GRP-${grp.id}",
                        grp.avgPrice
                    )
                },
                onAddVariant = { variant ->
                    val finalCat = group.category.ifBlank { onAutoCategorize(variant.itemName, variant.itemNameAr) }
                    onAddItem(
                        variant.itemName,
                        variant.itemNameAr,
                        finalCat,
                        1,
                        "pcs",
                        variant.barcode,
                        variant.priceDzd
                    )
                }
            )
        }

        // Manual Custom Product Entry (Collapsible Accordion at the bottom)
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isLight) Color(0xFFF8FAFC) else Color(0x161D2B44))
                    .border(
                        BorderStroke(1.dp, if (isLight) LightCardBorder else Color(0x24FFFFFF)),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { isManualEntryExpanded = !isManualEntryExpanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("toggle_manual_entry_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (isLight) CyanTintText else ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "أو إدخال منتج يدوي مخصص (سعر، كمية)" else "Or enter custom product (price, quantity)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = if (isManualEntryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Expanded Manual Entry Form
        if (isManualEntryExpanded) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isLight) Color.White else Color(0xFF131D31))
                        .border(
                            BorderStroke(1.dp, if (isLight) CyanTintBorder else ElectricCyan.copy(alpha = 0.4f)),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Manual Name Input
                    OutlinedTextField(
                        value = if (isArabic && manualNameAr.isNotBlank()) manualNameAr else manualName,
                        onValueChange = {
                            manualName = it
                            if (isArabic) manualNameAr = it
                            manualCategory = onAutoCategorize(manualName, manualNameAr)
                        },
                        label = { Text(if (isArabic) "اسم المنتج المخصص" else "Custom Product Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_item_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Price & Quantity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = if (manualPrice > 0) manualPrice.toInt().toString() else "",
                            onValueChange = { manualPrice = it.toDoubleOrNull() ?: 0.0 },
                            label = { Text(if (isArabic) "السعر (د.ج)" else "Price (DZD)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_item_price"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldSavings,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        // Quantity stepper
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLight) Color(0xFFF1F5F9) else GlassSurfaceLight)
                                .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { if (manualQuantity > 1) manualQuantity-- }) {
                                Text("–", color = ElectricCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "$manualQuantity $manualUnit",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { manualQuantity++ }) {
                                Text("+", color = ElectricCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Submit Manual Item Button
                    GlowingActionButton(
                        text = if (isArabic) "إضافة إلى القائمة" else "Add to Shopping List",
                        icon = Icons.Default.Add,
                        enabled = manualName.isNotBlank(),
                        onClick = {
                            if (manualName.isNotBlank()) {
                                val finalCat = manualCategory.ifBlank { onAutoCategorize(manualName, manualNameAr) }
                                onAddItem(
                                    manualName,
                                    manualNameAr,
                                    finalCat,
                                    manualQuantity,
                                    manualUnit,
                                    null,
                                    manualPrice
                                )
                                manualName = ""
                                manualNameAr = ""
                                manualPrice = 0.0
                                isManualEntryExpanded = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_add_item_button"),
                        color = EmeraldSavings
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AddItemGroupCard(
    group: ProductGroup,
    isArabic: Boolean,
    onAddGroup: (ProductGroup) -> Unit,
    onAddVariant: (ProductVariant) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background == BackgroundLight
    var isExpanded by remember { mutableStateOf(false) }
    var groupAdded by remember { mutableStateOf(false) }
    var justAddedId by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val cardBg = if (isLight) Color.White else Color(0xFF131D31)
    val cardBorder = if (isExpanded) {
        BorderStroke(1.2.dp, if (isLight) CyanTintBorder else ElectricCyan.copy(alpha = 0.5f))
    } else {
        BorderStroke(1.dp, if (isLight) LightCardBorder else Color(0x22FFFFFF))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(cardBorder, RoundedCornerShape(14.dp))
            .testTag("sheet_group_${group.id}")
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            // Top: Category tag + Variants Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val catLabel = LocalizationUtil.getCategoryLabel(group.category, isArabic)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isLight) Color(0xFFEBF5FF) else ElectricCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = catLabel,
                        color = if (isLight) CyanTintText else ElectricCyan,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isLight) Color(0xFFF1F5F9) else Color(0x18FFFFFF))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = if (isArabic) "${group.variants.size} أصناف" else "${group.variants.size} variants",
                        color = TextSecondary,
                        fontSize = 10.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Info: Group Name & Average Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) group.nameAr else group.nameEn,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isArabic) "متوسط المجموعة: ${LocalizationUtil.formatDzd(group.avgPrice, true)}" else "Group Average: ${LocalizationUtil.formatDzd(group.avgPrice, false)}",
                        color = if (isLight) MintTintText else EmeraldSavings,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Price display (Average price + range)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = LocalizationUtil.formatDzd(group.avgPrice, isArabic),
                        color = EmeraldSavings,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (group.minPrice != group.maxPrice) {
                        Text(
                            text = "${LocalizationUtil.formatDzd(group.minPrice, isArabic)} - ${LocalizationUtil.formatDzd(group.maxPrice, isArabic)}",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Quick Add Group Button + Expand Accordion Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Add Group Button (Adds group itself with average price)
                val addBg = if (groupAdded) EmeraldSavings else if (isLight) Color(0xFF0284C7) else ElectricCyan
                val addContentColor = if (groupAdded) Color.White else if (isLight) Color.White else Color(0xFF051323)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(addBg)
                        .clickable {
                            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                            onAddGroup(group)
                            groupAdded = true
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("quick_add_sheet_group_${group.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (groupAdded) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = addContentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (groupAdded) {
                                if (isArabic) "أُضيفت المجموعة!" else "Group Added!"
                            } else {
                                if (isArabic) "إضافة المجموعة (${group.avgPrice.toInt()} د.ج)" else "Add Group (${group.avgPrice.toInt()} DZD)"
                            },
                            color = addContentColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Expand / Collapse Accordion Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLight) Color(0xFFF1F5F9) else Color(0x18FFFFFF))
                        .border(
                            BorderStroke(1.dp, if (isLight) LightCardBorder else Color(0x1FFFFFFF)),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("expand_sheet_group_${group.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isExpanded) {
                                if (isArabic) "طي المجموعة" else "Collapse"
                            } else {
                                if (isArabic) "توسيع للمجموعة (${group.variants.size})" else "Expand group (${group.variants.size})"
                            },
                            color = if (isLight) CyanTintText else ElectricCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (isLight) CyanTintText else ElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Expanded Variants List
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = if (isLight) Color(0xFFE2E8F0) else Color(0x18FFFFFF))
                    Text(
                        text = if (isArabic) "اختر صنفاً أو نكهة محددة:" else "Choose a specific variant:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    var visibleLimit by remember(group.id, isExpanded) { mutableIntStateOf(20) }
                    val visibleVariants = remember(group.variants, visibleLimit) {
                        group.variants.take(visibleLimit)
                    }

                    visibleVariants.forEach { variant ->
                        val isVarAdded = (justAddedId == variant.barcode)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLight) Color(0xFFF8FAFC) else Color(0x14FFFFFF))
                                .border(
                                    BorderStroke(1.dp, if (isLight) Color(0xFFE2E8F0) else Color(0x10FFFFFF)),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) variant.itemNameAr else variant.itemName,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = LocalizationUtil.formatDzd(variant.priceDzd, isArabic),
                                    color = EmeraldSavings,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isVarAdded) EmeraldSavings else if (isLight) Color(0xFFE0F2FE) else ElectricCyan.copy(alpha = 0.2f))
                                    .border(
                                        BorderStroke(1.dp, if (isVarAdded) EmeraldSavings else if (isLight) Color(0xFFBAE6FD) else ElectricCyan.copy(alpha = 0.4f)),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                        onAddVariant(variant)
                                        justAddedId = variant.barcode
                                    }
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isVarAdded) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (isVarAdded) Color.White else if (isLight) Color(0xFF0369A1) else ElectricCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isVarAdded) (if (isArabic) "أُضيف" else "Added") else (if (isArabic) "أضف" else "Add"),
                                        color = if (isVarAdded) Color.White else if (isLight) Color(0xFF0369A1) else ElectricCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (group.variants.size > visibleLimit) {
                        val remaining = group.variants.size - visibleLimit
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLight) CyanTintBg else ElectricCyan.copy(alpha = 0.14f))
                                .border(
                                    BorderStroke(1.dp, if (isLight) CyanTintBorder else ElectricCyan.copy(alpha = 0.35f)),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { visibleLimit += 25 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "عرض المزيد ($remaining متبقي)" else "Show more ($remaining remaining)",
                                color = if (isLight) CyanTintText else ElectricCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
