package com.infowave.thedoctorathomeuser.billing;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Immutable Phase 4C server-authoritative booking quote.
 * No bill total is recalculated from floating-point values in Android.
 * Last Updated: 2026-09-18 16:52 IST
 */
public final class BookingQuote {

    public final String quoteToken;
    public final int pricingVersion;
    public final int calculationVersion;
    public final String calculationHash;
    public final String settlementPolicy;
    public final String depositStatus;
    public final String slotType;
    public final String vaccinationName;

    public final long distanceMeters;
    public final long baseDistanceMeters;
    public final long extraCostPerKmPaise;
    public final long gstPercentUnits;

    public final long appointmentChargePaise;
    public final long consultationFeePaise;
    public final long platformChargePaise;
    public final long gstPaise;
    public final long distanceChargePaise;
    public final long vaccinationPricePaise;
    public final long walletBalancePaise;
    public final long walletDepositPaise;
    public final long finalExactPaise;
    public final long gatewayPaise;
    public final long roundingAdjustmentPaise;

    private BookingQuote(JSONObject q) throws JSONException {
        quoteToken = requiredString(q, "quote_token");
        pricingVersion = q.getInt("pricing_version");
        calculationVersion = q.getInt("calculation_version");
        calculationHash = requiredString(q, "calculation_hash").toLowerCase();
        settlementPolicy = requiredString(q, "settlement_policy");
        depositStatus = requiredString(q, "deposit_status");
        slotType = q.optString("slot_type", "");
        vaccinationName = q.optString("vaccination_name", "");

        distanceMeters = requiredNonNegativeLong(q, "distance_meters");
        baseDistanceMeters = requiredNonNegativeLong(q, "base_distance_meters");
        extraCostPerKmPaise = requiredNonNegativeLong(q, "extra_cost_per_km_paise");
        gstPercentUnits = requiredNonNegativeLong(q, "gst_percent_units");

        appointmentChargePaise = requiredNonNegativeLong(q, "appointment_charge_paise");
        consultationFeePaise = requiredNonNegativeLong(q, "consultation_fee_paise");
        platformChargePaise = requiredNonNegativeLong(q, "platform_charge_paise");
        gstPaise = requiredNonNegativeLong(q, "gst_paise");
        distanceChargePaise = requiredNonNegativeLong(q, "distance_charge_paise");
        vaccinationPricePaise = requiredNonNegativeLong(q, "vaccination_price_paise");
        walletBalancePaise = requiredNonNegativeLong(q, "wallet_balance_paise");
        walletDepositPaise = requiredNonNegativeLong(q, "wallet_deposit_paise");
        finalExactPaise = requiredNonNegativeLong(q, "final_exact_paise");
        gatewayPaise = requiredNonNegativeLong(q, "gateway_paise");
        roundingAdjustmentPaise = requiredNonNegativeLong(q, "rounding_adjustment_paise");

        validateArithmetic();
    }

    public static BookingQuote fromJson(JSONObject quoteObject) throws JSONException {
        if (quoteObject == null) throw new JSONException("Missing quote object");
        return new BookingQuote(quoteObject);
    }

    public void validateForPaymentMethod(String paymentMethod) throws JSONException {
        if ("Online".equals(paymentMethod)) {
            if (walletDepositPaise != 0L || !"Added in Bill".equalsIgnoreCase(depositStatus)) {
                throw new JSONException("Online quote has invalid settlement mode");
            }
        } else if ("Offline".equals(paymentMethod)) {
            if (walletDepositPaise != platformChargePaise || !"Wallet Debited".equalsIgnoreCase(depositStatus)) {
                throw new JSONException("Offline quote has invalid wallet settlement mode");
            }
        } else {
            throw new JSONException("Unsupported payment method");
        }
    }

    private void validateArithmetic() throws JSONException {
        if (pricingVersion < 5 || calculationVersion != 5) {
            throw new JSONException("Phase 4C quote version required");
        }
        if (!"EXACT_PAISE_V5".equals(settlementPolicy)) {
            throw new JSONException("Unsupported settlement policy");
        }
        if (!calculationHash.matches("^[a-f0-9]{64}$")) {
            throw new JSONException("Invalid quote integrity fingerprint");
        }
        try {
            if (appointmentChargePaise != Math.addExact(consultationFeePaise, platformChargePaise)) {
                throw new JSONException("Appointment breakdown mismatch");
            }

            long expectedGst = MoneyUtil.roundDivHalfUp(
                    Math.multiplyExact(appointmentChargePaise, gstPercentUnits), 1_000_000L);
            if (expectedGst != gstPaise) {
                throw new JSONException("GST breakdown mismatch");
            }

            long expectedDistance = distanceMeters <= baseDistanceMeters
                    ? 0L
                    : MoneyUtil.roundDivHalfUp(
                            Math.multiplyExact(distanceMeters, extraCostPerKmPaise), 1000L);
            if (expectedDistance != distanceChargePaise) {
                throw new JSONException("Distance breakdown mismatch");
            }
        } catch (ArithmeticException ex) {
            throw new JSONException("Bill amount overflow");
        }

        long platformInBill;
        if ("Wallet Debited".equalsIgnoreCase(depositStatus)) {
            if (walletDepositPaise != platformChargePaise) {
                throw new JSONException("Wallet platform charge mismatch");
            }
            platformInBill = 0L;
        } else if ("Added in Bill".equalsIgnoreCase(depositStatus)) {
            if (walletDepositPaise != 0L) {
                throw new JSONException("Bill platform charge mismatch");
            }
            platformInBill = platformChargePaise;
        } else {
            throw new JSONException("Unknown platform settlement state");
        }

        long expectedFinal;
        try {
            expectedFinal = Math.addExact(consultationFeePaise, gstPaise);
            expectedFinal = Math.addExact(expectedFinal, distanceChargePaise);
            expectedFinal = Math.addExact(expectedFinal, vaccinationPricePaise);
            expectedFinal = Math.addExact(expectedFinal, platformInBill);
        } catch (ArithmeticException ex) {
            throw new JSONException("Bill amount overflow");
        }

        if (expectedFinal != finalExactPaise) {
            throw new JSONException("Final amount mismatch");
        }
        if (gatewayPaise != finalExactPaise || roundingAdjustmentPaise != 0L) {
            throw new JSONException("Gateway amount mismatch");
        }
        if (finalExactPaise <= 0L) {
            throw new JSONException("Final amount must be positive");
        }
    }

    private static String requiredString(JSONObject q, String key) throws JSONException {
        if (!q.has(key) || q.isNull(key)) throw new JSONException("Missing " + key);
        String value = q.getString(key).trim();
        if (value.isEmpty()) throw new JSONException("Empty " + key);
        return value;
    }

    private static long requiredNonNegativeLong(JSONObject q, String key) throws JSONException {
        if (!q.has(key) || q.isNull(key)) throw new JSONException("Missing " + key);
        long value = q.getLong(key);
        if (value < 0L) throw new JSONException("Negative " + key);
        return value;
    }
}
