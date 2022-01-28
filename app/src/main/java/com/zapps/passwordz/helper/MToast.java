package com.zapps.passwordz.helper;

import android.content.Context;

import com.shashank.sony.fancytoastlib.FancyToast;

/*
 * @author: Md Zahid Quraishi
 * @date: 25/08/2021
 * @desc: A library specialized to handle toast messages when a form is filled eg, login, registration etc.
 *        requires 'io.github.shashank02051997:FancyToast:2.0.1'
 * */

public class MToast {
    private int success, info, warning, error;
    private boolean stopAfterFirst;
    private final Context context;

    public MToast(Context context) {
        this.success = 0;
        this.info = 0;
        this.warning = 0;
        this.error = 0;
        this.context = context;
        this.stopAfterFirst = false;
    }

    // returns total number of toasts displayed
    public int displayCount() {
        return success + info + warning + error;
    }

    // displays error message
    public void error(String message) {
        if (stopAfterFirst && displayCount() == 1) return;
        error += 1;
        FancyToast.makeText(context, message, FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show();
    }

    // displays warning message
    public void warn(String message) {
        if (stopAfterFirst && displayCount() == 1) return;
        warning += 1;
        FancyToast.makeText(context, message, FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
    }

    // displays informative message
    public void info(String message) {
        if (stopAfterFirst && displayCount() == 1) return;
        info += 1;
        FancyToast.makeText(context, message, FancyToast.LENGTH_SHORT, FancyToast.INFO, false).show();
    }

    // displays success message
    public void success(String message) {
        if (stopAfterFirst && displayCount() == 1) return;
        success += 1;
        FancyToast.makeText(context, message, FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
    }

    /* when enabled, displays a single message then ignore any further display requests until disabled.
     * changing its value causes displayCount to reset
     * */
    public void stopAfterFirst(boolean stopAfterFirst) {
        this.stopAfterFirst = stopAfterFirst;
        resetCount();
    }

    // resets count of all displayed messages and returns Count object
    public Count resetCount() {
        Count count = getCount();
        this.success = 0;
        this.info = 0;
        this.warning = 0;
        this.error = 0;
        return count;
    }

    // returns Count object without resetting displayed messages count
    public Count getCount() {
        return new Count(success, info, warning, error);
    }

    public static final class Count {
        public final int successCount, infoCount, warningCount, errorCount;

        private Count(int success, int info, int warning, int error) {
            this.successCount = success;
            this.infoCount = info;
            this.warningCount = warning;
            this.errorCount = error;
        }

        public int sum() {
            return successCount + infoCount + warningCount + errorCount;
        }
    }

}
