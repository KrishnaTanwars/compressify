# Compressify Proguard / R8 Rules

# Keep Jetpack Compose rules
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }

# Coil image loader
-keep class io.coilkt.** { *; }
-dontwarn io.coilkt.**

# ExifInterface
-keep class androidx.exifinterface.media.ExifInterface { *; }

# Kotlin coroutines
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class kotlinx.coroutines.** { *; }
