package com.example.exmate;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BudgetHomeFragment extends Fragment {

    public BudgetHomeFragment() {
        super(R.layout.fragment_container_blank);
    }

    @Override
    public void onViewCreated(
            @NonNull android.view.View view,
            @Nullable Bundle savedInstanceState) {

        decideScreen();
    }

    private void decideScreen() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String monthKey = new SimpleDateFormat(
                "yyyy-MM", Locale.getDefault())
                .format(new Date());

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("budgets")
                        .child("monthly")
                        .child(monthKey);

        ref.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        Fragment target =
                                snapshot.exists()
                                        ? new BudgetFragment()

                                        : new BudgetEmptyFragment();

                        requireActivity()
                                .getSupportFragmentManager()
                                .beginTransaction()
                                .replace(
                                        R.id.fragmentContainer,
                                        target
                                )
                                .commit();
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {}
                });
    }
}
