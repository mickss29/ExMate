package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class AdminManageOffersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;

    private List<DiscoverOfferModel> offerList = new ArrayList<>();
    private AdminOfferAdapter adapter;

    private DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_offers);

        recyclerView = findViewById(R.id.recyclerOffers);
        fabAdd = findViewById(R.id.fabAdd);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminOfferAdapter(this, offerList);
        recyclerView.setAdapter(adapter);

        ref = FirebaseDatabase.getInstance().getReference("DiscoverOffers");

        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, AddOfferActivity.class));
        });

        loadOffers();
    }

    private void loadOffers() {

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                offerList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {

                    DiscoverOfferModel model =
                            data.getValue(DiscoverOfferModel.class);

                    if (model != null) {
                        model.setId(data.getKey()); // IMPORTANT
                        offerList.add(model);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminManageOffersActivity.this,
                        error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}