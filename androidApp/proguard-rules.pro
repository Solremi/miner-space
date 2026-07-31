-keep class com.badlogic.gdx.backends.android.AndroidApplication { *; }
-keep class fr.solremi.minerspace.android.MainActivity { *; }
-keepclassmembers enum fr.solremi.minerspace.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-dontwarn com.badlogic.gdx.**
-dontwarn org.jetbrains.annotations.**
