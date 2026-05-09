# PaisaTracker ProGuard Rules

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# iText7
-dontwarn com.itextpdf.**
-keep class com.itextpdf.** { *; }

# opencsv
-dontwarn com.opencsv.**

# slf4j (used by iText/opencsv)
-dontwarn org.slf4j.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao
