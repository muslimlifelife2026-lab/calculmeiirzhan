package com.calculator.feature.chemistry

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.ErrorRed
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.domain.model.chemistry.SimpleMolecule
import com.calculator.domain.model.chemistry.StructureDraft
import com.calculator.domain.model.chemistry.StructureElement
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StructureEditorSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("molecule_editor", Context.MODE_PRIVATE) }
    var encoded by rememberSaveable {
        mutableStateOf(StructureDraft.decode(preferences.getString("draft_v1", null))?.encode() ?: StructureDraft().encode())
    }
    val draft = remember(encoded) { StructureDraft.decode(encoded) ?: StructureDraft() }
    val molecule = draft.molecule
    var previous by rememberSaveable { mutableStateOf<String?>(null) }
    var linking by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var pendingExport by rememberSaveable { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("chemical/x-mdl-molfile")) { uri ->
        val document = pendingExport
        pendingExport = null
        if (uri != null && document != null) scope.launch {
            feedback = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(document.toByteArray(Charsets.UTF_8)) }
                        ?: throw IOException("Output unavailable")
                    "Файл MOL сохранён."
                } catch (_: IOException) {
                    "Не удалось сохранить файл. Выберите другую папку или скопируйте MOL."
                } catch (_: SecurityException) {
                    "Нет доступа к выбранному файлу. Выберите его повторно."
                }
            }
        }
    }

    fun update(next: StructureDraft, undoable: Boolean = true) {
        if (undoable) previous = encoded
        encoded = next.encode()
        preferences.edit().putString("draft_v1", encoded).apply()
        linking = false
        feedback = null
    }

    fun change(action: () -> StructureDraft) {
        try {
            update(action())
        } catch (error: IllegalArgumentException) {
            feedback = error.message ?: "Такую связь добавить нельзя."
        } catch (error: IllegalStateException) {
            feedback = error.message ?: "Сначала выберите атом."
        }
    }

    fun selectAtom(id: Int) {
        val from = draft.selectedAtomId
        if (linking && from != null) {
            change { draft.copy(molecule = molecule.withBond(from, id, draft.bondOrder), selectedAtomId = id) }
        } else {
            update(draft.copy(selectedAtomId = id), undoable = false)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("structure_editor"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Редактор структур", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text("Выберите атом на схеме, затем добавьте соседний. Свободные валентности C, N и O дополняются водородом.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Вода", "Этанол", "Этен", "Азот")) { name ->
                AssistChip(onClick = {
                    val example = when (name) {
                        "Вода" -> SimpleMolecule().addAtom(StructureElement.O)
                        "Этанол" -> SimpleMolecule().addAtom(StructureElement.C).addAtom(StructureElement.C, 1).addAtom(StructureElement.O, 2)
                        "Этен" -> SimpleMolecule().addAtom(StructureElement.C).addAtom(StructureElement.C, 1, 2)
                        else -> SimpleMolecule().addAtom(StructureElement.N).addAtom(StructureElement.N, 1, 3)
                    }
                    update(draft.copy(molecule = example, selectedAtomId = example.atoms.first().id))
                }, label = { Text(name) })
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (molecule.atoms.isEmpty()) {
                    Text("Начните с примера выше или добавьте первый атом ниже.", color = Color.White, modifier = Modifier.padding(vertical = 36.dp))
                } else {
                    StructureCanvas(molecule, draft.selectedAtomId, ::selectAtom)
                    Text("Атомы можно выбрать и по номеру:", color = TextSecondary, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(molecule.atoms, key = { it.id }) { atom ->
                            FilterChip(
                                selected = atom.id == draft.selectedAtomId,
                                onClick = { selectAtom(atom.id) },
                                label = { Text("${atom.element}${atom.id}") },
                                modifier = Modifier.semantics { contentDescription = "Атом ${atom.element}, номер ${atom.id}" }
                            )
                        }
                    }
                }
            }
        }

        Text(if (molecule.atoms.isEmpty()) "Первый атом" else "Добавить к атому ${draft.selectedAtomId ?: "—"}", color = Color.White, fontWeight = FontWeight.Medium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StructureElement.entries.forEach { element ->
                FilterChip(
                    selected = draft.nextElement == element,
                    onClick = { update(draft.copy(nextElement = element), undoable = false) },
                    label = { Text(element.name) }
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..3).forEach { order ->
                FilterChip(
                    selected = draft.bondOrder == order,
                    onClick = { update(draft.copy(bondOrder = order), undoable = false) },
                    label = { Text("Связь $order") }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                change {
                    val result = molecule.addAtom(draft.nextElement, draft.selectedAtomId, draft.bondOrder)
                    draft.copy(molecule = result, selectedAtomId = result.atoms.last().id)
                }
            }, enabled = molecule.atoms.size < SimpleMolecule.MAX_ATOMS, modifier = Modifier.weight(1f)) {
                Text("Добавить ${draft.nextElement}")
            }
            OutlinedButton(onClick = { linking = !linking; feedback = null }, enabled = molecule.atoms.size >= 2 && draft.selectedAtomId != null, modifier = Modifier.weight(1f)) {
                Text(if (linking) "Отмена связи" else "Соединить")
            }
        }
        if (linking) Text("Нажмите второй атом на схеме или в списке. Существующая связь получит выбранный порядок ${draft.bondOrder}.", color = Color.White, fontSize = 13.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val id = draft.selectedAtomId ?: return@OutlinedButton
                val result = molecule.removeAtom(id)
                update(draft.copy(molecule = result, selectedAtomId = result.atoms.lastOrNull()?.id))
            }, enabled = draft.selectedAtomId != null, modifier = Modifier.weight(1f)) { Text("Удалить атом") }
            OutlinedButton(onClick = {
                val restored = StructureDraft.decode(previous) ?: return@OutlinedButton
                update(restored, undoable = false)
                previous = null
            }, enabled = previous != null, modifier = Modifier.weight(1f)) { Text("Отменить шаг") }
        }

        if (molecule.bonds.isNotEmpty()) {
            Text("Удалить связь (атомы сохранятся):", color = TextSecondary, fontSize = 12.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(molecule.bonds) { bond ->
                    AssistChip(
                        onClick = { update(draft.copy(molecule = molecule.removeBond(bond.first, bond.second))) },
                        label = { Text("${bond.first} — ${bond.second} · ${bond.order}") },
                        modifier = Modifier.semantics { contentDescription = "Удалить связь между атомами ${bond.first} и ${bond.second}, порядок ${bond.order}" }
                    )
                }
            }
        }

        feedback?.let { Text(it, color = Color.White, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (molecule.isComplete) {
                    Text(molecule.formula(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("structure_formula"))
                    Text("Молярная масса ≈ ${molecule.molarMass().stripTrailingZeros().toPlainString()} г/моль", color = Color.White)
                    Text("По сокращённым атомным весам CIAAW 2024.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    Text(molecule.incompleteReason.orEmpty(), color = TextSecondary)
                }
                Text("Нейтральные структуры C/H/O/N с валентностями 4/1/2/3. До ${SimpleMolecule.MAX_ATOMS} атомов на схеме. Ионы, радикалы, ароматичность и стереохимия не описываются. Проверка валентности не доказывает устойчивость вещества.", color = TextSecondary, fontSize = 12.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                pendingExport = molecule.toMolfile()
                saveFile.launch("${molecule.formula()}.mol")
            }, enabled = molecule.isComplete, modifier = Modifier.weight(1f)) { Text("Сохранить MOL") }
            OutlinedButton(onClick = {
                clipboard.setText(AnnotatedString(molecule.toMolfile()))
                feedback = "MOL скопирован. В файле водород указан явно; стереохимия не задана."
            }, enabled = molecule.isComplete, modifier = Modifier.weight(1f)) { Text("Копировать MOL") }
        }
        Text("MOL V2000: схема связей и состав, без расчёта реальной геометрии молекулы. Черновик сохраняется на этом устройстве.", color = TextSecondary, fontSize = 12.sp)
        TextButton(onClick = { confirmReset = true }, enabled = molecule.atoms.isNotEmpty()) { Text("Очистить схему", color = ErrorRed) }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Очистить схему?") },
        text = { Text("Все атомы и связи будут удалены. Действие можно отменить кнопкой «Отменить шаг».") },
        confirmButton = { TextButton(onClick = { update(StructureDraft()); confirmReset = false }) { Text("Очистить") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Оставить") } }
    )
}

@Composable
private fun StructureCanvas(molecule: SimpleMolecule, selectedId: Int?, onSelect: (Int) -> Unit) {
    val currentOnSelect by rememberUpdatedState(onSelect)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val minX = molecule.atoms.minOf { it.x }
    val maxX = molecule.atoms.maxOf { it.x }
    val minY = molecule.atoms.minOf { it.y }
    val maxY = molecule.atoms.maxOf { it.y }
    val padding = with(density) { 32.dp.toPx() }
    val points = remember(molecule, canvasSize, padding) {
        val scale = minOf((canvasSize.width - 2 * padding) / (maxX - minX).coerceAtLeast(3.0), (canvasSize.height - 2 * padding) / (maxY - minY).coerceAtLeast(2.5)).coerceAtLeast(1.0)
        molecule.atoms.associate { atom -> atom.id to Offset((canvasSize.width / 2 + (atom.x - (minX + maxX) / 2) * scale).toFloat(), (canvasSize.height / 2 - (atom.y - (minY + maxY) / 2) * scale).toFloat()) }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val drawingWidth = maxOf(maxWidth, ((maxX - minX) * 55 + 64).dp)
        val drawingHeight = maxOf(210.dp, ((maxY - minY) * 55 + 64).dp)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            Canvas(
                Modifier.width(drawingWidth).height(drawingHeight).onSizeChanged { canvasSize = it }
                    .semantics { contentDescription = "Схема: ${molecule.atoms.size} атомов, ${molecule.bonds.size} связей. Выбор атомов также доступен в списке ниже." }
                    .pointerInput(points) {
                        detectTapGestures { point ->
                            points.minByOrNull { (_, position) -> (position - point).getDistance() }
                                ?.takeIf { (_, position) -> (position - point).getDistance() <= padding }
                                ?.let { currentOnSelect(it.key) }
                        }
                    }
            ) {
                molecule.bonds.forEach { bond ->
                    val first = points.getValue(bond.first)
                    val second = points.getValue(bond.second)
                    val vector = second - first
                    val distance = vector.getDistance().coerceAtLeast(1f)
                    val unit = vector / distance
                    val perpendicular = Offset(-unit.y, unit.x)
                    repeat(bond.order) { index ->
                        val shift = perpendicular * ((index - (bond.order - 1) / 2f) * 5.dp.toPx())
                        drawLine(Color(0xFFDADEE4), first + unit * 22.dp.toPx() + shift, second - unit * 22.dp.toPx() + shift, strokeWidth = 2.dp.toPx())
                    }
                }
                molecule.atoms.forEach { atom ->
                    val position = points.getValue(atom.id)
                    drawCircle(Color(0xFF30343B), 23.dp.toPx(), position)
                    drawCircle(if (atom.id == selectedId) Color.White else Color(0xFF79808A), 23.dp.toPx(), position, style = Stroke(if (atom.id == selectedId) 2.dp.toPx() else 0.75.dp.toPx()))
                    val hydrogenCount = molecule.implicitHydrogens(atom.id)
                    val label = atom.element.name + when (hydrogenCount) { 0 -> ""; 1 -> "H"; 2 -> "H₂"; 3 -> "H₃"; else -> "H₄" }
                    val text = textMeasurer.measure(label, TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium))
                    drawText(text, topLeft = position - Offset(text.size.width / 2f, text.size.height / 2f))
                    val idText = textMeasurer.measure(atom.id.toString(), TextStyle(color = TextSecondary, fontSize = 11.sp))
                    drawText(idText, topLeft = position + Offset(-idText.size.width / 2f, 25.dp.toPx()))
                }
            }
        }
    }
}
