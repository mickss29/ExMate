package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminUserFinanceReportActivity extends AppCompatActivity
        implements AdminUserSelectAdapter.OnUserClickListener {

    private RecyclerView recyclerView;
    private AdminUserSelectAdapter adapter;
    private List<AdminUserModel> userList;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_finance_report);

        recyclerView = findViewById(R.id.recyclerUsersFinance);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adapter = new AdminUserSelectAdapter(userList, this);
        recyclerView.setAdapter(adapter);

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        loadUsers();
    }

    private void loadUsers() {

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                userList.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {

                    String uid = snap.getKey();
                    String name = snap.child("name").getValue(String.class);
                    String email = snap.child("email").getValue(String.class);
                    String phone = snap.child("phone").getValue(String.class);

                    userList.add(
                            new AdminUserModel(
                                    uid,
                                    safe(name),
                                    safe(email),
                                    safe(phone),
                                    false
                            )
                    );
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        AdminUserFinanceReportActivity.this,
                        "Failed: " + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private String safe(String v) {
        return v == null || v.trim().isEmpty() ? "-" : v;
    }

    // 👉 USER CLICK
    @Override
    public void onUserClick(AdminUserModel user) {

        Intent intent = new Intent(
                this,
                AdminUserFullFinanceReportActivity.class
        );

        intent.putExtra("uid", user.getUid());
        intent.putExtra("name", user.getName());
        intent.putExtra("email", user.getEmail());
        intent.putExtra("phone", user.getPhone());

        startActivity(intent);
    }
}
