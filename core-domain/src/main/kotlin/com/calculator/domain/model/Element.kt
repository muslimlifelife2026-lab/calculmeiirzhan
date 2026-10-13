package com.calculator.domain.model

data class Element(
    val symbol: String,
    val name: String,
    val atomicNumber: Int,
    val atomicMass: Double,
    val nameRu: String = name,
    val category: String = "Неметалл",
    val group: Int = 1,
    val period: Int = 1,
    val electronConfig: String = "",
    val electronegativity: Double? = null,
    val oxidationStates: String = "",
    val atomicMassUncertainty: Double? = null,
    val atomicMassType: AtomicMassType = AtomicMassType.STANDARD_ATOMIC_WEIGHT
) {
    val atomicMassSource: String get() = when (atomicMassType) {
        AtomicMassType.STANDARD_ATOMIC_WEIGHT -> "CIAAW, Abridged Standard Atomic Weights 2024"
        AtomicMassType.REPRESENTATIVE_ISOTOPE_MASS -> "PubChem, масса выбранного изотопа"
    }
}

enum class AtomicMassType {
    STANDARD_ATOMIC_WEIGHT,
    REPRESENTATIVE_ISOTOPE_MASS
}
