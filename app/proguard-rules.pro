# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Anti-Tampering Layer A - ProGuard/R8 Obfuscation:
-keep class com.akanjimusab.hifzguard.integrity.** { *; }
-keep class com.akanjimusab.hifzguard.BuildConfig { *; }
-keep class com.example.integrity.** { *; }
-keep class com.example.BuildConfig { *; }
-assumenosideeffects class android.util.Log { *; }
-renamesourcefileattribute SourceFile
-keepattributes Signature
-keepattributes *Annotation*
