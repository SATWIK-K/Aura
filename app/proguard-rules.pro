# Compose + Kotlin
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.**
# Widget RemoteViews entry points are referenced from the manifest.
-keep class com.aura.weight.widget.** { *; }
