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
        switch (position) {
            case 0:
                return new AddExpenseActivity();
            case 1:
                return new AddIncomeActivity();
            default:
                return new AddExpenseActivity(); // fallback
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Expense + Income
    }
}
