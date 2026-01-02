package com.example.exmate;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TransactionPagerAdapter extends FragmentStateAdapter {

    public TransactionPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {

        if (position == 0) {
            return new ExpenseFragment(); // DEFAULT
        } else {
            return new IncomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // 🔥 MUST BE 2
    }
}
