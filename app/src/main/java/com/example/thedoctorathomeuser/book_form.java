package com.example.thedoctorathomeuser;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class book_form extends AppCompatActivity {

    Button book;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_form);

        book = findViewById(R.id.book_button);

        // Fetch data from Intent
        Intent intent = getIntent();
        String doctorId = intent.getStringExtra("doctor_id");
        String doctorName = intent.getStringExtra("doctor_name");


        // Button click event
        book.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(book_form.this, pending_bill.class);
                startActivity(intent);
            }
        });
    }
}
