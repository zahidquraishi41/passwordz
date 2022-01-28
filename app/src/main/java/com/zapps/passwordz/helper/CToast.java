package com.zapps.passwordz.helper;

import android.content.Context;

import com.shashank.sony.fancytoastlib.FancyToast;

// requires the following library
// implementation 'io.github.shashank02051997:FancyToast:2.0.1'
// makes toast creation too easy
public class CToast {

    public static void error(Context context, String message) {
        FancyToast.makeText(context, message, FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show();
    }

    public static void info(Context context, String message) {
        FancyToast.makeText(context, message, FancyToast.LENGTH_SHORT, FancyToast.INFO, false).show();
    }

    public static void warn(Context context, String message) {
        FancyToast.makeText(context, message, FancyToast.LENGTH_LONG, FancyToast.WARNING, false).show();
    }

    public static void success(Context context, String message) {
        FancyToast.makeText(context, message, FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
    }


}
