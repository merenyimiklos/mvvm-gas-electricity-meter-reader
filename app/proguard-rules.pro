# Keep generic signatures and annotations used by libraries/reflection.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod

# Keep enum helpers safe for JSON / enum-name based persistence.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Room entities/DAOs are generated at compile time; keep database classes readable to R8.
-keep class hu.merenyimiklos.meterreader.data.db.** { *; }

# Firebase / Google Identity / Credential Manager provide consumer rules,
# but keep auth model members used across serialization/reflection boundaries.
-keep class hu.merenyimiklos.meterreader.auth.** { *; }

# ML Kit and Lottie ship consumer ProGuard rules; no blanket keep is needed.
-dontwarn org.bouncycastle.**
