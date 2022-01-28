package com.zapps.passwordz.helper;

import android.view.View;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Arrays;

// an utility class to lock / unlock multiple views at once.
// if progressBar is provided then its visibility will be set
// to VISIBLE & GONE when locking and unlocking resp.
public class Enabler {
    private final ArrayList<View> views;
    private ProgressBar progressBar;

    public Enabler(View... views) {
        this.views = new ArrayList<>();
        this.views.addAll(Arrays.asList(views));
        this.progressBar = null;
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public void addView(View view) {
        views.add(view);
    }

    public void removeView(View view) {
        views.remove(view);
    }

    public void enableAll() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        for (View view : views) view.setEnabled(true);
    }

    public void disableAll() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        for (View view : views) view.setEnabled(false);
    }

    public void setVisibility(int visibility) {
        for (View view : views) view.setVisibility(visibility);
    }

    public void toggle() {
        if (progressBar != null) {
            if (progressBar.getVisibility() == View.VISIBLE)
                progressBar.setVisibility(View.GONE);
            else progressBar.setVisibility(View.VISIBLE);
        }
        for (View view : views) {
            view.setEnabled(!view.isEnabled());
        }
    }
}
