package com.calculator.feature.chemistry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.*
import com.calculator.domain.model.Element
import com.calculator.domain.model.PeriodicTable
import com.calculator.domain.model.SolubilityData
import com.calculator.domain.model.SolubilityStatus
import com.calculator.engine.solver.ChemistrySolver
import com.calculator.feature.chemistry.components.AtomModelCanvas

data class ChemicalPreset(val title: String, val formula: String)
data class ReactionPreset(val title: String, val equation: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChemistryScreen(
    modifier: Modifier = Modifier
) {
    var formulaInput by remember { mutableStateOf("H2SO4") }
    var equationInput by remember { mutableStateOf("Fe + O2 -> Fe2O3") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Mass, 1: Balancer, 2: Periodic Table, 3: Atom Model, 4: Solubility
    var selectedElement by remember { mutableStateOf<Element?>(null) }
    var selectedSolubilityPair by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedAtomElement by remember { mutableStateOf(PeriodicTable.getElement("C") ?: Element("C", "Carbon", 6, 12.011)) }

    val molarPresets = remember {
        listOf(
            ChemicalPreset("💧 Вода", "H2O"),
            ChemicalPreset("🧂 Поваренная соль", "NaCl"),
            ChemicalPreset("🔋 Серная к-та", "H2SO4"),
            ChemicalPreset("🍬 Глюкоза", "C6H12O6"),
            ChemicalPreset("🧼 Сода пищевая", "NaHCO3"),
            ChemicalPreset("🪨 Мел (Кальцит)", "CaCO3"),
            ChemicalPreset("🍷 Спирт (Этанол)", "C2H5OH"),
            ChemicalPreset("💨 Углекислый газ", "CO2"),
            ChemicalPreset("🩹 Перекись водорода", "H2O2"),
            ChemicalPreset("👃 Нашатырный спирт", "NH3"),
            ChemicalPreset("🧪 Соляная к-та", "HCl"),
            ChemicalPreset("⚗️ Азотная к-та", "HNO3"),
            ChemicalPreset("🥗 Уксусная к-та", "CH3COOH"),
            ChemicalPreset("🧼 Едкий натр (NaOH)", "NaOH"),
            ChemicalPreset("🔥 Метан (Газ)", "CH4"),
            ChemicalPreset("🍭 Сахар (Сахароза)", "C12H22O11"),
            ChemicalPreset("💊 Аспирин", "C9H8O4"),
            ChemicalPreset("🌾 Калийная селитра", "KNO3"),
            ChemicalPreset("🔷 Медный купорос", "CuSO4"),
            ChemicalPreset("🧱 Гашеная известь", "Ca(OH)2"),
            ChemicalPreset("⚡ Озон", "O3"),
            ChemicalPreset("🔩 Ржавчина", "Fe2O3")
        )
    }

    val reactionPresets = remember {
        listOf(
            ReactionPreset("🔥 Горение метана", "CH4 + O2 -> CO2 + H2O"),
            ReactionPreset("🧪 Ржавление железа", "Fe + O2 -> Fe2O3"),
            ReactionPreset("⚡ Электролиз воды", "H2O -> H2 + O2"),
            ReactionPreset("🧯 Сода + Кислота", "NaHCO3 + HCl -> NaCl + CO2 + H2O"),
            ReactionPreset("🌱 Фотосинтез", "CO2 + H2O -> C6H12O6 + O2"),
            ReactionPreset("💧 Нейтрализация щёлочи", "NaOH + HCl -> NaCl + H2O"),
            ReactionPreset("💨 Синтез аммиака", "N2 + H2 -> NH3"),
            ReactionPreset("💥 Горение водорода", "H2 + O2 -> H2O"),
            ReactionPreset("🪨 Разложение известняка", "CaCO3 -> CaO + CO2")
        )
    }

    val massResult = remember(formulaInput) {
        if (formulaInput.isNotBlank()) {
            ChemistrySolver.solveMolarMass(formulaInput)
        } else {
            null
        }
    }

    val balanceResult = remember(equationInput) {
        if (equationInput.isNotBlank()) {
            ChemistrySolver.balanceReaction(equationInput)
        } else {
            null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mode Selector: 5 Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color.White
                    )
                }
            },
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            val tabs = listOf("🧪 Молярная масса", "⚖️ Балансировщик", "⚛️ Таблица Менделеева", "🔬 Модель атома", "💧 Растворимость")
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // ─── MOLAR MASS MODE ──────────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1-Tap Substance Presets Row
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "✨ Популярные вещества (1 тап):",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(molarPresets) { preset ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceCard)
                                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                                        .clickable { formulaInput = preset.formula }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${preset.title} (${preset.formula})",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Химическая формула:",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (formulaInput.isNotEmpty()) {
                                    Text(
                                        text = "Очистить",
                                        color = ErrorRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { formulaInput = "" }
                                    )
                                }
                            }
                            
                            TextField(
                                value = formulaInput,
                                onValueChange = { formulaInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceElevated,
                                    unfocusedContainerColor = SurfaceElevated,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                placeholder = {
                                    Text(
                                        text = "Например: H2SO4 или C6H12O6",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            )

                            // Quick Chemistry Keyboard Chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                val keys = listOf("H", "O", "C", "N", "Na", "Cl", "Fe", "Ca", "S", "K", "Al", "Cu", "(", ")", "2", "3", "4", "5", "6", "⌫")
                                items(keys) { key ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (key == "⌫") ErrorRed.copy(alpha = 0.2f) else SurfaceElevated)
                                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (key == "⌫") {
                                                    if (formulaInput.isNotEmpty()) formulaInput = formulaInput.dropLast(1)
                                                } else {
                                                    formulaInput += key
                                                }
                                            }
                                            .padding(horizontal = 9.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            color = if (key == "⌫") ErrorRed else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (massResult != null) {
                        if (massResult.success) {
                            GlassCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "МОЛЯРНАЯ МАССА:",
                                        color = com.calculator.core.ui.theme.AccentAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Text(
                                        text = String.format("%.3f г/моль", massResult.molarMass),
                                        color = Color.White,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    HorizontalDivider(color = SurfaceBorder)

                                    Text(
                                        text = "Состав и массовые доли:",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth().weight(1f)
                                    ) {
                                        items(massResult.elementCounts.toList()) { (element, count) ->
                                            val elementTotalMass = element.atomicMass * count
                                            val fractionPercent = (elementTotalMass / massResult.molarMass) * 100.0
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(SurfaceElevated)
                                                            .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = element.symbol,
                                                            color = Color(0xFF38BDF8),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        )
                                                    }
                                                    Text(
                                                        text = "${element.name} (×$count)",
                                                        color = TextPrimary,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                Text(
                                                    text = "${String.format("%.1f", fractionPercent)}%",
                                                    color = Color(0xFF10B981),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = massResult.errorMessage ?: "Проверьте правильность химической формулы",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // ─── CHEMICAL EQUATION BALANCER MODE ─────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1-Tap Reaction Presets Row
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "🔥 Примеры реакций (1 тап):",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(reactionPresets) { preset ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceCard)
                                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                                        .clickable { equationInput = preset.equation }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = preset.title,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Уравнение реакции (реагенты → продукты):",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (equationInput.isNotEmpty()) {
                                    Text(
                                        text = "Очистить",
                                        color = ErrorRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { equationInput = "" }
                                    )
                                }
                            }

                            TextField(
                                value = equationInput,
                                onValueChange = { equationInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceElevated,
                                    unfocusedContainerColor = SurfaceElevated,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                placeholder = {
                                    Text(
                                        text = "H2 + O2 -> H2O",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            )

                            // Quick Operators & Elements for Balancer
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                val keys = listOf("+", " -> ", "H", "O", "C", "N", "Fe", "Na", "Cl", "2", "3", "4", "(", ")", "⌫")
                                items(keys) { key ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (key == " -> " || key == "+") Color.White else SurfaceElevated)
                                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (key == "⌫") {
                                                    if (equationInput.isNotEmpty()) equationInput = equationInput.dropLast(1)
                                                } else {
                                                    equationInput += key
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key.trim(),
                                            color = if (key == " -> " || key == "+") Background else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (balanceResult != null) {
                        val isSuccess = balanceResult.contains("=")
                        if (isSuccess) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "УРАВНЕННАЯ РЕАКЦИЯ:",
                                        color = com.calculator.core.ui.theme.AccentAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = balanceResult,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    HorizontalDivider(color = SurfaceBorder)

                                    Text(
                                        text = "Коэффициенты реакции расставлены по закону сохранения массы.",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = balanceResult,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                // ─── PERIODIC TABLE MODE ──────────────────────────────────────────────
                val elements = remember { PeriodicTable.allElements }
                var searchQuery by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Поиск элемента: Fe, Железо, 26...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedContainerColor = SurfaceElevated,
                            unfocusedContainerColor = SurfaceElevated,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    val filtered = elements.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.symbol.contains(searchQuery, ignoreCase = true) ||
                        it.atomicNumber.toString().contains(searchQuery)
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 75.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(filtered) { elem ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceCard)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                                    .clickable { selectedElement = elem }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = elem.atomicNumber.toString(),
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Text(
                                        text = elem.symbol,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = elem.name,
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // ─── ATOM MODEL VISUALIZER MODE ──────────────────────────────────────
                val sampleElements = remember { PeriodicTable.allElements }

                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quick Elements Selector Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(sampleElements) { elem ->
                            val isSelected = selectedAtomElement.atomicNumber == elem.atomicNumber
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) Color.White else SurfaceElevated)
                                    .border(1.dp, if (isSelected) Color.White else SurfaceBorder, RoundedCornerShape(14.dp))
                                    .clickable { selectedAtomElement = elem }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "${elem.symbol} (${elem.name})",
                                    color = if (isSelected) Background else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    AtomModelCanvas(
                        element = selectedAtomElement,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            }

            4 -> {
                // ─── SOLUBILITY MATRIX MODE ──────────────────────────────────────────
                val cations = SolubilityData.cations
                val anions = SolubilityData.anions
                val horizontalScrollState = rememberScrollState()

                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Таблица Растворимости Солей",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Нажмите на ячейку для просмотра подробностей",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        Column {
                            // Header Row: Anions
                            Row {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0x22FFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Кат\\Ан", color = TextSecondary, fontSize = 9.sp)
                                }
                                anions.forEach { anion ->
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color(0x1AFFFFFF))
                                            .border(0.5.dp, Color(0x22FFFFFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(anion, color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Data Rows: Cation x Anions
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(cations) { cation ->
                                    Row {
                                        // Left Cation Header
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(Color(0x1AFFFFFF))
                                                .border(0.5.dp, Color(0x22FFFFFF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(cation, color = com.calculator.core.ui.theme.AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Status cells
                                        anions.forEach { anion ->
                                            val status = SolubilityData.getStatus(cation, anion)
                                            val badgeBg = Color(status.hexColor).copy(alpha = 0.85f)

                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .background(badgeBg)
                                                    .border(0.5.dp, Color(0x33FFFFFF))
                                                    .clickable { selectedSolubilityPair = cation to anion },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = status.code,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── ELEMENT DETAIL DIALOG ────────────────────────────────────────────────
    selectedElement?.let { elem ->
        AlertDialog(
            onDismissRequest = { selectedElement = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(elem.symbol, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(elem.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Атомный номер Z = ${elem.atomicNumber}", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        AtomModelCanvas(element = elem)
                    }
                    HorizontalDivider(color = SurfaceBorder)
                    Text("• Атомная масса: ${elem.atomicMass} г/моль", color = Color.White, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        formulaInput += elem.symbol
                        selectedElement = null
                        selectedTab = 0 // Jump to Molar Mass
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text("Вставить в формулу 🧪", color = Background, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedElement = null }) {
                    Text("Закрыть", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard
        )
    }

    // ─── SOLUBILITY DETAIL DIALOG ─────────────────────────────────────────────
    selectedSolubilityPair?.let { (cat, an) ->
        val status = SolubilityData.getStatus(cat, an)
        AlertDialog(
            onDismissRequest = { selectedSolubilityPair = null },
            title = {
                Text("Соединение: $cat + $an", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(status.hexColor).copy(alpha = 0.3f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${status.code} — ${status.title}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = when (status) {
                            SolubilityStatus.SOLUBLE -> "Отлично растворяется в воде с образованием прозрачного раствора гидратированных ионов."
                            SolubilityStatus.INSOLUBLE -> "Нерастворимое вещество. Выпадает в осадок при взаимодействии соответствующих ионов."
                            SolubilityStatus.SLIGHTLY_SOLUBLE -> "Малорастворимое вещество. Лишь небольшая часть соли переходит в раствор."
                            SolubilityStatus.DECOMPOSES -> "Вещество разлагается водой (необратимый гидролиз) или не существует в водном растворе."
                        },
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSolubilityPair = null }) {
                    Text("Понятно", color = Color.White)
                }
            },
            containerColor = SurfaceCard
        )
    }
}
