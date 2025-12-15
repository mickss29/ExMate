package com.example.exmate;

import android.os.Bundle;
import android.widget.Toast;

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



import android.os.Bundle;
import android.widget.Toast;

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

public class ManageUsersActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    AdminUserAdapter adapter;
    List<AdminUserModel> userList;

    DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        recyclerView = findViewById(R.id.recyclerUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adapter = new AdminUserAdapter(this, userList);
        recyclerView.setAdapter(adapter);

        // 🔥 Firebase reference
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        loadUsersFromFirebase();
    }

    private void loadUsersFromFirebase() {

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                userList.clear();

                for (DataSnapshot userSnap : snapshot.getChildren()) {

                    String uid = userSnap.getKey();
                    String name = userSnap.child("name").getValue(String.class);
                    String email = userSnap.child("email").getValue(String.class);
                    String phone = userSnap.child("phone").getValue(String.class);
                    Boolean blocked = userSnap.child("blocked").getValue(Boolean.class);

                    if (blocked == null) blocked = false;

                    userList.add(
                            new AdminUserModel(uid, name, email, phone, blocked)
                    );
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(
                        ManageUsersActivity.this,
                        "Failed to load users: " + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }
}
