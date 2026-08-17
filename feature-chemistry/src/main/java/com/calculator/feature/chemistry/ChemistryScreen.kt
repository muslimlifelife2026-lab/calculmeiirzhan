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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChemistryScreen(
    modifier: Modifier = Modifier
) {
    var formulaInput by remember { mutableStateOf("") }
    var equationInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Mass, 1: Balancer, 2: Periodic Table, 3: Atom Model, 4: Solubility
    var selectedElement by remember { mutableStateOf<Element?>(null) }
    var selectedSolubilityPair by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedAtomElement by remember { mutableStateOf(PeriodicTable.getElement("C") ?: Element("C", "Carbon", 6, 12.011)) }
    
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🧪 Молярная Масса",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Введите химическую формулу для автоматического расчета молярной массы.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Химическая формула",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        
                        TextField(
                            value = formulaInput,
                            onValueChange = { formulaInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0x1AFFFFFF),
                                unfocusedContainerColor = Color(0x0DFFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = AccentCyan,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            placeholder = {
                                Text(
                                    text = "H2SO4, Fe2(SO4)3...",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 20.sp
                                )
                            }
                        )

                        // Quick Elements
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            val elements = listOf("H", "O", "C", "N", "Na", "Cl", "Fe", "Cu", "Ca", "Ba", "S")
                            items(elements) { element ->
                                FilterChip(
                                    selected = false,
                                    onClick = { formulaInput += element },
                                    label = { Text(element, color = Color.White) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0x1AFFFFFF)
                                    )
                                )
                            }
                        }
                    }
                }

                if (massResult != null) {
                    if (massResult.success) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Молярная масса:",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                                
                                Text(
                                    text = String.format("%.3f г/моль", massResult.molarMass),
                                    color = AccentCyan,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                                Text(
                                    text = "Состав элементов:",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)
                                ) {
                                    items(massResult.elementCounts.toList()) { (element, count) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(AccentCyan.copy(alpha = 0.15f))
                                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = element.symbol,
                                                        color = AccentCyan,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                                Text(
                                                    text = element.name,
                                                    color = TextSecondary,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Text(
                                                text = "$count × ${element.atomicMass}",
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = massResult.errorMessage ?: "Ошибка расчёта",
                                color = ErrorRed,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            1 -> {
                // ─── EQUATION BALANCER MODE ───────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "⚖️ Балансировщик Реакций",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Введите реакцию (например: Fe + O2 = Fe2O3 или H2 + O2 = H2O).",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Химическое уравнение",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        
                        TextField(
                            value = equationInput,
                            onValueChange = { equationInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0x1AFFFFFF),
                                unfocusedContainerColor = Color(0x0DFFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = AccentCyan,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            placeholder = {
                                Text(
                                    text = "H2 + O2 = H2O",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 18.sp
                                )
                            }
                        )

                        // Quick helpers for equations
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            val symbols = listOf(" + ", " = ", "H2O", "CO2", "O2", "HCl", "NaOH")
                            items(symbols) { item ->
                                FilterChip(
                                    selected = false,
                                    onClick = { equationInput += item },
                                    label = { Text(item.trim(), color = Color.White) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0x1AFFFFFF)
                                    )
                                )
                            }
                        }
                    }
                }

                if (balanceResult != null) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Результат балансировки:",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )

                            val isError = balanceResult.startsWith("Ошибка") || balanceResult.startsWith("Не удалось")
                            
                            Text(
                                text = balanceResult,
                                color = if (isError) ErrorRed else com.calculator.core.ui.theme.AccentAmber,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            2 -> {
                // ─── PERIODIC TABLE MODE ─────────────────────────────────────────────
                var searchQuery by remember { mutableStateOf("") }
                val allElements = remember { PeriodicTable.elements.values.toList() }
                val filteredElements = remember(searchQuery) {
                    if (searchQuery.isBlank()) allElements else {
                        allElements.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.symbol.contains(searchQuery, ignoreCase = true) ||
                            it.atomicNumber.toString() == searchQuery
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Поиск элемента (символ, имя, номер)...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentViolet,
                            focusedContainerColor = Color(0x0AFFFFFF),
                            unfocusedContainerColor = Color(0x0AFFFFFF)
                        ),
                        singleLine = true
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredElements) { elem ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x1AFFFFFF))
                                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                                    .clickable { selectedElement = elem }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = elem.atomicNumber.toString(),
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 9.sp,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Text(
                                        text = elem.symbol,
                                        color = AccentCyan,
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
                val sampleElements = remember {
                    PeriodicTable.allElements
                }

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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Нажмите на ячейку для просмотра подробностей",
                        color = Color.White.copy(alpha = 0.5f),
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
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentCyan.copy(alpha = 0.2f))
                            .border(1.dp, AccentCyan, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(elem.symbol, color = AccentCyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(elem.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                    HorizontalDivider(color = Color(0x22FFFFFF))
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
                    colors = ButtonDefaults.buttonColors(containerColor = AccentViolet)
                ) {
                    Text("Вставить в формулу 🧪", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedElement = null }) {
                    Text("Закрыть", color = TextSecondary)
                }
            },
            containerColor = com.calculator.core.ui.theme.SurfaceCard
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
                    Text("Понятно", color = AccentCyan)
                }
            },
            containerColor = com.calculator.core.ui.theme.SurfaceCard
        )
    }
}
