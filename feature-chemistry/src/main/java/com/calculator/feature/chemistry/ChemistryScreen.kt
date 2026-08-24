package com.calculator.feature.chemistry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import com.calculator.core.ui.theme.*
import com.calculator.domain.model.Element
import com.calculator.domain.model.SolubilityData
import com.calculator.domain.model.SolubilityStatus
import com.calculator.feature.chemistry.components.AtomModelCanvas

data class ChemicalPreset(val title: String, val formula: String)
data class ReactionPreset(val title: String, val equation: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChemistryScreen(
    modifier: Modifier = Modifier
) {
    var formulaInput by rememberSaveable { mutableStateOf("H2SO4") }
    var equationInput by rememberSaveable { mutableStateOf("Fe + O2 -> Fe2O3") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Mass, 1: Balancer, 2: Periodic Table, 3: Atom Model, 4: Solubility
    var selectedElement by remember { mutableStateOf<Element?>(null) }
    var selectedSolubilityPair by remember { mutableStateOf<Pair<String, String>?>(null) }

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
            0 -> MolarMassSection(
                formulaInput = formulaInput,
                onFormulaChange = { formulaInput = it },
                molarPresets = molarPresets,
                modifier = Modifier.weight(1f)
            )
            1 -> BalancerSection(
                equationInput = equationInput,
                onEquationChange = { equationInput = it },
                reactionPresets = reactionPresets,
                modifier = Modifier.weight(1f)
            )
            2 -> PeriodicTableSection(
                onElementClick = { selectedElement = it },
                modifier = Modifier.weight(1f)
            )
            3 -> AtomModelSection(
                modifier = Modifier.weight(1f)
            )
            4 -> SolubilityTableSection(
                onPairClick = { selectedSolubilityPair = it },
                modifier = Modifier.weight(1f)
            )
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
