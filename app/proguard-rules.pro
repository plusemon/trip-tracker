# Room Database & Entities
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.data.entity.** { *; }
-keep class com.example.data.dao.** { *; }
-keep class com.example.data.db.** { *; }
-dontwarn androidx.room.paging.**


# Retain Retrofit, Gson, and Moshi reflection/attributes
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# Keep GitHub Release Data Models and updater classes
-keep class com.example.updater.** { *; }
-keepclassmembers class com.example.updater.** { *; }
-keep class com.example.updater.model.** { *; }
-keepclassmembers class com.example.updater.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.squareup.moshi.Json <fields>;
}

# Keep Gson and Moshi classes
-dontwarn com.google.gson.**
-keep class com.google.code.gson.** { *; }
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keep interface com.squareup.moshi.** { *; }

# Aggressive Optimization
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
