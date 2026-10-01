package com.aura.weight.data

object Units {
    const val LB_PER_KG = 2.2046226218

    fun toDisplay(kg: Double, unit: UnitSystem): Double =
        if (unit == UnitSystem.KG) kg else kg * LB_PER_KG

    fun fromDisplay(value: Double, unit: UnitSystem): Double =
        if (unit == UnitSystem.KG) value else value / LB_PER_KG

    fun label(unit: UnitSystem): String = unit.label

    fun format(kg: Double, unit: UnitSystem, decimals: Int = 1): String {
        val value = toDisplay(kg, unit)
        return if (decimals <= 0) "${value.toInt()} ${label(unit)}"
        else String.format("%.${decimals}f %s", value, label(unit))
    }

    fun formatSigned(kgDelta: Double, unit: UnitSystem, decimals: Int = 1): String {
        val value = toDisplay(kgDelta, unit)
        val sign = if (value < 0) "−" else "+"
        val abs = kotlin.math.abs(value)
        val body = if (decimals <= 0) abs.toInt().toString()
        else String.format("%.${decimals}f", abs)
        return "$sign$body ${label(unit)}"
    }
}
