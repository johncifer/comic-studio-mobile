package com.comicstudio;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int REQ_ALL_FILES = 1001;
    private static final int REQ_INSTALL = 1002;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadsImagesAutomatically(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // 自定义 WebViewClient：拦截 assets/www/vendor/ 下的 wasm/onnx 资源请求，
        // 用 AssetManager 直接喂流并返回 HTTP 200 + 正确 MIME。
        // 否则 WebView 的 fetch() 对 file:///android_asset/ 常拿到 status 0 或被拒，
        // onnxruntime-web 会报 "Failed to fetch" / "no available backend found"。
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                android.net.Uri uri = request.getUrl();
                if ("file".equals(uri.getScheme())) {
                    String path = uri.getPath();
                    if (path != null && path.startsWith("/android_asset/")) {
                        String assetPath = path.substring("/android_asset/".length());
                        try {
                            InputStream is = getAssets().open(assetPath);
                            String name = uri.getLastPathSegment();
                            String mime = guessMime(name);
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Access-Control-Allow-Origin", "*");
                            return new WebResourceResponse(mime, null, 200, "OK", headers, is);
                        } catch (IOException e) {
                            return null;
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            private String guessMime(String name) {
                if (name == null) return "application/octet-stream";
                String n = name.toLowerCase();
                if (n.endsWith(".wasm")) return "application/wasm";
                if (n.endsWith(".onnx")) return "application/octet-stream";
                if (n.endsWith(".js")) return "application/javascript";
                if (n.endsWith(".html")) return "text/html";
                if (n.endsWith(".css")) return "text/css";
                if (n.endsWith(".png")) return "image/png";
                if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
                if (n.endsWith(".svg")) return "image/svg+xml";
                if (n.endsWith(".json")) return "application/json";
                if (n.endsWith(".webp")) return "image/webp";
                return "application/octet-stream";
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        // 注入 JS 桥，前端通过 window.ComicBridge 调用原生能力
        webView.addJavascriptInterface(new ComicBridge(this), "ComicBridge");

        requestPermissionsIfNeeded();

        webView.loadUrl("file:///android_asset/www/index.html");
    }

    private void requestPermissionsIfNeeded() {
        // Android 11+：引导授予「所有文件访问」
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQ_ALL_FILES);
                } catch (Exception ignored) { }
            }
        }
        // Android 8+：引导授予「安装未知应用」
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQ_INSTALL);
                } catch (Exception ignored) { }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 权限授予后通知前端重新读取（前端可定义 window.onNativePermissionResult）
        webView.loadUrl("javascript:if(window.onNativePermissionResult){window.onNativePermissionResult();}");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
