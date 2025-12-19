# MakimaKey ProGuard Rules

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Keep crypto classes
-keep class com.makimakey.crypto.** { *; }
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# Keep model classes for serialization
-keep class com.makimakey.domain.model.** { *; }

# AndroidX Security
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Biometric
-keep class androidx.biometric.** { *; }

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Remove source file information
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Optimization
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Remove stack traces in release
-printmapping mapping.txt