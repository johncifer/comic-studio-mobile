# 保留 JS 桥接口，避免混淆后 @JavascriptInterface 失效
-keepclassmembers class com.comicstudio.ComicBridge {
    public *;
}
-keepattributes *Annotation*
