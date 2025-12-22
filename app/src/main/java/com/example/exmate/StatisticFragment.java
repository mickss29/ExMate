package com.example.exmate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class StatisticFragment extends Fragment {

    // UI
    private ChipGroup chipGroupRange;
    private TextView txtInsight;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.statistics_fragment, container, false);

        initViews(view);
        setupChips();

        return view;
    }

    // ================= INIT =================

    private void initViews(View view) {
        chipGroupRange = view.findViewById(R.id.chipGroupRange);
        txtInsight = view.findViewById(R.id.txtInsight);
    }

    // ================= CHIP LOGIC =================

    private void setupChips() {

        // Default insight
        txtInsight.setText("📊 Insights about your spending will appear here");

        chipGroupRange.setOnCheckedChangeListener((group, checkedId) -> {

            if (checkedId == View.NO_ID) return;

            Chip chip = group.findViewById(checkedId);
            if (chip == null) return;

            String selected = chip.getText().toString();

            switch (selected) {
                case "Today":
                    txtInsight.setText("📅 Showing today's spending");
                    break;

                case "This Month":
                    txtInsight.setText("🗓 Showing this month's spending");
                    break;

                case "This Year":
                    txtInsight.setText("📆 Showing this year's spending");
                    break;
            }
        });
    }
}


