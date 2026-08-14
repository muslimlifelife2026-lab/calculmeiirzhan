# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\ASUS\AppData\Local\Android\Sdk\tools\proguard\proguard-android.txt
# You can edit the include path and share correct rules.

# Rules for Kotlin Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep serializable classes and their companion properties
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    companion object;
}
-keepclassmembers class * {
    *** Companion;
}
-keep class kotlinx.serialization.json.** { *; }

# Keep Room DB Components
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase$Callback
-keep class * implements androidx.room.RoomOpenHelper
-dontwarn androidx.room.**
-keep class com.calculator.core.data.database.** { *; }
-keep interface com.calculator.core.data.database.** { *; }

# Keep Domain Entities & Utilities
-keep class com.calculator.domain.model.** { *; }
-keep class com.calculator.domain.utils.** { *; }
-keep class com.calculator.core.data.database.CalculationEntity { *; }

# Keep BigDecimalMath library (ch.obermuhlner.math.big)
-keep class ch.obermuhlner.math.big.** { *; }
-dontwarn ch.obermuhlner.math.big.**

# Keep Compose/Material3
-keep class androidx.compose.** { *; }
