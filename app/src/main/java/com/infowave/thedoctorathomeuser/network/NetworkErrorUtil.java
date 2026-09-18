package com.infowave.thedoctorathomeuser.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

/**
 * Shared user-facing network state/error helper.
 * Keeps weak/slow-network wording consistent across the app without coupling UI to Volley internals.
 * Last Updated: 2026-09-18 14:00 IST
 */
public final class NetworkErrorUtil {

    private NetworkErrorUtil() { }

    public static boolean isConnected(Context context) {
        if (context == null) return false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network active = cm.getActiveNetwork();
                if (active == null) return false;
                NetworkCapabilities caps = cm.getNetworkCapabilities(active);
                return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }

            @SuppressWarnings("deprecation")
            android.net.NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String userMessage(Context context, VolleyError error) {
        return userMessage(context, error, "Could not connect right now. Please try again.");
    }

    public static String userMessage(Context context, VolleyError error, String fallback) {
        if (!isConnected(context)) {
            return "No internet connection. Check your network and try again.";
        }
        if (error instanceof TimeoutError) {
            return "The network is slow and the request timed out. Please try again.";
        }
        if (error instanceof NoConnectionError || error instanceof NetworkError) {
            return "Network connection is unstable. Please try again.";
        }
        if (error instanceof AuthFailureError) {
            return "Your session could not be verified. Please try again.";
        }
        if (error instanceof ServerError) {
            int code = statusCode(error);
            if (code >= 500) return "The server is temporarily busy. Please try again shortly.";
        }
        if (error instanceof ParseError) {
            return "We received an unexpected response. Please try again.";
        }
        return (fallback == null || fallback.trim().isEmpty())
                ? "Could not connect right now. Please try again."
                : fallback;
    }

    public static int statusCode(VolleyError error) {
        return error != null && error.networkResponse != null ? error.networkResponse.statusCode : -1;
    }

    public static String slowRequestMessage(Context context) {
        if (!isConnected(context)) {
            return "No internet connection. Check your network; the request will stop safely if it cannot continue.";
        }
        return "Your network is taking longer than usual. Still working — please avoid tapping the action again.";
    }
}
