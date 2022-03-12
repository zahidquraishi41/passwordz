package com.zapps.passwordz.helper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {

    public static boolean requiresPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            return !Environment.isExternalStorageManager();
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED;
    }

    private static void takePermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivityForResult(intent, 101);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(intent, 101);
            }
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    101);
        }
    }

    public static void requestPermissions(Activity activity) {
        if (!requiresPermission(activity)) return;
        requestDialog(activity);
    }

    public static void requestDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Permission Required!")
                .setMessage("Due to Android 11 storage restrictions, all files permission is required for app to work.")
                .setPositiveButton("Allow", (dialogInterface, i) -> takePermissions(activity))
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

}
