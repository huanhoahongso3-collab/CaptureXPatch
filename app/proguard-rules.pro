# Xposed (modern io.github.libxposed.api entry point)
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keepattributes RuntimeVisibleAnnotations
-keep,allowobfuscation,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public <init>(...);
    public void onPackageLoaded(...);
    public void onSystemServerLoaded(...);
}
-keep,allowoptimization,allowobfuscation @io.github.libxposed.api.annotations.* class * {
    @io.github.libxposed.api.annotations.BeforeInvocation <methods>;
    @io.github.libxposed.api.annotations.AfterInvocation <methods>;
}

# Xposed (legacy de.robv.android.xposed entry point, referenced by name from assets/xposed_init).
# Unlike META-INF/xposed/java_init.list (a java-resource file R8's -adaptresourcefilecontents can
# rewrite), assets/xposed_init lives under src/main/assets and is packaged straight into the APK
# by AAPT2 without ever going through R8 — so its class-name reference can never be kept in sync
# with an obfuscated rename. The entry class name must therefore stay fixed (no allowobfuscation).
-keep,allowoptimization public class * implements de.robv.android.xposed.IXposedHookLoadPackage {
    public <init>();
    public void handleLoadPackage(...);
}

# Kotlin
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
	public static void check*(...);
	public static void throw*(...);
}
-assumenosideeffects class java.util.Objects {
    public static ** requireNonNull(...);
}

# Strip debug log
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Obfuscation
-repackageclasses
-allowaccessmodification