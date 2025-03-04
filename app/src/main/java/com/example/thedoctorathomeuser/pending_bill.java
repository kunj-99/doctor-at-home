package com.example.thedoctorathomeuser;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;
import org.json.JSONObject;

public class pending_bill extends AppCompatActivity implements PaymentResultListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_bill);

        // Initialize Razorpay
        Checkout.preload(getApplicationContext());

        Button payButton = findViewById(R.id.pay_button);
        payButton.setOnClickListener(v -> startPayment());
    }

    private void startPayment() {
        Checkout checkout = new Checkout();
        checkout.setKeyID("rzp_test_GDMFMRAC3bnneR"); // Replace with your Razorpay Key ID

        try {
            JSONObject options = new JSONObject();
            options.put("name", "Doctor at Home");
            options.put("description", "Consultation Fee");
            options.put("currency", "INR");
            options.put("amount", 54000); // Amount in paisa (₹540 * 100)
            options.put("prefill.email", "user@example.com");
            options.put("prefill.contact", "9876543210");

            checkout.open(this, options);
        } catch (Exception e) {
            Toast.makeText(this, "Error in payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        Toast.makeText(this, "Payment Successful: " + razorpayPaymentID, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPaymentError(int code, String response) {
        Toast.makeText(this, "Payment Failed: " + response, Toast.LENGTH_SHORT).show();
    }
}
