package com.calculator.feature.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calculator.core.ui.components.MagicCard
import com.calculator.core.ui.components.AnimatedNumberTicker
import com.calculator.core.ui.components.shimmerBorder
import com.calculator.core.ui.components.MathKeyboard
import com.calculator.core.ui.theme.NeonCyan
import com.calculator.core.ui.theme.ElectricViolet
import com.calculator.core.ui.theme.CoralPink
import com.calculator.core.ui.theme.TextPrimary
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.core.ui.theme.ObsidianBackground
import com.calculator.core.ui.theme.KeyOrange
import com.calculator.core.ui.theme.KeyLightGray
import com.calculator.core.ui.theme.KeyDarkGray
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.DeleteOutline
import com.calculator.feature.calculator.scanner.ScannerScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import com.calculator.core.ui.utils.PdfGenerator

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = viewModel()
) {
    val expr by viewModel.displayExpression.collectAsState()
    val result by viewModel.calculatedResult.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val steps by viewModel.calculationSteps.collectAsState()

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val view = androidx.compose.ui.platform.LocalView.current

    androidx.compose.runtime.LaunchedEffect(result) {
        if (result.isNotEmpty()) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    androidx.compose.runtime.LaunchedEffect(error) {
        if (error.isNotEmpty()) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        }
    }

    var showScanner by remember { mutableStateOf(false) }

    val onShowScanner = remember { { showScanner = true } }
    val onScannerClose = remember { { showScanner = false } }
    val onScannerResult = remember(viewModel) {
        { text: String ->
            viewModel.clearDisplay()
            text.forEach { char -> viewModel.onKeyPressed(char.toString()) }
            showScanner = false
        }
    }

    if (showScanner) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onScannerClose,
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            ScannerScreen(
                onResult = onScannerResult,
                onClose = onScannerClose
            )
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val onKeyPressed = remember(viewModel) { { key: String -> viewModel.onKeyPressed(key) } }

    val recentHistory by viewModel.recentHistory.collectAsState()
    val isDegrees by viewModel.isDegrees.collectAsState()

    val onPasteHistory = remember(viewModel) { { value: String -> viewModel.pasteFromHistory(value) } }
    val onDeleteHistoryItem = remember(viewModel) { { id: Int -> viewModel.deleteHistoryItem(id) } }
    val onClearAllHistory = remember(viewModel) { { viewModel.clearAllHistory() } }
    val onToggleAngleMode = remember(viewModel) { { viewModel.toggleAngleMode() } }

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(com.calculator.core.ui.theme.Background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DisplayArea(
                expr = expr,
                result = result,
                error = error,
                steps = steps,
                recentHistory = recentHistory,
                isDegrees = isDegrees,
                onToggleAngleMode = onToggleAngleMode,
                onShowScanner = onShowScanner,
                onPasteHistory = onPasteHistory,
                onDeleteHistoryItem = onDeleteHistoryItem,
                onClearAllHistory = onClearAllHistory,
                onBackspace = { onKeyPressed("⌫") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            
            MathKeyboard(
                onKeyPressed = onKeyPressed,
                showVariables = false,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(com.calculator.core.ui.theme.Background)
                .padding(16.dp)
        ) {
            DisplayArea(
                expr = expr,
                result = result,
                error = error,
                steps = steps,
                recentHistory = recentHistory,
                isDegrees = isDegrees,
                onToggleAngleMode = onToggleAngleMode,
                onShowScanner = onShowScanner,
                onPasteHistory = onPasteHistory,
                onDeleteHistoryItem = onDeleteHistoryItem,
                onClearAllHistory = onClearAllHistory,
                onBackspace = { onKeyPressed("⌫") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            MathKeyboard(
                onKeyPressed = onKeyPressed,
                showVariables = false,
                modifier = Modifier.weight(2.5f)
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DisplayArea(
    expr: String,
    result: String,
    error: String,
    steps: List<com.calculator.engine.parser.MathParser.MathStep>,
    recentHistory: List<com.calculator.core.data.database.CalculationEntity>,
    isDegrees: Boolean,
    onToggleAngleMode: () -> Unit,
    onShowScanner: () -> Unit,
    onPasteHistory: (String) -> Unit,
    onDeleteHistoryItem: (Int) -> Unit,
    onClearAllHistory: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var showStepsSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val success = PdfGenerator.generateAndSavePdf(
                    context = context,
                    uri = it,
                    title = "Math Solution",
                    expression = expr,
                    result = result,
                    steps = steps.map { step -> "${step.description}: ${step.expression}" }
                )
                if (success) {
                    android.widget.Toast.makeText(context, "PDF saved successfully", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "Error saving PDF", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        // Action Buttons Row (Scanner, DEG/RAD, Quick History & Export) - Top Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scanner button
                androidx.compose.material3.Button(
                    onClick = onShowScanner,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = KeyDarkGray
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Scan Math",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Сканер",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // DEG / RAD Precision Segmented Capsule
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(KeyDarkGray)
                        .border(1.dp, com.calculator.core.ui.theme.SurfaceBorder, RoundedCornerShape(20.dp))
                        .clickable { onToggleAngleMode() }
                        .padding(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDegrees) Color.White else Color.Transparent)
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "DEG",
                                color = if (isDegrees) com.calculator.core.ui.theme.Background else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (!isDegrees) Color.White else Color.Transparent)
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "RAD",
                                color = if (!isDegrees) com.calculator.core.ui.theme.Background else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Quick History Peek Button
                IconButton(
                    onClick = { showHistorySheet = true },
                    modifier = Modifier
                        .background(KeyDarkGray, shape = RoundedCornerShape(20.dp))
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = "Quick History",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = result.isNotEmpty()) {
                IconButton(
                    onClick = {
                        exportLauncher.launch("solution.pdf")
                    },
                    modifier = Modifier
                        .background(KeyDarkGray, shape = RoundedCornerShape(12.dp))
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = "Export to PDF",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Display Area (Dynamic Auto-Sizing Typography & Tabular Figures)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            val dynamicFontSize = when {
                expr.length <= 6 -> 54.sp
                expr.length <= 11 -> 40.sp
                expr.length <= 16 -> 30.sp
                expr.length <= 22 -> 24.sp
                else -> 19.sp
            }
            val dynamicLineHeight = when {
                expr.length <= 6 -> 58.sp
                expr.length <= 11 -> 44.sp
                expr.length <= 16 -> 34.sp
                expr.length <= 22 -> 28.sp
                else -> 23.sp
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = expr.ifEmpty { "0" },
                    color = Color.White,
                    fontSize = dynamicFontSize,
                    lineHeight = dynamicLineHeight,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    maxLines = 3,
                    fontFamily = com.calculator.core.ui.theme.MonospaceFontFamily,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFeatureSettings = "tnum",
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                if (expr.isNotEmpty()) {
                    IconButton(
                        onClick = onBackspace,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("⌫", color = com.calculator.core.ui.theme.AccentSecondary, fontSize = 22.sp)
                    }
                }
            }

            // Evaluated result or error
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .clickable(enabled = result.isNotEmpty()) {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Result", result)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Скопировано: $result", android.widget.Toast.LENGTH_SHORT).show()
                        showStepsSheet = true
                    }
                    .padding(vertical = 8.dp)
            ) {
                if (error.isNotEmpty()) {
                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val locale = configuration.locales[0]
                    val isRussian = locale.language == "ru"
                    val errorPrefix = androidx.compose.ui.res.stringResource(com.calculator.core.ui.R.string.label_error)
                    
                    val localizedError = when {
                        error.contains("Parentheses mismatch", ignoreCase = true) -> if (isRussian) "несовпадение скобок" else "parentheses mismatch"
                        error.contains("Division by zero", ignoreCase = true) -> if (isRussian) "деление на ноль" else "division by zero"
                        error.contains("Unknown operator", ignoreCase = true) -> if (isRussian) error.replace("Unknown operator", "неизвестный оператор") else error
                        error.contains("Unexpected character", ignoreCase = true) -> if (isRussian) error.replace("Unexpected character", "неожиданный символ").replace("at position", "на позиции") else error
                        error.contains("Variable not initialized", ignoreCase = true) -> if (isRussian) error.replace("Variable not initialized", "переменная не задана") else error
                        error.contains("Факториал определен", ignoreCase = true) -> if (isRussian) "факториал определен только для целых неотрицательных чисел" else "factorial is only defined for non-negative integers"
                        error.contains("Значение слишком велико", ignoreCase = true) -> if (isRussian) "значение слишком велико для факториала" else "value is too large for factorial"
                        else -> error
                    }

                    Text(
                        text = "$errorPrefix: $localizedError",
                        color = CoralPink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End
                    )
                }
                
                AnimatedVisibility(
                    visible = result.isNotEmpty(),
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        AnimatedNumberTicker(
                            text = "= $result",
                            color = com.calculator.core.ui.theme.AccentSecondary,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = com.calculator.core.ui.theme.MonospaceFontFamily,
                                fontFeatureSettings = "tnum"
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Нажмите, чтобы скопировать и посмотреть шаги ℹ️",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet with steps
    if (showStepsSheet && steps.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showStepsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = com.calculator.core.ui.theme.SurfaceCard,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Пошаговое решение",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.calculator.core.ui.theme.AccentAmber,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Пример: $expr",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = com.calculator.core.ui.theme.MonospaceFontFamily,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x0AFFFFFF), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    itemsIndexed(steps) { index, step ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Vertical color line
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(com.calculator.core.ui.theme.AccentViolet)
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Шаг ${index + 1}: ${step.description}",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = step.expression,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = com.calculator.core.ui.theme.MonospaceFontFamily
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showStepsSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = com.calculator.core.ui.theme.AccentViolet)
                ) {
                    Text("Понятно", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Quick History Modal Bottom Sheet
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = com.calculator.core.ui.theme.SurfaceCard,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "История расчётов",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (recentHistory.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onClearAllHistory()
                                showHistorySheet = false
                                android.widget.Toast.makeText(context, "История очищена", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Clear All History",
                                tint = com.calculator.core.ui.theme.ErrorRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (recentHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "История пуста — выполните вычисления",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(recentHistory, key = { it.id }) { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(com.calculator.core.ui.theme.SurfaceElevated)
                                    .border(1.dp, com.calculator.core.ui.theme.SurfaceBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        onPasteHistory(item.result)
                                        showHistorySheet = false
                                        android.widget.Toast.makeText(context, "Вставлено: ${item.result}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.expression.ifEmpty { item.formulaName },
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            fontFamily = com.calculator.core.ui.theme.MonospaceFontFamily
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "= ${item.result}",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = com.calculator.core.ui.theme.MonospaceFontFamily,
                                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteHistoryItem(item.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete Item",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
