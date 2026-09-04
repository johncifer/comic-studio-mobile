package com.comicstudio;

import android.app.Activity;
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

    /** 返回建议的默认漫画文件夹（用户把文件夹拖进手机后放这里） */
    @JavascriptInterface
    public String defaultFolder() {
        return "/storage/emulated/0/漫画镜头工坊";
    }

    /** 列出某文件夹内容，返回 JSON 数组 */
    @JavascriptInterface
    public String listFolder(String path) {
        JSONArray arr = new JSONArray();
        try {
            File dir = new File(path);
            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdirs(); // 默认目录不存在则创建
            }
            File[] files = dir.listFiles();
            if (files != null) {
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
            }
        } catch (Exception ignored) { }
        return arr.toString();
    }

    @JavascriptInterface
    public boolean fileExists(String path) {
        return new File(path).exists();
    }

    @JavascriptInterface
    public String readText(String path) {
        try {
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

    /** 调起系统安装器安装指定 APK（需 REQUEST_INSTALL_PACKAGES 已授权） */
    @JavascriptInterface
    public void installApk(String path) {
        activity.runOnUiThread(() -> ApkInstaller.install(activity, path));
    }

    @JavascriptInterface
    public void toast(String msg) {
        activity.runOnUiThread(() -> Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show());
    }
}
