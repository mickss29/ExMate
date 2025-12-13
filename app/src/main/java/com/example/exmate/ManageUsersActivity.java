package com.example.exmate;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.usersToolbar);
        setSupportActionBar(toolbar);

        // RecyclerView
        recyclerView = findViewById(R.id.usersRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Dummy users (for now)
        List<UserModel> users = new ArrayList<>();
        users.add(new UserModel("Amit Sharma", "amit@gmail.com"));
        users.add(new UserModel("Neha Verma", "neha@gmail.com"));
        users.add(new UserModel("Rahul Singh", "rahul@gmail.com"));
        users.add(new UserModel("Pooja Patel", "pooja@gmail.com"));

        adapter = new UserAdapter(users);
        recyclerView.setAdapter(adapter);
    }
}
