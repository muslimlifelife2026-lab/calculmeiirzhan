package com.calculator.domain.model

/** Element masses and electron configurations are bundled from PubChem's periodic-table CSV. */
object PeriodicTable {
    private val russianNames = ("""
        Водород,Гелий,Литий,Бериллий,Бор,Углерод,Азот,Кислород,Фтор,Неон,
        Натрий,Магний,Алюминий,Кремний,Фосфор,Сера,Хлор,Аргон,Калий,Кальций,
        Скандий,Титан,Ванадий,Хром,Марганец,Железо,Кобальт,Никель,Медь,Цинк,
        Галлий,Германий,Мышьяк,Селен,Бром,Криптон,Рубидий,Стронций,Иттрий,Цирконий,
        Ниобий,Молибден,Технеций,Рутений,Родий,Палладий,Серебро,Кадмий,Индий,Олово,
        Сурьма,Теллур,Иод,Ксенон,Цезий,Барий,Лантан,Церий,Празеодим,Неодим,
        Прометий,Самарий,Европий,Гадолиний,Тербий,Диспрозий,Гольмий,Эрбий,Тулий,Иттербий,
        Лютеций,Гафний,Тантал,Вольфрам,Рений,Осмий,Иридий,Платина,Золото,Ртуть,
        Таллий,Свинец,Висмут,Полоний,Астат,Радон,Франций,Радий,Актиний,Торий,
        Протактиний,Уран,Нептуний,Плутоний,Америций,Кюрий,Берклий,Калифорний,Эйнштейний,Фермий,
        Менделевий,Нобелий,Лоуренсий,Резерфордий,Дубний,Сиборгий,Борий,Хассий,Мейтнерий,Дармштадтий,
        Рентгений,Коперниций,Нихоний,Флеровий,Московий,Ливерморий,Теннессин,Оганесон
    """).trimIndent().replace("\n", "").replace(" ", "").split(',')

    val elements: Map<String, Element> by lazy {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream("elements/periodic_table.tsv")) {
            "Periodic table resource is missing"
        }
        val entries = stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith('#') }.map { line ->
                val fields = line.split('\t')
                require(fields.size == 7) { "Malformed periodic table row: $line" }
                val number = fields[0].toInt()
                Element(
                    symbol = fields[1], name = fields[2], atomicNumber = number,
                    atomicMass = fields[3].toDouble(), nameRu = russianNames[number - 1],
                    category = fields[6], period = when {
                        number <= 2 -> 1; number <= 10 -> 2; number <= 18 -> 3
                        number <= 36 -> 4; number <= 54 -> 5; number <= 86 -> 6; else -> 7
                    },
                    electronConfig = fields[4], electronegativity = fields[5].toDoubleOrNull() ?: 0.0
                )
            }.toList()
        }
        require(entries.size == 118 && entries.map { it.atomicNumber } == (1..118).toList()) {
            "Periodic table must contain elements 1 through 118"
        }
        entries.associateBy { it.symbol }
    }

    val allElements: List<Element> get() = elements.values.toList()

    fun getElement(symbol: String): Element? = elements[symbol]

    /** Counts electrons by principal quantum number from the supplied configuration. */
    fun shellDistribution(atomicNumber: Int): List<Int> {
        val element = allElements.getOrNull(atomicNumber - 1) ?: return emptyList()
        val counts = IntArray(7)
        val config = element.electronConfig
        val core = Regex("\\[([A-Za-z]+)]").find(config)?.groupValues?.get(1)
        if (core != null) {
            shellDistribution(getElement(core)?.atomicNumber ?: return emptyList()).forEachIndexed { index, count ->
                counts[index] += count
            }
        }
        Regex("(\\d+)[spdfg](\\d+)").findAll(config).forEach { match ->
            val shell = match.groupValues[1].toInt()
            if (shell in 1..7) counts[shell - 1] += match.groupValues[2].toInt()
        }
        return if (counts.sum() == atomicNumber) counts.toList().dropLastWhile { it == 0 } else emptyList()
    }
}
