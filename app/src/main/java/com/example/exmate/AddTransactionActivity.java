package com.example.exmate;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AddTransactionActivity extends AppCompatActivity {

    private MaterialToolbar toolbarAdd;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        toolbarAdd = findViewById(R.id.toolbarAdd);
        tabLayout  = findViewById(R.id.tabLayout);
        viewPager  = findViewById(R.id.viewPager);

        setSupportActionBar(toolbarAdd);
        toolbarAdd.setNavigationOnClickListener(v -> finish());

        // ✅ ViewPager Adapter
        TransactionPagerAdapter adapter = new TransactionPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // 🔥 VERY IMPORTANT
        viewPager.setOffscreenPageLimit(2);

        // ✅ Tabs
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (position == 0) tab.setText("Expense");
                    if (position == 1) tab.setText("Income");
                }).attach();

        // ✅ Default tab = Expense
        viewPager.setCurrentItem(0, false);
    }

    // ===== OPTIONAL (future use) =====
    public void switchToIncome() {
        viewPager.setCurrentItem(1, true);
    }

    public void switchToExpense() {
        viewPager.setCurrentItem(0, true);
    }
}
