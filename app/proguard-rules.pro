# Custom view inflated by the framework.
-keep class com.everyonesniche.nightkeys.ui.LuxMeterView { public <init>(...); }

# NightKeys calls Shizuku API 13's retained legacy remote-process primitive
# reflectively because UserService callbacks did not work on the target firmware.
# Keep the method name/signature in minified release builds.
-keepclassmembers class rikka.shizuku.Shizuku {
    private static rikka.shizuku.ShizukuRemoteProcess newProcess(java.lang.String[], java.lang.String[], java.lang.String);
}
-keep class rikka.shizuku.ShizukuRemoteProcess { *; }
