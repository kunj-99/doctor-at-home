package com.infowave.thedoctorathomeuser.network;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.Volley;

/**
 * Process-wide Volley queue and shared request policies.
 *
 * One queue means one disk cache and one worker pool for the whole app. Callers that need an
 * explicit policy can use add(..., Profile); older callers can keep queue(context).add(...)
 * without creating another RequestQueue.
 * Last Updated: 2026-09-18 14:00 IST
 */
public final class VolleySingleton {

    public enum Profile {
        READ,       // safe/idempotent reads: one bounded retry
        WRITE,      // state-changing request: no automatic retry
        PAYMENT,    // money movement/order creation: never auto-repeat
        BACKGROUND  // quiet polling: short timeout, no retry
    }

    private static volatile VolleySingleton instance;
    private final RequestQueue requestQueue;

    private VolleySingleton(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static VolleySingleton getInstance(Context context) {
        if (instance == null) {
            synchronized (VolleySingleton.class) {
                if (instance == null) {
                    instance = new VolleySingleton(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /** Drop-in replacement for Volley.newRequestQueue(context). */
    public static RequestQueue queue(Context context) {
        return getInstance(context).getRequestQueue();
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public <T> Request<T> add(Request<T> request) {
        requestQueue.add(request);
        return request;
    }

    public <T> Request<T> add(Request<T> request, Object tag, Profile profile) {
        if (tag != null) request.setTag(tag);
        if (profile != null) request.setRetryPolicy(policy(profile));
        requestQueue.add(request);
        return request;
    }

    public void cancel(Object tag) {
        if (tag != null) requestQueue.cancelAll(tag);
    }

    public static RetryPolicy policy(Profile profile) {
        if (profile == null) profile = Profile.READ;
        switch (profile) {
            case WRITE:
                return new DefaultRetryPolicy(15000, 0, 1.0f);
            case PAYMENT:
                return new DefaultRetryPolicy(20000, 0, 1.0f);
            case BACKGROUND:
                return new DefaultRetryPolicy(8000, 0, 1.0f);
            case READ:
            default:
                return new DefaultRetryPolicy(12000, 1, 1.5f);
        }
    }
}
