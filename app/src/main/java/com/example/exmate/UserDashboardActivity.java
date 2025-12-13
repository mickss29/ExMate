package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvIncome, tvExpense, tvBalance;
    private Button btnAddIncome, btnAddExpense;

    private FirebaseAuth auth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        // UI
        tvWelcome = findViewById(R.id.tvWelcome);
        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);
        tvBalance = findViewById(R.id.tvBalance);
        btnAddIncome = findViewById(R.id.btnAddIncome);
        btnAddExpense = findViewById(R.id.btnAddExpense);

        // Firebase
        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUserData(user.getUid());

        // Buttons
        btnAddIncome.setOnClickListener(v ->
                startActivity(new Intent(this, AddIncomeActivity.class)));

        btnAddExpense.setOnClickListener(v ->
                startActivity(new Intent(this, AddExpenseActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            loadUserData(user.getUid()); // refresh after add income/expense
        }
    }

    private void loadUserData(String uid) {

        usersRef.child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        // Welcome name
                        String name = snapshot.child("name").getValue(String.class);
                        tvWelcome.setText("Welcome, " + (name != null ? name : "User"));

                        double totalIncome = 0.0;
                        double totalExpense = 0.0;

                        // Income
                        if (snapshot.child("income").exists()) {
                            for (DataSnapshot incomeSnap :
                                    snapshot.child("income").getChildren()) {
                                Double amt = incomeSnap.child("amount").getValue(Double.class);
                                if (amt != null) totalIncome += amt;
                            }
                        }

                        // Expense
                        if (snapshot.child("expenses").exists()) {
                            for (DataSnapshot expSnap :
                                    snapshot.child("expenses").getChildren()) {
                                Double amt = expSnap.child("amount").getValue(Double.class);
                                if (amt != null) totalExpense += amt;
                            }
                        }

                        double balance = totalIncome - totalExpense;

                        tvIncome.setText("₹ " + String.format("%.2f", totalIncome));
                        tvExpense.setText("₹ " + String.format("%.2f", totalExpense));
                        tvBalance.setText("₹ " + String.format("%.2f", balance));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(UserDashboardActivity.this,
                                "Failed to load data",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
