package com.calculator.feature.chemistry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.calculator.core.ui.components.GlassCard
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
    var selectedTab by rememberSaveable { mutableIntStateOf(-1) }
    val sectionState = rememberSaveableStateHolder()
    var selectedElement by remember { mutableStateOf<Element?>(null) }
    var selectedSolubilityPair by remember { mutableStateOf<Pair<String, String>?>(null) }
    BackHandler(selectedTab >= 0 && selectedElement == null && selectedSolubilityPair == null) { selectedTab = -1 }

    val molarPresets = remember {
        listOf(
            ChemicalPreset("Вода", "H2O"),
            ChemicalPreset("Поваренная соль", "NaCl"),
            ChemicalPreset("Серная к-та", "H2SO4"),
            ChemicalPreset("Глюкоза", "C6H12O6"),
            ChemicalPreset("Сода пищевая", "NaHCO3"),
            ChemicalPreset("Мел (Кальцит)", "CaCO3"),
            ChemicalPreset("Спирт (Этанол)", "C2H5OH"),
            ChemicalPreset("Углекислый газ", "CO2"),
            ChemicalPreset("Перекись водорода", "H2O2"),
            ChemicalPreset("Аммиак", "NH3"),
            ChemicalPreset("Соляная к-та", "HCl"),
            ChemicalPreset("Азотная к-та", "HNO3"),
            ChemicalPreset("Уксусная к-та", "CH3COOH"),
            ChemicalPreset("Едкий натр (NaOH)", "NaOH"),
            ChemicalPreset("Метан (Газ)", "CH4"),
            ChemicalPreset("Сахар (Сахароза)", "C12H22O11"),
            ChemicalPreset("Аспирин", "C9H8O4"),
            ChemicalPreset("Калийная селитра", "KNO3"),
            ChemicalPreset("Сульфат меди(II)", "CuSO4"),
            ChemicalPreset("Гашеная известь", "Ca(OH)2"),
            ChemicalPreset("Озон", "O3"),
            ChemicalPreset("Оксид железа(III)", "Fe2O3")
        )
    }

    val reactionPresets = remember {
        listOf(
            ReactionPreset("Горение метана", "CH4 + O2 -> CO2 + H2O"),
            ReactionPreset("Образование оксида железа(III)", "Fe + O2 -> Fe2O3"),
            ReactionPreset("Электролиз воды", "H2O -> H2 + O2"),
            ReactionPreset("Сода + Кислота", "NaHCO3 + HCl -> NaCl + CO2 + H2O"),
            ReactionPreset("Фотосинтез", "CO2 + H2O -> C6H12O6 + O2"),
            ReactionPreset("Нейтрализация щёлочи", "NaOH + HCl -> NaCl + H2O"),
            ReactionPreset("Синтез аммиака", "N2 + H2 -> NH3"),
            ReactionPreset("Горение водорода", "H2 + O2 -> H2O"),
            ReactionPreset("Разложение известняка", "CaCO3 -> CaO + CO2")
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()

            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedTab >= 0) {
            TextButton(onClick = { selectedTab = -1 }) { Text("‹  Разделы химии") }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
          sectionState.SaveableStateProvider(selectedTab) {
           when (selectedTab) {
            -1 -> ChemistryLibrary { selectedTab = it }
            0 -> MolarMassSection(
                formulaInput = formulaInput,
                onFormulaChange = { formulaInput = it },
                molarPresets = molarPresets,
                modifier = Modifier.fillMaxSize()
            )
            1 -> BalancerSection(
                equationInput = equationInput,
                onEquationChange = { equationInput = it },
                reactionPresets = reactionPresets,
                modifier = Modifier.fillMaxSize()
            )
            2 -> PeriodicTableSection(
                onElementClick = { selectedElement = it },
                modifier = Modifier.fillMaxSize()
            )
            3 -> AtomModelSection(
                modifier = Modifier.fillMaxSize()
            )
            4 -> SolubilityTableSection(
                onPairClick = { selectedSolubilityPair = it },
                modifier = Modifier.fillMaxSize()
            )
            5 -> StructureEditorSection(Modifier.fillMaxSize())
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
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
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
                    val standardMass = elem.atomicMassType == com.calculator.domain.model.AtomicMassType.STANDARD_ATOMIC_WEIGHT
                    val massLabel = if (standardMass) "Относительная атомная масса" else "Масса выбранного изотопа"
                    val uncertainty = elem.atomicMassUncertainty?.let { " ± $it" }.orEmpty()
                    Text("• $massLabel: ${elem.atomicMass}$uncertainty${if (standardMass) "" else " а.е.м."}", color = Color.White, fontSize = 13.sp)
                    Text("• Электроотрицательность (Полинг): ${elem.electronegativity?.toString() ?: "нет данных"}", color = TextSecondary, fontSize = 12.sp)
                    Text("Источник массы: ${elem.atomicMassSource}", color = TextSecondary, fontSize = 11.sp)
                    if (!standardMass) {
                        Text("Стандартная атомная масса не установлена. Расчёт использует массу выбранного изотопа из справочника, а не природной смеси.", color = TextSecondary, fontSize = 11.sp)
                    }
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
                    Text("Вставить в формулу ", color = Background, fontWeight = FontWeight.Bold)
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
                            SolubilityStatus.UNKNOWN -> "Для этой пары нет проверенных данных. Не используйте клетку как справочное значение."
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

private data class ChemistryDestination(val index: Int, val title: String, val detail: String, val mark: String)

@Composable
private fun ChemistryLibrary(onSelect: (Int) -> Unit) {
    val sections = listOf(
        ChemistryDestination(0, "Молярная масса", "Состав вещества и вклад каждого элемента", "M"),
        ChemistryDestination(1, "Реакции", "Подберите коэффициенты химического уравнения", "⇄"),
        ChemistryDestination(2, "Элементы", "Таблица Менделеева, свойства и атомные массы", "He"),
        ChemistryDestination(3, "Модель атома", "Рассмотрите электронные оболочки и ядро", "e⁻"),
        ChemistryDestination(4, "Растворимость", "Исследуйте сочетания ионов в воде", "aq"),
        ChemistryDestination(5, "Структуры", "Соберите простую молекулу и сохраните MOL", "C–O")
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("chemistry_library"),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Из чего состоит мир", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Text("От отдельного атома — к веществам и реакциям.", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        }
        items(sections, key = { it.index }) { section ->
            GlassCard(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button) { onSelect(section.index) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(section.mark, style = MaterialTheme.typography.headlineSmall, color = TextSecondary,
                        modifier = Modifier.width(44.dp).padding(top = 3.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(section.title, style = MaterialTheme.typography.titleLarge)
                        Text(section.detail, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
