package com.example.exmate;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class SelectBudgetCategoryBottomSheet extends BottomSheetDialogFragment {

    private OnBudgetCategorySelectedListener listener;
    private final List<BudgetCategoryModel> categoryList = new ArrayList<>();

    public void setListener(OnBudgetCategorySelectedListener listener) {
        this.listener = listener;
    }

    // 🔥 VERY IMPORTANT FOR ANDROID 13+
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BottomSheetDialog(
                requireContext(),
                R.style.ThemeOverlay_ExMate_BottomSheet
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.bottomsheet_select_budget_category,
                container,
                false
        );

        RecyclerView recyclerView =
                view.findViewById(R.id.recyclerViewCategories);
        Button btnDone = view.findViewById(R.id.btnDone);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        // Dummy categories
        categoryList.clear();
        categoryList.add(new BudgetCategoryModel("Food", 0));
        categoryList.add(new BudgetCategoryModel("Transport", 0));
        categoryList.add(new BudgetCategoryModel("Shopping", 0));
        categoryList.add(new BudgetCategoryModel("Bills", 0));
        categoryList.add(new BudgetCategoryModel("Entertainment", 0));

        recyclerView.setAdapter(
                new BudgetCategoryAdapter(
                        categoryList,
                        model -> {
                            // No action needed here
                            // This bottom sheet is only for selection
                        }
                )
        );

        btnDone.setOnClickListener(v -> {
            if (listener != null) {
                List<BudgetCategoryModel> selected = new ArrayList<>();
                for (BudgetCategoryModel model : categoryList) {
                    if (model.isSelected()) selected.add(model);
                }
                listener.onCategoriesSelected(selected);
            }
            dismissAllowingStateLoss(); // 🔒 prevent window crash
        });

        return view;
    }
}
