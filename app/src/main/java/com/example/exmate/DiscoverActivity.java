package com.example.exmate;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class DiscoverActivity extends AppCompatActivity {

    private ViewPager2 bannerViewPager;
    private RecyclerView categoryRecycler, offerRecycler;
    private ShimmerFrameLayout shimmerLayout;
    private LinearLayout emptyLayout;

    private BannerAdapter bannerAdapter;
    private DiscoverCategoryAdapter categoryAdapter;
    private OfferAdapter offerAdapter;

    private final List<String> bannerList = new ArrayList<>();
    private final List<String> categoryList = new ArrayList<>();
    private final List<DiscoverOfferModel> offerList = new ArrayList<>();

    private DatabaseReference offersRef;
    private String selectedCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        selectedCategory = getIntent().getStringExtra("selectedCategory");

        initViews();
        setupRecyclerViews();
        loadBanners();

        if (selectedCategory != null && !selectedCategory.isEmpty()) {
            loadOffersByCategory(selectedCategory);
        } else {
            loadOffers();
        }
    }

    private void initViews() {
        bannerViewPager = findViewById(R.id.bannerViewPager);
        categoryRecycler = findViewById(R.id.categoryRecycler);
        offerRecycler = findViewById(R.id.offerRecycler);
        shimmerLayout = findViewById(R.id.shimmerLayout);
        emptyLayout = findViewById(R.id.emptyLayout);

        offersRef = FirebaseDatabase.getInstance()
                .getReference("DiscoverOffers");
    }

    private void setupRecyclerViews() {

        // Banner
        bannerAdapter = new BannerAdapter(this, bannerList);
        bannerViewPager.setAdapter(bannerAdapter);

        // Categories (Horizontal)
        categoryAdapter = new DiscoverCategoryAdapter(
                this,
                createCategoryModelList(),
                category -> loadOffersByCategory(category)
        );

        categoryRecycler.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        categoryRecycler.setAdapter(categoryAdapter);

        // Offers (Vertical)
        offerAdapter = new OfferAdapter(this, offerList);
        offerRecycler.setLayoutManager(new LinearLayoutManager(this));
        offerRecycler.setAdapter(offerAdapter);

        // 🔥 Stability Fixes (No Logic Change)
        categoryRecycler.setNestedScrollingEnabled(false);
        offerRecycler.setNestedScrollingEnabled(false);

        categoryRecycler.setHasFixedSize(true);
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

    private void loadBanners() {
        bannerList.clear();
        bannerList.add("https://picsum.photos/800/400?1");
        bannerList.add("https://picsum.photos/800/400?2");
        bannerList.add("https://picsum.photos/800/400?3");
        bannerAdapter.notifyDataSetChanged();
    }

    private void loadOffers() {

        shimmerLayout.startShimmer();
        shimmerLayout.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.GONE);

        Query query = offersRef
                .orderByChild("isActive")
                .equalTo(true);

        fetchOffers(query);
    }

    private void loadOffersByCategory(String category) {

        shimmerLayout.startShimmer();
        shimmerLayout.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.GONE);

        Query query = offersRef
                .orderByChild("isActive")
                .equalTo(true);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                offerList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {

                    DiscoverOfferModel model =
                            data.getValue(DiscoverOfferModel.class);

                    if (model == null) continue;

                    if (model.getCategory() != null &&
                            model.getCategory().equalsIgnoreCase(category)) {

                        offerList.add(model);
                    }
                }

                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);

                if (offerList.isEmpty()) {
                    emptyLayout.setVisibility(View.VISIBLE);
                } else {
                    offerAdapter.notifyDataSetChanged();
                    offerRecycler.requestLayout(); // 🔥 layout refresh safety
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
                Toast.makeText(DiscoverActivity.this,
                        error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchOffers(Query query) {

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                offerList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {

                    DiscoverOfferModel model =
                            data.getValue(DiscoverOfferModel.class);

                    if (model != null && model.isActive()) {
                        offerList.add(model);
                    }
                }

                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);

                if (offerList.isEmpty()) {
                    emptyLayout.setVisibility(View.VISIBLE);
                } else {
                    offerAdapter.notifyDataSetChanged();
                    offerRecycler.requestLayout(); // 🔥 layout refresh safety
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
                Toast.makeText(DiscoverActivity.this,
                        error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}