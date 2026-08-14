package com.calculator.feature.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calculator.core.ui.R
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Category color accent map
private fun accentForCategory(name: String): Color = when {
    name.contains("Geom", ignoreCase = true) || name.contains("Геом", ignoreCase = true) -> AccentViolet
    name.contains("Phys", ignoreCase = true) || name.contains("Физ", ignoreCase = true)  -> AccentCyan
    name.contains("Chem", ignoreCase = true) || name.contains("Хим", ignoreCase = true)  -> AccentCyan
    else -> AccentAmber
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var itemToExport by remember { mutableStateOf<com.calculator.core.data.database.CalculationEntity?>(null) }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val item = itemToExport
        if (uri != null && item != null) {
            coroutineScope.launch {
                val stepsList = if (item.serializedSteps.isNotEmpty()) {
                    item.serializedSteps.split("|")
                } else emptyList()
                val success = com.calculator.core.ui.utils.PdfGenerator.generateAndSavePdf(
                    context = context,
                    uri = uri,
                    title = "History: ${item.formulaName}",
                    expression = item.solvedVariable,
                    result = "${String.format(Locale.US, "%.4f", item.resultValue)} ${item.resultUnit}",
                    steps = stepsList
                )
                android.widget.Toast.makeText(
                    context,
                    if (success) "PDF сохранён" else "Ошибка PDF",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                itemToExport = null
            }
        } else {
            itemToExport = null
        }
    }

    val historyItems by viewModel.historyItems.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        // ─── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(AccentVioletLight.copy(alpha = 0.4f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "История",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${historyItems.size} вычислений",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            if (historyItems.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.history_btn_clear),
                        color = ErrorRed,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Empty State ────────────────────────────────────────────────────────
        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "🕰️", fontSize = 56.sp)
                    Text(
                        text = "История пуста",
                        color = TextSecondary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Сделайте первый расчёт,\nи он появится здесь",
                        color = TextMuted,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                itemsIndexed(historyItems) { index, item ->
                    val accentColor = accentForCategory(item.formulaName)

                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(item.id) { visible = true }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(300, delayMillis = (index * 40).coerceAtMost(300))) +
                                slideInHorizontally(tween(300, delayMillis = (index * 40).coerceAtMost(300))) { -40 }
                    ) {
                        HistoryCard(
                            item = item,
                            accentColor = accentColor,
                            dateFormat = dateFormat,
                            onExport = {
                                itemToExport = item
                                exportLauncher.launch("solution_${item.id}.pdf")
                            },
                            onDelete = { viewModel.deleteItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    item: com.calculator.core.data.database.CalculationEntity,
    accentColor: Color,
    dateFormat: java.text.SimpleDateFormat,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // ── Colored left accent bar ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(accentColor, accentColor.copy(alpha = 0.3f))
                        )
                    )
            )

            Spacer(Modifier.width(12.dp))

            // ── Content ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.formulaName,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${item.solvedVariable} = ${String.format(Locale.US, "%.4f", item.resultValue)} ${item.resultUnit}",
                        color = accentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = dateFormat.format(Date(item.timestamp)),
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = onExport, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = AccentVioletLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete",
                            tint = ErrorRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
