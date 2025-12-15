package com.example.exmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryReportAdapter
        extends RecyclerView.Adapter<CategoryReportAdapter.VH> {

    private List<CategoryReportModel> list;

    public CategoryReportAdapter(List<CategoryReportModel> list) {
        this.list = list;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_report, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH h, int p) {
        CategoryReportModel m = list.get(p);
        h.txtCategory.setText(m.getCategory());
        h.txtAmount.setText("₹" + String.format("%,.0f", m.getAmount()));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView txtCategory, txtAmount;

        VH(View v) {
            super(v);
            txtCategory = v.findViewById(R.id.txtCategory);
            txtAmount = v.findViewById(R.id.txtAmount);
        }
    }
}
