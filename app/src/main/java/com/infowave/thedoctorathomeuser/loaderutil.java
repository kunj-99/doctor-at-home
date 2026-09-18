package com.infowave.thedoctorathomeuser;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;

import com.infowave.thedoctorathomeuser.network.NetworkErrorUtil;

/**
 * Backward-compatible facade for the app's existing loaderutil call sites.
 *
 * All rendering/animation is now handled by LoadingDialog. This keeps current Activities working
 * unchanged while new code can use LoadingDialog.showLoading(...) directly.
 *
 * Last Updated: 2026-09-18 15:59 IST
 */
public final class loaderutil {

    private static final long SLOW_MESSAGE_AFTER_MS = 2500L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Runnable slowMessageRunnable;

    private loaderutil() {
    }

    public static void showLoader(Context context) {
        showLoader(context, "Getting things ready", "Loading data…");
    }

    public static void showLoader(Context context, String title) {
        showLoader(context, title, "Loading data…");
    }

    public static void showLoader(Context context, String title, String message) {
        Activity activity = findActivity(context);
        if (!isActivityAlive(activity)) return;

        LoadingDialog.showLoading(activity,
                safe(title, "Getting things ready"),
                safe(message, "Loading data…"));
        scheduleSlowMessage(activity.getApplicationContext());
    }

    /**
     * Existing API: updates text without rebuilding or restarting the animated loader.
     */
    public static void updateMessage(String title, String message) {
        LoadingDialog.updateLoadingText(title, message);
    }

    /**
     * Optional same-component error state for screens that want Retry/Close actions.
     */
    public static void showError(
            Context context,
            String title,
            String message,
            Runnable onRetry,
            Runnable onClose
    ) {
        cancelSlowMessage();
        Activity activity = findActivity(context);
        if (!isActivityAlive(activity)) return;
        LoadingDialog.showError(activity, title, message, onRetry, onClose);
    }

    public static void hideLoader() {
        cancelSlowMessage();
        LoadingDialog.hideLoading();
    }

    public static boolean isShowing() {
        return LoadingDialog.isShowing();
    }

    private static void scheduleSlowMessage(Context appContext) {
        cancelSlowMessage();
        if (appContext == null) return;

        slowMessageRunnable = () -> {
            if (!LoadingDialog.isShowing()) return;

            if (NetworkErrorUtil.isConnected(appContext)) {
                LoadingDialog.updateLoadingText(
                        "Still working",
                        "The network is a little slow. Please keep this screen open…"
                );
            } else {
                LoadingDialog.updateLoadingText(
                        "Waiting for internet",
                        "Your connection dropped. We’ll continue when the network responds…"
                );
            }
        };
        MAIN.postDelayed(slowMessageRunnable, SLOW_MESSAGE_AFTER_MS);
    }

    private static void cancelSlowMessage() {
        if (slowMessageRunnable != null) {
            MAIN.removeCallbacks(slowMessageRunnable);
            slowMessageRunnable = null;
        }
    }

    private static Activity findActivity(Context context) {
        if (context == null) return null;

        Context cursor = context;
        while (cursor instanceof ContextWrapper) {
            if (cursor instanceof Activity) return (Activity) cursor;
            Context base = ((ContextWrapper) cursor).getBaseContext();
            if (base == cursor) break;
            cursor = base;
        }
        return cursor instanceof Activity ? (Activity) cursor : null;
    }

    private static boolean isActivityAlive(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
