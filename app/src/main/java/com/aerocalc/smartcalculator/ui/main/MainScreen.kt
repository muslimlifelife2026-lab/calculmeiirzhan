package com.aerocalc.smartcalculator.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.R
import com.calculator.core.ui.components.bounceClick
import com.calculator.core.ui.theme.*
import com.calculator.feature.calculator.CalculatorScreen
import com.calculator.feature.chemistry.ChemistryScreen
import com.calculator.feature.geometry.GeometryScreen
import com.calculator.feature.graphing.GraphingScreen
import com.calculator.feature.history.HistoryScreen
import com.calculator.feature.physics.PhysicsScreen
import kotlinx.coroutines.launch

data class NavItem(val icon: ImageVector, val label: String)

@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 6 })
    val coroutineScope = rememberCoroutineScope()
    val currentPage by remember { derivedStateOf { pagerState.targetPage } }

    val navItems = listOf(
        NavItem(Icons.Rounded.Calculate,              stringResource(R.string.tab_calculations)),
        NavItem(Icons.Rounded.Science,                stringResource(R.string.tab_physics)),
        NavItem(Icons.Rounded.Biotech,                stringResource(R.string.tab_chemistry)),
        NavItem(Icons.Rounded.Architecture,           stringResource(R.string.tab_geometry)),
        NavItem(Icons.AutoMirrored.Rounded.ShowChart, stringResource(R.string.tab_graphing)),
        NavItem(Icons.Rounded.History,                stringResource(R.string.tab_history))
    )

    AeroCalcTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ─── Sleek Top Subject Tabs (Compact & Responsive) ─────────────────
            TopSubjectTabBar(
                items = navItems,
                currentIndex = currentPage,
                onItemSelected = { index ->
                    coroutineScope.launch { pagerState.scrollToPage(index) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            // ─── Screen-Adaptive Content (100% Zero-Scroll Viewport) ────────────
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false, // Disable accidental swipe gesture
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> CalculatorScreen()
                    1 -> PhysicsScreen()
                    2 -> ChemistryScreen()
                    3 -> GeometryScreen()
                    4 -> GraphingScreen()
                    5 -> HistoryScreen()
                }
            }
        }
    }
}

@Composable
private fun TopSubjectTabBar(
    items: List<NavItem>,
    currentIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(items) { index, item ->
            val isSelected = currentIndex == index

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) SurfaceElevated else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "tab_bg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else TextSecondary,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "tab_fg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) SurfaceBorder else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "tab_border"
            )
            val dotScale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "tab_dot_scale"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(containerColor)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .bounceClick { onItemSelected(index) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = item.label,
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .graphicsLayer {
                                scaleX = dotScale
                                scaleY = dotScale
                                alpha = dotScale.coerceIn(0f, 1f)
                            }
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }
    }
}
