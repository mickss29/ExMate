package com.example.exmate;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    AdminUserAdapter adapter;
    List<AdminUserModel> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        recyclerView = findViewById(R.id.recyclerUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();

        // TEMP admin users
        userList.add(new AdminUserModel("Rahul Sharma", "rahul@gmail.com", false));
        userList.add(new AdminUserModel("Amit Patel", "amit@gmail.com", false));
        userList.add(new AdminUserModel("Sneha Verma", "sneha@gmail.com", true));

        adapter = new AdminUserAdapter(this, userList);
        recyclerView.setAdapter(adapter);
    }
}
