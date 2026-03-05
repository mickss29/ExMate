package com.example.exmate;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DiscoverActivity extends AppCompatActivity {

    // =========================================================================
    // VIEWS
    // =========================================================================
    private RecyclerView          categoryRecycler, offerRecycler;
    private ShimmerFrameLayout    shimmerLayout;
    private LinearLayout          emptyLayout;
    private TextView              offerTitle, tvOfferCount;
    private MaterialCardView      btnBack;

    // =========================================================================
    // ADAPTERS
    // =========================================================================
    private DiscoverCategoryAdapter categoryAdapter;
    private DiscoverOfferAdapter    offerAdapter;

    // =========================================================================
    // DATA
    // =========================================================================
    private final List<DiscoverOfferModel> offerList = new ArrayList<>();

    // =========================================================================
    // FIREBASE
    // =========================================================================
    private DatabaseReference offersRef;

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        initViews();
        setupRecyclerViews();

        // If launched from dashboard with a pre-selected category, load it directly
        String selectedCategory = getIntent().getStringExtra("selectedCategory");
        if (selectedCategory != null) {
            loadOffersByCategory(selectedCategory);
        } else {
            loadOffers();
        }
    }

    // =========================================================================
    // INIT
    // =========================================================================

    private void initViews() {
        categoryRecycler = findViewById(R.id.categoryRecycler);
        offerRecycler    = findViewById(R.id.offerRecycler);
        shimmerLayout    = findViewById(R.id.shimmerLayout);
        emptyLayout      = findViewById(R.id.emptyLayout);
        offerTitle       = findViewById(R.id.offerTitle);
        tvOfferCount     = findViewById(R.id.tvOfferCount);
        btnBack          = findViewById(R.id.btnBack);

        offersRef = FirebaseDatabase.getInstance().getReference("DiscoverOffers");

        // Back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerViews() {

        // ── Category chips ──
        categoryAdapter = new DiscoverCategoryAdapter(
                this,
                createCategoryList(),
                this::loadOffersByCategory
        );
        categoryRecycler.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        categoryRecycler.setAdapter(categoryAdapter);
        categoryRecycler.setHasFixedSize(true);

        // ── Offer list ──
        offerAdapter = new DiscoverOfferAdapter(offerList);
        offerRecycler.setLayoutManager(new LinearLayoutManager(this));
        offerRecycler.setAdapter(offerAdapter);
        offerRecycler.setHasFixedSize(false);
    }

    // =========================================================================
    // CATEGORY LIST
    // =========================================================================

    private List<DiscoverCategoryModel> createCategoryList() {
        List<DiscoverCategoryModel> list = new ArrayList<>();
        list.add(new DiscoverCategoryModel("Food",          R.drawable.ic_food));
        list.add(new DiscoverCategoryModel("Shopping",      R.drawable.ic_offer));
        list.add(new DiscoverCategoryModel("Travel",        R.drawable.ic_travel));
        list.add(new DiscoverCategoryModel("Recharge",      R.drawable.ic_bills));
        list.add(new DiscoverCategoryModel("Entertainment", R.drawable.ic_movie));
        return list;
    }

    // =========================================================================
    // LOAD OFFERS
    // =========================================================================

    private void loadOffers() {
        offerTitle.setText("All Offers");
        showLoading();

        Query query = offersRef.orderByChild("isActive").equalTo(true);
        fetchOffers(query, null);
    }

    private void loadOffersByCategory(String category) {
        offerTitle.setText(category + " Offers");
        showLoading();

        Query query = offersRef.orderByChild("isActive").equalTo(true);
        fetchOffers(query, category);
    }

    private void fetchOffers(Query query, String categoryFilter) {

        query.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                offerList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    DiscoverOfferModel model = data.getValue(DiscoverOfferModel.class);
                    if (model == null) continue;

                    if (categoryFilter == null) {
                        offerList.add(model);
                    } else if (model.getCategory() != null
                            && model.getCategory().equalsIgnoreCase(categoryFilter)) {
                        offerList.add(model);
                    }
                }

                // Update offer count label
                int count = offerList.size();
                tvOfferCount.setText(count > 0 ? count + " available" : "");

                hideLoading();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoading();
                Toast.makeText(DiscoverActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================================
    // SHIMMER HELPERS
    // =========================================================================

    private void showLoading() {
        shimmerLayout.setVisibility(View.VISIBLE);
        shimmerLayout.startShimmer();
        emptyLayout.setVisibility(View.GONE);
        offerRecycler.setVisibility(View.GONE);
        tvOfferCount.setText("");
    }

    private void hideLoading() {
        shimmerLayout.stopShimmer();
        shimmerLayout.setVisibility(View.GONE);

        if (offerList.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            offerRecycler.setVisibility(View.GONE);
        } else {
            offerAdapter.notifyDataSetChanged();
            offerRecycler.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        }
    }
}