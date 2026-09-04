package com.comicstudio;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 前端（assets/www）与原生之间的桥。
 * 所有方法通过 @JavascriptInterface 暴露给 window.ComicBridge。
 */
public class ComicBridge {
    private final Activity activity;

    public ComicBridge(Activity a) {
        this.activity = a;
    }

    /** 解码 JS 传来的路径：前端把路径用 base64（纯 ASCII）编码后跨桥传，这里解码还原为 UTF-8 字符串。base64 不受 WebView 桥字符集误读影响，彻底规避中文路径乱码 */
    private String dec(String p) {
        if (p == null) return p;
        try {
            byte[] b = android.util.Base64.decode(p, android.util.Base64.DEFAULT);
            return new String(b, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) { return p; }
    }

    /** 返回建议的默认漫画文件夹（用户把文件夹拖进手机后放这里） */
    @JavascriptInterface
    public String defaultFolder() {
        return "/storage/emulated/0/漫画镜头工坊";
    }

    /** 列出某文件夹内容，返回 JSON 数组；无读取权限时返回字符串 "__NOACCESS__" 以便前端区分 */
    @JavascriptInterface
    public String listFolder(String path) {
        JSONArray arr = new JSONArray();
        try {
            path = dec(path);
            File dir = new File(path);
            if (!dir.exists()) {
                if (path.equals(defaultFolder())) dir.mkdirs(); // 仅默认目录自动创建，避免乱码路径污染
                else return "__NOACCESS__";
            }
            if (!dir.isDirectory()) return "__NOACCESS__";
            if (!dir.canRead()) return "__NOACCESS__"; // 未授予「所有文件访问」时 readable=false
            File[] files = dir.listFiles();
            if (files == null) return "__NOACCESS__"; // 仍读不出（如权限/IO 异常）
            List<File> list = new ArrayList<>(Arrays.asList(files));
            list.sort((a, b) -> {
                if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            String[] imgExt = {".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp"};
            for (File f : list) {
                JSONObject o = new JSONObject();
                o.put("name", f.getName());
                o.put("isDir", f.isDirectory());
                o.put("size", f.length());
                o.put("path", f.getAbsolutePath());
                String lower = f.getName().toLowerCase();
                boolean img = false, apk = false;
                if (!f.isDirectory()) {
                    for (String e : imgExt) {
                        if (lower.endsWith(e)) { img = true; break; }
                    }
                    if (lower.endsWith(".apk")) apk = true;
                }
                o.put("isImage", img);
                o.put("isApk", apk);
                arr.put(o);
            }
        } catch (Exception ignored) { }
        return arr.toString();
    }

    @JavascriptInterface
    public boolean fileExists(String path) {
        return new File(dec(path)).exists();
    }

    @JavascriptInterface
    public String readText(String path) {
        try {
            path = dec(path);
            File f = new File(path);
            FileInputStream fis = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) > 0) bos.write(buf, 0, n);
            fis.close();
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public boolean saveText(String path, String content) {
        try {
            path = dec(path);
            File f = new File(path);
            File parent = f.getParentFile();
            if (parent != null) parent.mkdirs();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 写入 base64 图片（含 data: 前缀时自动剥离），用于保存标注后的漫画 */
    @JavascriptInterface
    public boolean saveBase64Image(String path, String base64) {
        try {
            path = dec(path);
            int comma = base64.indexOf(",");
            String b64 = comma >= 0 ? base64.substring(comma + 1) : base64;
            byte[] data = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
            File out = new File(path);
            File parent = out.getParentFile();
            if (parent != null) parent.mkdirs();
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(data);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 读取图片并以 data URI 返回（可选等比缩放到 maxSide，0=原图）。
     * 用于 WebView 中可靠显示手机图片，规避 file:// 跨源访问限制导致的「看不到图片」问题。
     */
    @JavascriptInterface
    public String readImageBase64(String path, int maxSide) {
        try {
            path = dec(path);
            File f = new File(path);
            if (!f.exists() || f.isDirectory()) return "";
            Bitmap bm = BitmapFactory.decodeFile(path);
            if (bm == null) return "";
            if (maxSide > 0) {
                int w = bm.getWidth(), h = bm.getHeight();
                int m = Math.max(w, h);
                if (m > maxSide) {
                    float s = (float) maxSide / m;
                    Bitmap sc = Bitmap.createScaledBitmap(bm, Math.max(1, Math.round(w * s)), Math.max(1, Math.round(h * s)), true);
                    bm.recycle(); bm = sc;
                }
            }
            String lower = f.getName().toLowerCase();
            Bitmap.CompressFormat fmt = Bitmap.CompressFormat.PNG;
            String mime = "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) { fmt = Bitmap.CompressFormat.JPEG; mime = "image/jpeg"; }
            else if (lower.endsWith(".webp")) { fmt = Bitmap.CompressFormat.WEBP; mime = "image/webp"; }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bm.compress(fmt, 85, bos);
            bm.recycle();
            String b64 = android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP);
            return "data:" + mime + ";base64," + b64;
        } catch (Exception e) {
            return "";
        }
    }

    /** 调起系统安装器安装指定 APK（需 REQUEST_INSTALL_PACKAGES 已授权） */
    @JavascriptInterface
    public void installApk(String path) {
        final String p = dec(path);
        activity.runOnUiThread(() -> ApkInstaller.install(activity, p));
    }

    @JavascriptInterface
    public void toast(String msg) {
        activity.runOnUiThread(() -> Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show());
    }
}
