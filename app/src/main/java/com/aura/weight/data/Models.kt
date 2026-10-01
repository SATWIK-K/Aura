package com.aura.weight.data

data class WeightEntry(
    val day: Long,
    val kg: Double,
    val note: String? = null,
    val recordedAt: Long = 0L,
)

enum class UnitSystem(val label: String) {
    KG("kg"),
    LB("lb"),
}

enum class BmiCategory(val label: String) {
    UNDER("underweight"),
    HEALTHY("healthy"),
    OVER("overweight"),
    OBESE("obese"),
}

data class UserProfile(
    val onboarded: Boolean = false,
    val unit: UnitSystem = UnitSystem.KG,
    val heightCm: Double = 0.0,
    val startKg: Double? = null,
    val targetKg: Double? = null,
    val backdropId: String = "aurora",
    val darkTheme: Boolean = true,
    val widgetAlpha: Int = 72,
)
