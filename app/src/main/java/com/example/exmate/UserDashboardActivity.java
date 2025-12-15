package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserDashboardActivity extends AppCompatActivity {

    TextView tvBalance;
    Button btnAddIncome;

    DatabaseReference incomeRef;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        tvBalance = findViewById(R.id.tvBalance);
        btnAddIncome = findViewById(R.id.btnAddIncome);

        userId = FirebaseAuth.getInstance().getUid();

        incomeRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(userId)
                .child("incomes");

        btnAddIncome.setOnClickListener(v ->
                startActivity(new Intent(this, AddIncomeActivity.class))
        );

        loadTotalIncome();
    }

    private void loadTotalIncome() {
        incomeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int total = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Integer amt = ds.child("amount").getValue(Integer.class);
                    if (amt != null) total += amt;
                }

                tvBalance.setText("₹ " + total);
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }
}
