# ═══════════════════════════════════════════════════════════════
# Aetherion Mobile NOC — ProGuard / R8 Production Rules
# Developer: Mohammad Abdalftah Ibrahime
# ═══════════════════════════════════════════════════════════════

# ── Keep app entry points ────────────────────────────────────
-keep class com.aetherion.noc.AetherionApp { *; }

# ── Kotlin Serialization ─────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}
-keep,includedescriptorclasses class com.aetherion.noc.**$$serializer { *; }
-keepclassmembers class com.aetherion.noc.** {
    *** Companion;
}
-keepclasseswithmembers class com.aetherion.noc.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Retrofit & OkHttp ────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep interface com.aetherion.noc.data.remote.api.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# ── Hilt / Dagger ────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel *;
    @javax.inject.Inject *;
}

# ── Room ─────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# ── Firebase ─────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Security — never log sensitive data ─────────────────────
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
    public static void i(...);
}

# ── Data Transfer Objects ────────────────────────────────────
-keep class com.aetherion.noc.data.remote.dto.** { *; }
-keep class com.aetherion.noc.domain.model.** { *; }

# ── Biometric ────────────────────────────────────────────────
-keep class androidx.biometric.** { *; }

# ── Maps ─────────────────────────────────────────────────────
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }

# ── Remove debug-only code ───────────────────────────────────
-assumenosideeffects class com.aetherion.noc.core.utils.DebugUtils { *; }

# ── General optimizations ────────────────────────────────────
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'a'
