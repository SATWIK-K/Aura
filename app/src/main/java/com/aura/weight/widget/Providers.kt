package com.aura.weight.widget

class TodayWidgetProvider : AuraWidgetProvider() {
    override val style: WidgetStyle = WidgetStyle.TODAY
}

class TrendWidgetProvider : AuraWidgetProvider() {
    override val style: WidgetStyle = WidgetStyle.TREND
}

class RingWidgetProvider : AuraWidgetProvider() {
    override val style: WidgetStyle = WidgetStyle.RING
}

class StreakWidgetProvider : AuraWidgetProvider() {
    override val style: WidgetStyle = WidgetStyle.STREAK
}

class BmiWidgetProvider : AuraWidgetProvider() {
    override val style: WidgetStyle = WidgetStyle.BMI
}

class CompactWidgetProvider : AuraWidgetProvider() {
    override val style: WidgetStyle = WidgetStyle.COMPACT
}

class ProgressWidgetProvider : AuraWidgetProvider() {
    override val style: WidgetStyle = WidgetStyle.PROGRESS
}
