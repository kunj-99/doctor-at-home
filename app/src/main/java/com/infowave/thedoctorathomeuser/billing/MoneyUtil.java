package com.infowave.thedoctorathomeuser.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Display/parsing helpers for server-authoritative money values.
 * Financial arithmetic stays in integer paise; BigDecimal is used only at boundaries.
 * Last Updated: 2026-09-18 16:55 IST
 */
public final class MoneyUtil {

    private MoneyUtil() {}

    public static long parseRupeesToPaise(Object value) {
        if (value == null) return 0L;
        try {
            BigDecimal rupees = new BigDecimal(String.valueOf(value).trim())
                    .setScale(2, RoundingMode.HALF_UP);
            return rupees.movePointRight(2).longValueExact();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public static BigDecimal paiseToRupees(long paise) {
        return BigDecimal.valueOf(paise, 2);
    }

    public static double paiseToRupeesDouble(long paise) {
        return paiseToRupees(paise).doubleValue();
    }

    public static String formatPaise(long paise) {
        return "₹ " + paiseToRupees(paise).setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    public static String formatPaiseCompact(long paise) {
        BigDecimal value = paiseToRupees(paise).stripTrailingZeros();
        return "₹ " + value.toPlainString();
    }

    public static String formatDistanceMeters(long meters) {
        return String.format(Locale.getDefault(), "%.1f km", meters / 1000.0d);
    }

    public static long roundDivHalfUp(long numerator, long denominator) {
        if (denominator <= 0L) throw new IllegalArgumentException("denominator must be > 0");
        if (numerator == Long.MIN_VALUE) throw new ArithmeticException("money overflow");
        if (numerator < 0L) return -roundDivHalfUp(-numerator, denominator);
        long quotient = numerator / denominator;
        long remainder = numerator % denominator;
        long threshold = (denominator / 2L) + (denominator % 2L);
        return remainder >= threshold ? Math.addExact(quotient, 1L) : quotient;
    }
}
