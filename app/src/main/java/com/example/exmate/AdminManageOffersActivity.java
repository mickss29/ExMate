package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminManageOffersActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private RecyclerView recyclerView;
    private MaterialCardView fabAdd, btnBack;
    private LinearLayout emptyState;
    private TextView tvOfferCount;

    // ── Data ───────────────────────────────────────────────────────────────
    private final List<DiscoverOfferModel> offerList = new ArrayList<>();
    private AdminOfferAdapter adapter;
    private DatabaseReference ref;

    // ══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_offers);

        initViews();
        setupRecyclerView();
        setClickListeners();
        loadOffers();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Init
    // ══════════════════════════════════════════════════════════════════════

    private void initViews() {
        btnBack      = findViewById(R.id.btnBack);
        fabAdd       = findViewById(R.id.fabAdd);
        recyclerView = findViewById(R.id.recyclerOffers);
        emptyState   = findViewById(R.id.emptyState);
        tvOfferCount = findViewById(R.id.tvOfferCount);
    }

    private void setupRecyclerView() {
        adapter = new AdminOfferAdapter(this, offerList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddOfferActivity.class)));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Firebase
    // ══════════════════════════════════════════════════════════════════════

    private void loadOffers() {
        ref = FirebaseDatabase.getInstance().getReference("DiscoverOffers");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                offerList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    DiscoverOfferModel model = data.getValue(DiscoverOfferModel.class);
                    if (model != null) {
                        model.setId(data.getKey());
                        offerList.add(model);
                    }
                }

                adapter.notifyDataSetChanged();
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminManageOffersActivity.this,
                        error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // UI helpers
    // ══════════════════════════════════════════════════════════════════════

    private void updateUI() {
        boolean empty = offerList.isEmpty();
        recyclerView.setVisibility(empty ? View.GONE    : View.VISIBLE);
        emptyState.setVisibility  (empty ? View.VISIBLE : View.GONE);
        tvOfferCount.setText(offerList.size() + " active offer" +
                (offerList.size() == 1 ? "" : "s"));
    }
}