package com.calculator.domain.model

enum class SolubilityStatus(val code: String, val title: String, val hexColor: Long) {
    SOLUBLE("Р", "Растворимо", 0xFF10B981),        // Emerald Green
    INSOLUBLE("Н", "Нерастворимо", 0xFFEF4444),      // Crimson Red
    SLIGHTLY_SOLUBLE("М", "Малорастворимо", 0xFFF59E0B), // Amber Yellow
    DECOMPOSES("—", "Разлагается водой", 0xFF6B7280)    // Muted Gray
}

object SolubilityData {
    val cations = listOf("H⁺", "NH₄⁺", "K⁺", "Na⁺", "Ba²⁺", "Ca²⁺", "Mg²⁺", "Al³⁺", "Mn²⁺", "Zn²⁺", "Fe²⁺", "Fe³⁺", "Cu²⁺", "Ag⁺", "Pb²⁺")
    val anions = listOf("OH⁻", "F⁻", "Cl⁻", "Br⁻", "I⁻", "S²⁻", "SO₃²⁻", "SO₄²⁻", "NO₃⁻", "PO₄³⁻", "CO₃²⁻", "SiO₃²⁻", "CH₃COO⁻")

    private val grid: Map<Pair<String, String>, SolubilityStatus> = mapOf(
        // OH-
        ("H⁺" to "OH⁻") to SolubilityStatus.SOLUBLE,
        ("NH₄⁺" to "OH⁻") to SolubilityStatus.SOLUBLE,
        ("K⁺" to "OH⁻") to SolubilityStatus.SOLUBLE,
        ("Na⁺" to "OH⁻") to SolubilityStatus.SOLUBLE,
        ("Ba²⁺" to "OH⁻") to SolubilityStatus.SOLUBLE,
        ("Ca²⁺" to "OH⁻") to SolubilityStatus.SLIGHTLY_SOLUBLE,
        ("Mg²⁺" to "OH⁻") to SolubilityStatus.INSOLUBLE,
        ("Al³⁺" to "OH⁻") to SolubilityStatus.INSOLUBLE,
        ("Mn²⁺" to "OH⁻") to SolubilityStatus.INSOLUBLE,
        ("Zn²⁺" to "OH⁻") to SolubilityStatus.INSOLUBLE,
        ("Fe²⁺" to "OH⁻") to SolubilityStatus.INSOLUBLE,
        ("Fe³⁺" to "OH⁻") to SolubilityStatus.INSOLUBLE,
        ("Cu²⁺" to "OH⁻") to SolubilityStatus.INSOLUBLE,
        ("Ag⁺" to "OH⁻") to SolubilityStatus.DECOMPOSES,
        ("Pb²⁺" to "OH⁻") to SolubilityStatus.INSOLUBLE,

        // Cl-
        ("H⁺" to "Cl⁻") to SolubilityStatus.SOLUBLE,
        ("NH₄⁺" to "Cl⁻") to SolubilityStatus.SOLUBLE,
        ("K⁺" to "Cl⁻") to SolubilityStatus.SOLUBLE,
        ("Na⁺" to "Cl⁻") to SolubilityStatus.SOLUBLE,
        ("Ba²⁺" to "Cl⁻") to SolubilityStatus.SOLUBLE,
        ("Ca²⁺" to "Cl⁻") to SolubilityStatus.SOLUBLE,
        ("Mg²⁺" to "Cl⁻") to SolubilityStatus.SOLUBLE,
        ("Al³⁺" to "Cl⁻") to SolubilityStatus.SOLUBLE,
        ("Mn²⁺" to "Cl⁻") to SolubilityStatus.SOLUBLE,
        ("Zn²⁺" to "Cl⁻") to SolubilityStatus.SOLUBLE,
        ("Fe²⁺" to "Cl⁻") to SolubilityStatus.SOLUBLE,
        ("Fe³⁺" to "Cl⁻") to SolubilityStatus.SOLUBLE,
        ("Cu²⁺" to "Cl⁻") to SolubilityStatus.SOLUBLE,
        ("Ag⁺" to "Cl⁻") to SolubilityStatus.INSOLUBLE,
        ("Pb²⁺" to "Cl⁻") to SolubilityStatus.SLIGHTLY_SOLUBLE,

        // SO4 2-
        ("H⁺" to "SO₄²⁻") to SolubilityStatus.SOLUBLE,
        ("NH₄⁺" to "SO₄²⁻") to SolubilityStatus.SOLUBLE,
        ("K⁺" to "SO₄²⁻") to SolubilityStatus.SOLUBLE,
        ("Na⁺" to "SO₄²⁻") to SolubilityStatus.SOLUBLE,
        ("Ba²⁺" to "SO₄²⁻") to SolubilityStatus.INSOLUBLE,
        ("Ca²⁺" to "SO₄²⁻") to SolubilityStatus.SLIGHTLY_SOLUBLE,
        ("Mg²⁺" to "SO₄²⁻") to SolubilityStatus.SOLUBLE,
        ("Al³⁺" to "SO₄²⁻") to SolubilityStatus.SOLUBLE,
        ("Mn²⁺" to "SO₄²⁻") to SolubilityStatus.SOLUBLE,
        ("Zn²⁺" to "SO₄²⁻") to SolubilityStatus.SOLUBLE,
        ("Fe²⁺" to "SO₄²⁻") to SolubilityStatus.SOLUBLE,
        ("Fe³⁺" to "SO₄²⁻") to SolubilityStatus.SOLUBLE,
        ("Cu²⁺" to "SO₄²⁻") to SolubilityStatus.SOLUBLE,
        ("Ag⁺" to "SO₄²⁻") to SolubilityStatus.SLIGHTLY_SOLUBLE,
        ("Pb²⁺" to "SO₄²⁻") to SolubilityStatus.INSOLUBLE,

        // CO3 2-
        ("H⁺" to "CO₃²⁻") to SolubilityStatus.SOLUBLE,
        ("NH₄⁺" to "CO₃²⁻") to SolubilityStatus.SOLUBLE,
        ("K⁺" to "CO₃²⁻") to SolubilityStatus.SOLUBLE,
        ("Na⁺" to "CO₃²⁻") to SolubilityStatus.SOLUBLE,
        ("Ba²⁺" to "CO₃²⁻") to SolubilityStatus.INSOLUBLE,
        ("Ca²⁺" to "CO₃²⁻") to SolubilityStatus.INSOLUBLE,
        ("Mg²⁺" to "CO₃²⁻") to SolubilityStatus.INSOLUBLE,
        ("Al³⁺" to "CO₃²⁻") to SolubilityStatus.DECOMPOSES,
        ("Mn²⁺" to "CO₃²⁻") to SolubilityStatus.INSOLUBLE,
        ("Zn²⁺" to "CO₃²⁻") to SolubilityStatus.INSOLUBLE,
        ("Fe²⁺" to "CO₃²⁻") to SolubilityStatus.INSOLUBLE,
        ("Fe³⁺" to "CO₃²⁻") to SolubilityStatus.DECOMPOSES,
        ("Cu²⁺" to "CO₃²⁻") to SolubilityStatus.INSOLUBLE,
        ("Ag⁺" to "CO₃²⁻") to SolubilityStatus.INSOLUBLE,
        ("Pb²⁺" to "CO₃²⁻") to SolubilityStatus.INSOLUBLE,

        // NO3-
        ("H⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,
        ("NH₄⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,
        ("K⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,
        ("Na⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,
        ("Ba²⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,
        ("Ca²⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,
        ("Mg²⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,
        ("Al³⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,
        ("Mn²⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,
        ("Zn²⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,
        ("Fe²⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,
        ("Fe³⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,
        ("Cu²⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,
        ("Ag⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,
        ("Pb²⁺" to "NO₃⁻") to SolubilityStatus.SOLUBLE,

        // S 2-
        ("H⁺" to "S²⁻") to SolubilityStatus.SOLUBLE,
        ("NH₄⁺" to "S²⁻") to SolubilityStatus.SOLUBLE,
        ("K⁺" to "S²⁻") to SolubilityStatus.SOLUBLE,
        ("Na⁺" to "S²⁻") to SolubilityStatus.SOLUBLE,
        ("Ba²⁺" to "S²⁻") to SolubilityStatus.SOLUBLE,
        ("Ca²⁺" to "S²⁻") to SolubilityStatus.SOLUBLE,
        ("Mg²⁺" to "S²⁻") to SolubilityStatus.SOLUBLE,
        ("Al³⁺" to "S²⁻") to SolubilityStatus.DECOMPOSES,
        ("Mn²⁺" to "S²⁻") to SolubilityStatus.INSOLUBLE,
        ("Zn²⁺" to "S²⁻") to SolubilityStatus.INSOLUBLE,
        ("Fe²⁺" to "S²⁻") to SolubilityStatus.INSOLUBLE,
        ("Fe³⁺" to "S²⁻") to SolubilityStatus.DECOMPOSES,
        ("Cu²⁺" to "S²⁻") to SolubilityStatus.INSOLUBLE,
        ("Ag⁺" to "S²⁻") to SolubilityStatus.INSOLUBLE,
        ("Pb²⁺" to "S²⁻") to SolubilityStatus.INSOLUBLE
    )

    fun getStatus(cation: String, anion: String): SolubilityStatus {
        return grid[cation to anion] ?: SolubilityStatus.SOLUBLE
    }
}
