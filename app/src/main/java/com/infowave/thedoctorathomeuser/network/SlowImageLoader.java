package com.infowave.thedoctorathomeuser.network;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.infowave.thedoctorathomeuser.R;

/**
 * Slow-network friendly image loading used on RecyclerView/high-traffic screens.
 * Uses Glide's memory/disk cache, a small thumbnail first, one bounded retry and recycle-safe tags.
 * Last Updated: 2026-09-18 14:42 IST
 */
public final class SlowImageLoader {

    private static final long RETRY_DELAY_MS = 650L;

    private SlowImageLoader() { }

    public static void load(@NonNull ImageView imageView,
                            @Nullable String url,
                            @DrawableRes int placeholder,
                            @DrawableRes int error) {
        loadInternal(imageView, normalize(url), placeholder, error, 0, 0);
    }

    public static void loadCircle(@NonNull ImageView imageView,
                                  @Nullable String url,
                                  @DrawableRes int placeholder,
                                  @DrawableRes int error) {
        loadInternal(imageView, normalize(url), placeholder, error, 1, 0);
    }

    public static void loadCenterCrop(@NonNull ImageView imageView,
                                      @Nullable String url,
                                      @DrawableRes int placeholder,
                                      @DrawableRes int error) {
        loadInternal(imageView, normalize(url), placeholder, error, 2, 0);
    }

    private static void loadInternal(@NonNull ImageView imageView,
                                     @Nullable String normalizedUrl,
                                     @DrawableRes int placeholder,
                                     @DrawableRes int error,
                                     int transform,
                                     int attempt) {
        if (TextUtils.isEmpty(normalizedUrl)) {
            imageView.setTag(R.id.tag_slow_image_url, null);
            Glide.with(imageView.getContext()).clear(imageView);
            imageView.setImageResource(error != 0 ? error : placeholder);
            return;
        }

        imageView.setTag(R.id.tag_slow_image_url, normalizedUrl);

        RequestBuilder<Drawable> request = Glide.with(imageView.getContext())
                .load(normalizedUrl)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .thumbnail(0.25f)
                .dontAnimate()
                .placeholder(placeholder)
                .error(attempt == 0 && placeholder != 0 ? placeholder : error)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        if (attempt == 0 && isStillBound(imageView, normalizedUrl)) {
                            imageView.postDelayed(() -> {
                                if (isStillBound(imageView, normalizedUrl)) {
                                    loadInternal(imageView, normalizedUrl, placeholder, error, transform, 1);
                                }
                            }, RETRY_DELAY_MS);
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        return false;
                    }
                });

        if (transform == 1) request = request.circleCrop();
        else if (transform == 2) request = request.centerCrop();
        request.into(imageView);
    }

    public static void clear(@NonNull ImageView imageView) {
        imageView.setTag(R.id.tag_slow_image_url, null);
        try {
            Glide.with(imageView.getContext()).clear(imageView);
        } catch (Throwable ignored) { }
    }

    private static boolean isStillBound(ImageView imageView, String url) {
        Object tag = imageView.getTag(R.id.tag_slow_image_url);
        return tag != null && url.equals(String.valueOf(tag));
    }

    private static String normalize(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty() || "null".equalsIgnoreCase(value)) return null;
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1).trim();
        }
        return value.isEmpty() ? null : value;
    }
}
