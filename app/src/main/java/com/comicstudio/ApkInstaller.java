package com.comicstudio;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * 通过 FileProvider 暴露 APK 的 content URI，调起系统 PackageInstaller 安装。
 */
public class ApkInstaller {
    public static void install(Context context, String path) {
        File file = new File(path);
        if (!file.exists()) return;
        try {
            Uri uri = FileProvider.getUriForFile(context,
                    "com.comicstudio.mobile.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // 安装失败（如权限未授予）由系统弹窗提示，这里不崩溃
        }
    }
}
