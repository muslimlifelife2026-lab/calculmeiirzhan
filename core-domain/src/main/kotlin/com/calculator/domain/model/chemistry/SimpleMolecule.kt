package com.calculator.domain.model.chemistry

import com.calculator.domain.model.PeriodicTable
import java.math.BigDecimal
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Supported neutral, closed-shell valences, not a general chemical validity/stability predictor. */
@Serializable
enum class StructureElement(val valence: Int) { C(4), H(1), O(2), N(3) }

@Serializable
data class StructureAtom(val id: Int, val element: StructureElement, val x: Double, val y: Double)

@Serializable
data class StructureBond(val first: Int, val second: Int, val order: Int)

/**
 * Small, local connection-table editor. No charges, radicals, aromatic bonds or stereochemistry.
 * Ordinary CHON valences are completed with implicit H, except for explicit H atoms themselves.
 * Coordinates describe a drawing in arbitrary units, never measured molecular geometry.
 */
@Serializable
data class SimpleMolecule(
    val atoms: List<StructureAtom> = emptyList(),
    val bonds: List<StructureBond> = emptyList()
) {
    init {
        require(atoms.size <= MAX_ATOMS) { "В этой версии можно разместить до $MAX_ATOMS атомов." }
        require(atoms.all { it.id in 1..100_000 && it.x.isFinite() && it.y.isFinite() && abs(it.x) <= 999 && abs(it.y) <= 999 }) {
            "Некорректные координаты или номера атомов."
        }
        val ids = atoms.map { it.id }.toSet()
        require(ids.size == atoms.size) { "Номера атомов должны различаться." }
        require(bonds.all { it.first in ids && it.second in ids && it.first != it.second && it.order in 1..3 }) {
            "Связь должна соединять разные атомы и иметь порядок 1, 2 или 3."
        }
        require(bonds.map { setOf(it.first, it.second) }.toSet().size == bonds.size) { "Между атомами уже есть связь." }
        atoms.forEach { atom ->
            require(bondOrderSum(atom.id) <= atom.element.valence) {
                "У ${atom.element} допустимая сумма порядков связей — ${atom.element.valence}. Выберите другой атом или уменьшите порядок связи."
            }
        }
    }

    fun bondOrderSum(atomId: Int): Int = bonds.sumOf { if (it.first == atomId || it.second == atomId) it.order else 0 }

    fun implicitHydrogens(atomId: Int): Int {
        val atom = atoms.first { it.id == atomId }
        return if (atom.element == StructureElement.H) 0 else atom.element.valence - bondOrderSum(atomId)
    }

    val incompleteReason: String?
        get() {
            if (atoms.isEmpty()) return "Добавьте первый атом."
            if (atoms.any { it.element == StructureElement.H && bondOrderSum(it.id) != 1 }) {
                return "Соедините свободный атом H одинарной связью. Радикалы здесь не поддерживаются."
            }
            val visited = mutableSetOf(atoms.first().id)
            val pending = ArrayDeque<Int>()
            pending.add(atoms.first().id)
            while (pending.isNotEmpty()) {
                val id = pending.removeFirst()
                bonds.forEach { bond ->
                    val neighbor = when (id) { bond.first -> bond.second; bond.second -> bond.first; else -> null }
                    if (neighbor != null && visited.add(neighbor)) pending.add(neighbor)
                }
            }
            return if (visited.size == atoms.size) null else "В схеме несколько фрагментов. Соедините их перед расчётом и экспортом."
        }

    val isComplete: Boolean get() = incompleteReason == null

    fun addAtom(element: StructureElement, attachedTo: Int? = null, order: Int = 1): SimpleMolecule {
        require(order in 1..3) { "Выберите порядок связи от 1 до 3." }
        require(atoms.isEmpty() || attachedTo != null) { "Сначала выберите атом, к которому добавить соседний." }
        val anchor = attachedTo?.let { id -> atoms.firstOrNull { it.id == id } ?: error("Атом не найден.") }
        val id = (atoms.maxOfOrNull { it.id } ?: 0) + 1
        val position = if (anchor == null) 0.0 to 0.0 else freePosition(anchor)
        val atom = StructureAtom(id, element, position.first, position.second)
        return copy(
            atoms = atoms + atom,
            bonds = if (anchor == null) bonds else bonds + StructureBond(anchor.id, id, order)
        )
    }

    fun withBond(first: Int, second: Int, order: Int): SimpleMolecule = copy(
        bonds = bonds.filterNot { setOf(it.first, it.second) == setOf(first, second) } + StructureBond(first, second, order)
    )

    fun removeBond(first: Int, second: Int): SimpleMolecule = copy(
        bonds = bonds.filterNot { setOf(it.first, it.second) == setOf(first, second) }
    )

    fun removeAtom(id: Int): SimpleMolecule = copy(
        atoms = atoms.filterNot { it.id == id },
        bonds = bonds.filterNot { it.first == id || it.second == id }
    )

    private fun composition(): Map<String, Int> {
        require(isComplete) { incompleteReason.orEmpty() }
        val result = atoms.groupingBy { it.element.name }.eachCount().toMutableMap()
        val implicit = atoms.sumOf { implicitHydrogens(it.id) }
        if (implicit > 0) result["H"] = (result["H"] ?: 0) + implicit
        return result
    }

    /** Hill order: C, then H, then alphabetical; without C, all symbols alphabetical. */
    fun formula(): String {
        val counts = composition()
        val symbols = if ("C" in counts) listOf("C", "H").filter { it in counts } + counts.keys.filter { it != "C" && it != "H" }.sorted()
        else counts.keys.sorted()
        return symbols.joinToString("") { symbol -> symbol + if (counts.getValue(symbol) == 1) "" else counts.getValue(symbol).toString() }
    }

    /** Nominal molar mass, g/mol, using the bundled CIAAW abridged standard atomic weights. */
    fun molarMass(): BigDecimal = composition().entries.fold(BigDecimal.ZERO) { mass, (symbol, count) ->
        mass + BigDecimal.valueOf(requireNotNull(PeriodicTable.getElement(symbol)).atomicMass) * count.toBigDecimal()
    }

    /**
     * BIOVIA CTfile Formats 2020, ch. 6–7 (V2000). Every implied H is exported explicitly.
     * Double-bond stereo = 3 (unspecified), so the sketch does not accidentally encode E/Z.
     * https://discover.3ds.com/sites/default/files/2020-08/biovia_ctfileformats_2020.pdf
     */
    fun toMolfile(): String {
        require(isComplete) { incompleteReason.orEmpty() }
        val exportAtoms = atoms.toMutableList()
        val exportBonds = bonds.toMutableList()
        var nextId = atoms.maxOf { it.id } + 1
        atoms.forEach { atom ->
            repeat(implicitHydrogens(atom.id)) {
                val position = freePosition(atom, exportAtoms, 0.75)
                val hydrogen = StructureAtom(nextId++, StructureElement.H, position.first, position.second)
                exportAtoms.add(hydrogen)
                exportBonds.add(StructureBond(atom.id, hydrogen.id, 1))
            }
        }
        val indices = exportAtoms.mapIndexed { index, atom -> atom.id to index + 1 }.toMap()
        return buildString {
            appendLine("AeroCalc ${formula()}")
            appendLine("  AeroCalc          2D")
            appendLine("Neutral CHON; explicit H; stereochemistry unspecified")
            appendLine(String.format(Locale.US, "%3d%3d  0  0  0  0            999 V2000", exportAtoms.size, exportBonds.size))
            exportAtoms.forEach { atom ->
                appendLine(String.format(Locale.US, "%10.4f%10.4f%10.4f %-3s 0  0  0  0  0  0  0  0  0  0  0  0", atom.x, atom.y, 0.0, atom.element.name))
            }
            exportBonds.forEach { bond ->
                appendLine(String.format(Locale.US, "%3d%3d%3d%3d  0  0  0", indices.getValue(bond.first), indices.getValue(bond.second), bond.order, if (bond.order == 2) 3 else 0))
            }
            appendLine("M  END")
        }
    }

    private fun freePosition(anchor: StructureAtom, occupied: List<StructureAtom> = atoms, length: Double = 1.5): Pair<Double, Double> {
        val candidates = listOf(0, 60, -60, 120, -120, 180).map { degrees ->
            val angle = degrees * PI / 180.0
            anchor.x + length * cos(angle) to anchor.y + length * sin(angle)
        }
        return candidates.maxBy { (x, y) ->
            occupied.filter { it.id != anchor.id }.minOfOrNull { atom ->
                val dx = x - atom.x
                val dy = y - atom.y
                dx * dx + dy * dy
            } ?: 0.0
        }
    }

    companion object { const val MAX_ATOMS = 24 }
}

/** Entire editor draft, including selection and controls, survives recreation/navigation. */
@Serializable
data class StructureDraft(
    val molecule: SimpleMolecule = SimpleMolecule(),
    val selectedAtomId: Int? = null,
    val nextElement: StructureElement = StructureElement.C,
    val bondOrder: Int = 1
) {
    init {
        require(selectedAtomId == null || molecule.atoms.any { it.id == selectedAtomId })
        require(bondOrder in 1..3)
    }

    fun encode(): String = Json.encodeToString(this)

    companion object {
        fun decode(value: String?): StructureDraft? {
            if (value == null || value.length > 32_768) return null
            return try {
                Json.decodeFromString<StructureDraft>(value)
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}
