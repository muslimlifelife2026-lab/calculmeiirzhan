package com.aerocalc.smartcalculator.ui.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.R
import com.calculator.core.ui.theme.*
import com.calculator.core.ui.theme.TextSecondary
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
        NavItem(Icons.Rounded.Calculate,    stringResource(R.string.tab_calculations)),
        NavItem(Icons.Rounded.Architecture, stringResource(R.string.tab_geometry)),
        NavItem(Icons.Rounded.Science,      stringResource(R.string.tab_physics)),
        NavItem(Icons.Rounded.History,      stringResource(R.string.tab_history)),
        NavItem(Icons.AutoMirrored.Rounded.ShowChart, stringResource(R.string.tab_graphing)),
        NavItem(Icons.Rounded.Biotech,      stringResource(R.string.tab_chemistry))
    )

    AeroCalcTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {
            // ─── Content ────────────────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 90.dp) // leave room for floating bar
            ) { page ->
                when (page) {
                    0 -> CalculatorScreen()
                    1 -> GeometryScreen()
                    2 -> PhysicsScreen()
                    3 -> HistoryScreen()
                    4 -> GraphingScreen()
                    5 -> ChemistryScreen()
                }
            }

            // ─── Floating Navigation Bar ─────────────────────────────────────
            FloatingNavBar(
                items = navItems,
                currentIndex = currentPage,
                onItemSelected = { index ->
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun FloatingNavBar(
    items: List<NavItem>,
    currentIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(32.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                FloatingNavItem(
                    item = item,
                    isSelected = currentIndex == index,
                    onClick = { onItemSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_scale"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            val pillAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(180),
                label = "pill_alpha"
            )
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(AccentPrimary.copy(alpha = pillAlpha))
            )
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (isSelected) Background else TextSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }

        val labelAlpha by animateFloatAsState(
            targetValue = if (isSelected) 1f else 0f,
            animationSpec = tween(160),
            label = "label_alpha"
        )
        if (labelAlpha > 0f) {
            Text(
                text = item.label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = AccentPrimary.copy(alpha = labelAlpha),
                maxLines = 1
            )
        }
    }
}
