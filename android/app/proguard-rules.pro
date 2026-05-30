# Reglas de ProGuard/R8 para el build de release.

# --- kotlinx.serialization ---
# Mantener los serializadores generados (R8 los elimina si no se referencian directamente).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.controlfinanciero.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.controlfinanciero.**$$serializer { *; }
-keep @kotlinx.serialization.Serializable class com.controlfinanciero.** { *; }

# --- Retrofit / OkHttp ---
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
