# Add project specific ProGuard rules here.
-keep class com.seagull.msreg.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}