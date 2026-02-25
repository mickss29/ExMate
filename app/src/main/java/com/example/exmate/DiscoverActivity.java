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
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class DiscoverActivity extends AppCompatActivity {

    private RecyclerView categoryRecycler, offerRecycler;
    private ShimmerFrameLayout shimmerLayout;
    private LinearLayout emptyLayout;
    private TextView offerTitle;

    private DiscoverCategoryAdapter categoryAdapter;
    private DiscoverOfferAdapter offerAdapter;   // ✅ FIXED

    private final List<DiscoverOfferModel> offerList = new ArrayList<>();

    private DatabaseReference offersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        initViews();
        setupRecyclerViews();
        loadOffers();
    }

    private void initViews() {
        categoryRecycler = findViewById(R.id.categoryRecycler);
        offerRecycler = findViewById(R.id.offerRecycler);
        shimmerLayout = findViewById(R.id.shimmerLayout);
        emptyLayout = findViewById(R.id.emptyLayout);
        offerTitle = findViewById(R.id.offerTitle);

        offersRef = FirebaseDatabase.getInstance()
                .getReference("DiscoverOffers");
    }

    private void setupRecyclerViews() {

        // Categories
        categoryAdapter = new DiscoverCategoryAdapter(
                this,
                createCategoryModelList(),
                this::loadOffersByCategory
        );

        categoryRecycler.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        categoryRecycler.setAdapter(categoryAdapter);
        categoryRecycler.setHasFixedSize(true);

        // Offers ✅ FIXED ADAPTER
        offerAdapter = new DiscoverOfferAdapter(offerList);
        offerRecycler.setLayoutManager(new LinearLayoutManager(this));
        offerRecycler.setAdapter(offerAdapter);
        offerRecycler.setHasFixedSize(true);
    }

    private List<DiscoverCategoryModel> createCategoryModelList() {
        List<DiscoverCategoryModel> list = new ArrayList<>();
        list.add(new DiscoverCategoryModel("Food", R.drawable.ic_food));
        list.add(new DiscoverCategoryModel("Shopping", R.drawable.ic_offer));
        list.add(new DiscoverCategoryModel("Travel", R.drawable.ic_travel));
        list.add(new DiscoverCategoryModel("Recharge", R.drawable.ic_bills));
        list.add(new DiscoverCategoryModel("Entertainment", R.drawable.ic_movie));
        return list;
    }

    private void loadOffers() {

        showLoading();

        Query query = offersRef
                .orderByChild("isActive")
                .equalTo(true);

        fetchOffers(query, null);
    }

    private void loadOffersByCategory(String category) {

        showLoading();
        offerTitle.setText(category + " Offers");

        Query query = offersRef
                .orderByChild("isActive")
                .equalTo(true);

        fetchOffers(query, category);
    }

    private void fetchOffers(Query query, String categoryFilter) {

        query.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                offerList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {

                    DiscoverOfferModel model =
                            data.getValue(DiscoverOfferModel.class);

                    if (model == null) continue;

                    if (categoryFilter == null) {
                        offerList.add(model);
                    } else if (model.getCategory() != null &&
                            model.getCategory().equalsIgnoreCase(categoryFilter)) {
                        offerList.add(model);
                    }
                }

                hideLoading();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoading();
                Toast.makeText(DiscoverActivity.this,
                        error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading() {
        shimmerLayout.setVisibility(View.VISIBLE);
        shimmerLayout.startShimmer();
        emptyLayout.setVisibility(View.GONE);
        offerRecycler.setVisibility(View.GONE);
    }

    private void hideLoading() {

        shimmerLayout.stopShimmer();
        shimmerLayout.setVisibility(View.GONE);

        if (offerList.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            offerAdapter.notifyDataSetChanged();
            offerRecycler.setVisibility(View.VISIBLE);
        }
    }
}