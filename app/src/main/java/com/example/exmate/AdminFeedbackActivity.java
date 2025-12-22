package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class AdminFeedbackActivity extends AppCompatActivity {

    private RecyclerView rvFeedback;
    private Toolbar toolbar;

    private DatabaseReference feedbackRef;
    private final List<FeedbackModel> feedbackList = new ArrayList<>();
    private FeedbackAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_feedback);

        toolbar = findViewById(R.id.toolbarFeedback);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rvFeedback = findViewById(R.id.rvFeedback);
        rvFeedback.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FeedbackAdapter(feedbackList, feedback -> {
            Intent i = new Intent(this, AdminFeedbackDetailActivity.class);
            i.putExtra("feedbackId", feedback.id);
            startActivity(i);
        });

        rvFeedback.setAdapter(adapter);

        feedbackRef = FirebaseDatabase.getInstance().getReference("feedbacks");

        loadFeedbacks();
    }

    private void loadFeedbacks() {
        feedbackRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                feedbackList.clear();

                for (DataSnapshot s : snapshot.getChildren()) {
                    FeedbackModel model = s.getValue(FeedbackModel.class);
                    if (model != null) {
                        model.id = s.getKey();
                        feedbackList.add(model);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        AdminFeedbackActivity.this,
                        "Failed to load feedback",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
