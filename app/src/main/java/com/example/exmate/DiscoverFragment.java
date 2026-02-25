package com.example.exmate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DiscoverFragment extends Fragment {

    private RecyclerView recyclerView;
    private DiscoverOfferAdapter adapter;
    private final List<DiscoverOfferModel> list = new ArrayList<>();
    private DatabaseReference ref;
    private String category;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        recyclerView = view.findViewById(R.id.rvDiscoverOffers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new DiscoverOfferAdapter(list);
        recyclerView.setAdapter(adapter);

        if (getArguments() != null) {
            category = getArguments().getString("category");
        }

        ref = FirebaseDatabase.getInstance()
                .getReference("DiscoverOffers");

        loadOffers();

        return view;
    }

    private void loadOffers() {

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                list.clear();

                long now = System.currentTimeMillis();
                SimpleDateFormat sdf =
                        new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",
                                Locale.getDefault());

                for (DataSnapshot ds : snapshot.getChildren()) {

                    DiscoverOfferModel model =
                            ds.getValue(DiscoverOfferModel.class);

                    if (model == null || !model.getIsActive())
                        continue;

                    if (category != null &&
                            !model.getCategory().equalsIgnoreCase(category))
                        continue;

                    try {
                        java.util.Date expiry =
                                sdf.parse(model.getExpiryDateTime());

                        if (expiry != null && expiry.getTime() > now) {
                            list.add(model);
                        }

                    } catch (Exception ignored) {}
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
