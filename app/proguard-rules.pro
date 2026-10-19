# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlin Coroutines & Serialization
-keepclassmembers class * {
    @androidx.room.Entity *;
    @androidx.room.PrimaryKey *;
}

# Aggressive Optimization
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

