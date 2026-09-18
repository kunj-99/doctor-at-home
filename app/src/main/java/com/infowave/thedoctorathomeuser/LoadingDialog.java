package com.infowave.thedoctorathomeuser;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;

/**
 * Doctor at Home branded animated loading dialog.
 *
 * Lightweight native animation only: no GIF, Lottie or third-party loader library.
 * Existing loaderutil calls delegate here, while new code may use LoadingDialog directly.
 *
 * Last Updated: 2026-09-18 15:59 IST
 */
public final class LoadingDialog {

    private static final long ICON_CHANGE_INTERVAL_MS = 850L;
    private static final long ICON_OUT_DURATION_MS = 210L;
    private static final long ICON_IN_DURATION_MS = 320L;
    private static final long PULSE_DURATION_MS = 1150L;
    private static final long ENTRANCE_DURATION_MS = 210L;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final int[] LOADER_ICONS = {
            R.drawable.ic_loader_stethoscope,
            R.drawable.ic_loader_medical_cross,
            R.drawable.ic_loader_paw,
            R.drawable.ic_loader_syringe,
            R.drawable.ic_loader_home_heart
    };

    private static WeakReference<Dialog> dialogRef = new WeakReference<>(null);
    private static WeakReference<Activity> activityRef = new WeakReference<>(null);
    private static WeakReference<View> cardRef = new WeakReference<>(null);
    private static WeakReference<View> pulseRef = new WeakReference<>(null);
    private static WeakReference<ImageView> iconRef = new WeakReference<>(null);
    private static WeakReference<ProgressBar> progressRef = new WeakReference<>(null);
    private static WeakReference<View> actionRowRef = new WeakReference<>(null);
    private static WeakReference<Button> retryButtonRef = new WeakReference<>(null);
    private static WeakReference<Button> closeButtonRef = new WeakReference<>(null);
    private static WeakReference<View> hostDecorRef = new WeakReference<>(null);

    private static AnimatorSet pulseAnimator;
    private static AnimatorSet iconAnimator;
    private static AnimatorSet entranceAnimator;
    private static int currentIconIndex = 0;
    private static boolean iconTransitionRunning = false;
    private static boolean errorState = false;

    private static final Runnable iconChangeRunnable = new Runnable() {
        @Override
        public void run() {
            Dialog dialog = dialogRef.get();
            Activity activity = activityRef.get();
            ImageView icon = iconRef.get();

            if (dialog == null || !dialog.isShowing() || !isActivityAlive(activity) || errorState) {
                return;
            }

            if (!iconTransitionRunning && icon != null) {
                animateToNextIcon(icon);
            }

            MAIN.postDelayed(this, ICON_CHANGE_INTERVAL_MS);
        }
    };

    private static final View.OnAttachStateChangeListener HOST_DETACH_LISTENER =
            new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    // Nothing to do.
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    Activity activity = activityRef.get();
                    if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                        hideLoading();
                    }
                }
            };

    private LoadingDialog() {
    }

    /**
     * Recommended public API for new code.
     */
    public static void showLoading(Activity activity, String message) {
        showLoading(activity, "Getting things ready", message);
    }

    /**
     * Optional overload used by the existing loader wrapper when a specific action title is useful.
     */
    public static void showLoading(Activity activity, String title, String message) {
        if (!isActivityAlive(activity)) return;

        final String safeTitle = safe(title, "Getting things ready");
        final String safeMessage = safe(message, "Loading data…");

        MAIN.post(() -> showInternal(activity, safeTitle, safeMessage));
    }

    private static void showInternal(Activity activity, String title, String message) {
        if (!isActivityAlive(activity)) return;

        Dialog existing = dialogRef.get();
        Activity previousActivity = activityRef.get();

        if (existing != null && existing.isShowing()) {
            if (previousActivity == activity) {
                if (errorState) {
                    setLoadingStateInternal(title, message);
                } else {
                    updateTextInternal(title, message);
                }
                return;
            }
            dismissAndCleanup(existing);
        }

        try {
            View root = LayoutInflater.from(activity)
                    .inflate(R.layout.dialog_custom_loader, null, false);

            View card = root.findViewById(R.id.loader_card);
            View pulse = root.findViewById(R.id.loader_pulse_circle);
            ImageView icon = root.findViewById(R.id.iv_loader_icon);
            ProgressBar progress = root.findViewById(R.id.progress_loader_ring);
            View actionRow = root.findViewById(R.id.loader_action_row);
            Button retry = root.findViewById(R.id.btn_loader_retry);
            Button close = root.findViewById(R.id.btn_loader_close);

            setText(root, R.id.tv_loader_title, title);
            setText(root, R.id.tv_loader_message, message);
            setText(root, R.id.tv_loader_label, "PLEASE WAIT");

            Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
            dialog.setContentView(root);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);

            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setDimAmount(0f);
            }

            dialog.setOnDismissListener(d -> cleanupAnimationState());
            dialog.show();

            window = dialog.getWindow();
            if (window != null) {
                window.setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT
                );
                applyNormalWindow(window);
            }

            dialogRef = new WeakReference<>(dialog);
            activityRef = new WeakReference<>(activity);
            cardRef = new WeakReference<>(card);
            pulseRef = new WeakReference<>(pulse);
            iconRef = new WeakReference<>(icon);
            progressRef = new WeakReference<>(progress);
            actionRowRef = new WeakReference<>(actionRow);
            retryButtonRef = new WeakReference<>(retry);
            closeButtonRef = new WeakReference<>(close);

            errorState = false;
            currentIconIndex = 0;
            iconTransitionRunning = false;

            if (icon != null) {
                icon.setImageResource(LOADER_ICONS[currentIconIndex]);
            }
            if (progress != null) progress.setVisibility(View.VISIBLE);
            if (pulse != null) pulse.setVisibility(View.VISIBLE);
            if (actionRow != null) actionRow.setVisibility(View.GONE);

            installHostDetachGuard(activity);
            startEntranceAnimation(card, activity);
            startPulseAnimation(pulse);
            startIconCycling();

        } catch (Throwable ignored) {
            // A loader must never crash the user's action.
            cleanupAnimationState();
        }
    }

    /**
     * Updates only the smaller status message. Running animations are not restarted.
     */
    public static void updateLoadingMessage(String message) {
        MAIN.post(() -> {
            Dialog dialog = dialogRef.get();
            if (dialog == null || !dialog.isShowing()) return;
            setText(dialog, R.id.tv_loader_message, safe(message, "Loading data…"));
        });
    }

    /**
     * Updates only the main title. Running animations are not restarted.
     */
    public static void updateLoadingTitle(String title) {
        MAIN.post(() -> {
            Dialog dialog = dialogRef.get();
            if (dialog == null || !dialog.isShowing()) return;
            setText(dialog, R.id.tv_loader_title, safe(title, "Getting things ready"));
        });
    }

    /**
     * Internal convenience used by loaderutil. Does not restart animations.
     */
    public static void updateLoadingText(String title, String message) {
        MAIN.post(() -> updateTextInternal(title, message));
    }

    /**
     * Switches this same component to a recoverable connection/error state.
     * Retry returns the same dialog to loading state before invoking the supplied action.
     */
    public static void showError(
            Activity activity,
            String title,
            String message,
            Runnable onRetry,
            Runnable onClose
    ) {
        if (!isActivityAlive(activity)) return;

        MAIN.post(() -> {
            Dialog dialog = dialogRef.get();
            Activity currentActivity = activityRef.get();

            if (dialog == null || !dialog.isShowing() || currentActivity != activity) {
                showInternal(activity,
                        safe(title, "Connection problem"),
                        safe(message, "Please check your internet connection and try again."));
                dialog = dialogRef.get();
            }

            if (dialog == null || !dialog.isShowing()) return;
            setErrorStateInternal(
                    safe(title, "Connection problem"),
                    safe(message, "Please check your internet connection and try again."),
                    onRetry,
                    onClose
            );
        });
    }

    /**
     * Returns an existing error dialog to its normal loading state without recreating it.
     */
    public static void setLoadingState(String title, String message) {
        MAIN.post(() -> setLoadingStateInternal(
                safe(title, "Getting things ready"),
                safe(message, "Loading data…")
        ));
    }

    public static void hideLoading() {
        MAIN.post(() -> {
            Dialog dialog = dialogRef.get();
            if (dialog != null) {
                dismissAndCleanup(dialog);
            } else {
                cleanupAnimationState();
            }
        });
    }

    public static boolean isShowing() {
        Dialog dialog = dialogRef.get();
        Activity activity = activityRef.get();
        return dialog != null && dialog.isShowing() && isActivityAlive(activity);
    }

    private static void setErrorStateInternal(
            String title,
            String message,
            Runnable onRetry,
            Runnable onClose
    ) {
        Dialog dialog = dialogRef.get();
        if (dialog == null || !dialog.isShowing()) return;

        errorState = true;
        stopLoadingAnimations();

        ProgressBar progress = progressRef.get();
        View pulse = pulseRef.get();
        ImageView icon = iconRef.get();
        View actionRow = actionRowRef.get();
        Button retry = retryButtonRef.get();
        Button close = closeButtonRef.get();

        if (progress != null) progress.setVisibility(View.INVISIBLE);
        if (pulse != null) {
            pulse.animate().cancel();
            pulse.setScaleX(1f);
            pulse.setScaleY(1f);
            pulse.setAlpha(1f);
            pulse.setVisibility(View.VISIBLE);
        }
        if (icon != null) {
            icon.animate().cancel();
            icon.setScaleX(1f);
            icon.setScaleY(1f);
            icon.setAlpha(1f);
            icon.setRotation(0f);
            icon.setImageResource(R.drawable.ic_loader_offline);
        }

        setText(dialog, R.id.tv_loader_label, "CONNECTION ISSUE");
        setText(dialog, R.id.tv_loader_title, title);
        setText(dialog, R.id.tv_loader_message, message);

        if (actionRow != null) actionRow.setVisibility(View.VISIBLE);

        if (retry != null) {
            retry.setOnClickListener(v -> {
                Activity activity = activityRef.get();
                if (!isActivityAlive(activity)) {
                    hideLoading();
                    return;
                }
                setLoadingStateInternal("Trying again", "Checking the connection…");
                if (onRetry != null) onRetry.run();
            });
        }

        if (close != null) {
            close.setOnClickListener(v -> {
                hideLoading();
                if (onClose != null) onClose.run();
            });
        }

        Window window = dialog.getWindow();
        if (window != null) applyErrorWindow(window);
    }

    private static void setLoadingStateInternal(String title, String message) {
        Dialog dialog = dialogRef.get();
        Activity activity = activityRef.get();
        if (dialog == null || !dialog.isShowing() || !isActivityAlive(activity)) return;

        errorState = false;

        setText(dialog, R.id.tv_loader_label, "PLEASE WAIT");
        setText(dialog, R.id.tv_loader_title, title);
        setText(dialog, R.id.tv_loader_message, message);

        View actionRow = actionRowRef.get();
        Button retry = retryButtonRef.get();
        Button close = closeButtonRef.get();
        ProgressBar progress = progressRef.get();
        View pulse = pulseRef.get();
        ImageView icon = iconRef.get();

        if (actionRow != null) actionRow.setVisibility(View.GONE);
        if (retry != null) retry.setOnClickListener(null);
        if (close != null) close.setOnClickListener(null);
        if (progress != null) progress.setVisibility(View.VISIBLE);
        if (pulse != null) pulse.setVisibility(View.VISIBLE);

        currentIconIndex = Math.max(0, Math.min(currentIconIndex, LOADER_ICONS.length - 1));
        if (icon != null) {
            icon.setImageResource(LOADER_ICONS[currentIconIndex]);
            icon.setScaleX(1f);
            icon.setScaleY(1f);
            icon.setAlpha(1f);
            icon.setRotation(0f);
        }

        Window window = dialog.getWindow();
        if (window != null) applyNormalWindow(window);

        startPulseAnimation(pulse);
        startIconCycling();
    }

    private static void updateTextInternal(String title, String message) {
        Dialog dialog = dialogRef.get();
        if (dialog == null || !dialog.isShowing()) return;
        if (title != null && !title.trim().isEmpty()) {
            setText(dialog, R.id.tv_loader_title, title.trim());
        }
        if (message != null && !message.trim().isEmpty()) {
            setText(dialog, R.id.tv_loader_message, message.trim());
        }
    }

    private static void startPulseAnimation(View pulse) {
        if (pulse == null || errorState) return;

        if (pulseAnimator != null) pulseAnimator.cancel();

        pulse.setScaleX(0.94f);
        pulse.setScaleY(0.94f);
        pulse.setAlpha(0.68f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(
                pulse, View.SCALE_X, 0.94f, 1.08f, 0.94f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(
                pulse, View.SCALE_Y, 0.94f, 1.08f, 0.94f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(
                pulse, View.ALPHA, 0.68f, 1.0f, 0.68f);

        scaleX.setDuration(PULSE_DURATION_MS);
        scaleY.setDuration(PULSE_DURATION_MS);
        alpha.setDuration(PULSE_DURATION_MS);

        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);

        scaleX.setRepeatMode(ObjectAnimator.RESTART);
        scaleY.setRepeatMode(ObjectAnimator.RESTART);
        alpha.setRepeatMode(ObjectAnimator.RESTART);

        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
        scaleX.setInterpolator(interpolator);
        scaleY.setInterpolator(interpolator);
        alpha.setInterpolator(interpolator);

        pulseAnimator = new AnimatorSet();
        pulseAnimator.playTogether(scaleX, scaleY, alpha);
        pulseAnimator.start();
    }

    private static void startIconCycling() {
        MAIN.removeCallbacks(iconChangeRunnable);
        if (errorState) return;
        MAIN.postDelayed(iconChangeRunnable, ICON_CHANGE_INTERVAL_MS);
    }

    private static void animateToNextIcon(ImageView icon) {
        if (icon == null || errorState || iconTransitionRunning) return;

        iconTransitionRunning = true;
        if (iconAnimator != null) iconAnimator.cancel();

        ObjectAnimator outScaleX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 0.30f);
        ObjectAnimator outScaleY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 0.30f);
        ObjectAnimator outAlpha = ObjectAnimator.ofFloat(icon, View.ALPHA, 1f, 0f);
        ObjectAnimator outRotation = ObjectAnimator.ofFloat(icon, View.ROTATION, 0f, 90f);

        AnimatorSet out = new AnimatorSet();
        out.playTogether(outScaleX, outScaleY, outAlpha, outRotation);
        out.setDuration(ICON_OUT_DURATION_MS);
        out.setInterpolator(new AccelerateDecelerateInterpolator());

        out.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (errorState) {
                    iconTransitionRunning = false;
                    return;
                }

                currentIconIndex = (currentIconIndex + 1) % LOADER_ICONS.length;
                icon.setImageResource(LOADER_ICONS[currentIconIndex]);
                icon.setScaleX(0.30f);
                icon.setScaleY(0.30f);
                icon.setAlpha(0f);
                icon.setRotation(-90f);

                ObjectAnimator inScaleX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 0.30f, 1f);
                ObjectAnimator inScaleY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 0.30f, 1f);
                ObjectAnimator inAlpha = ObjectAnimator.ofFloat(icon, View.ALPHA, 0f, 1f);
                ObjectAnimator inRotation = ObjectAnimator.ofFloat(icon, View.ROTATION, -90f, 0f);

                AnimatorSet in = new AnimatorSet();
                in.playTogether(inScaleX, inScaleY, inAlpha, inRotation);
                in.setDuration(ICON_IN_DURATION_MS);
                in.setInterpolator(new OvershootInterpolator(1.15f));
                in.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        iconTransitionRunning = false;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        iconTransitionRunning = false;
                    }
                });

                iconAnimator = in;
                in.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                iconTransitionRunning = false;
            }
        });

        iconAnimator = out;
        out.start();
    }

    private static void startEntranceAnimation(View card, Activity activity) {
        if (card == null || activity == null) return;
        if (entranceAnimator != null) entranceAnimator.cancel();

        float translationStart = dp(activity, 10f);

        card.setAlpha(0f);
        card.setScaleX(0.95f);
        card.setScaleY(0.95f);
        card.setTranslationY(translationStart);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(card, View.ALPHA, 0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(card, View.SCALE_X, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(card, View.SCALE_Y, 0.95f, 1f);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(
                card, View.TRANSLATION_Y, translationStart, 0f);

        entranceAnimator = new AnimatorSet();
        entranceAnimator.playTogether(alpha, scaleX, scaleY, translationY);
        entranceAnimator.setDuration(ENTRANCE_DURATION_MS);
        entranceAnimator.setInterpolator(new OvershootInterpolator(0.9f));
        entranceAnimator.start();
    }

    private static void stopLoadingAnimations() {
        MAIN.removeCallbacks(iconChangeRunnable);
        iconTransitionRunning = false;

        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        if (iconAnimator != null) {
            iconAnimator.cancel();
            iconAnimator = null;
        }
    }

    private static void dismissAndCleanup(Dialog dialog) {
        cleanupAnimationState();
        try {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
        } catch (Throwable ignored) {
        }
    }

    private static void cleanupAnimationState() {
        MAIN.removeCallbacks(iconChangeRunnable);

        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        if (iconAnimator != null) {
            iconAnimator.cancel();
            iconAnimator = null;
        }
        if (entranceAnimator != null) {
            entranceAnimator.cancel();
            entranceAnimator = null;
        }

        Button retry = retryButtonRef.get();
        Button close = closeButtonRef.get();
        if (retry != null) retry.setOnClickListener(null);
        if (close != null) close.setOnClickListener(null);

        removeHostDetachGuard();

        iconTransitionRunning = false;
        errorState = false;
        currentIconIndex = 0;

        dialogRef.clear();
        activityRef.clear();
        cardRef.clear();
        pulseRef.clear();
        iconRef.clear();
        progressRef.clear();
        actionRowRef.clear();
        retryButtonRef.clear();
        closeButtonRef.clear();
    }

    private static void installHostDetachGuard(Activity activity) {
        removeHostDetachGuard();
        try {
            View decor = activity.getWindow().getDecorView();
            decor.addOnAttachStateChangeListener(HOST_DETACH_LISTENER);
            hostDecorRef = new WeakReference<>(decor);
        } catch (Throwable ignored) {
        }
    }

    private static void removeHostDetachGuard() {
        View decor = hostDecorRef.get();
        if (decor != null) {
            try {
                decor.removeOnAttachStateChangeListener(HOST_DETACH_LISTENER);
            } catch (Throwable ignored) {
            }
        }
        hostDecorRef.clear();
    }

    private static void applyNormalWindow(Window window) {
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0f;
            window.setAttributes(params);
        } catch (Throwable ignored) {
        }
    }

    private static void applyErrorWindow(Window window) {
        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.40f;
            window.setAttributes(params);
        } catch (Throwable ignored) {
        }
    }

    private static boolean isActivityAlive(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    private static void setText(Dialog dialog, int id, String value) {
        if (dialog == null) return;
        View view = dialog.findViewById(id);
        if (view instanceof TextView) ((TextView) view).setText(value);
    }

    private static void setText(View root, int id, String value) {
        if (root == null) return;
        View view = root.findViewById(id);
        if (view instanceof TextView) ((TextView) view).setText(value);
    }

    private static float dp(Activity activity, float value) {
        return value * activity.getResources().getDisplayMetrics().density;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
