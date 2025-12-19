package com.example.exmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TransactionAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<TransactionListItem> list;

    public TransactionAdapter(List<TransactionListItem> list) {
        this.list = list;
    }

    // ---------------- VIEW TYPES ----------------

    @Override
    public int getItemViewType(int position) {
        return list.get(position).getType();
    }

    // ---------------- CREATE ----------------

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TransactionListItem.TYPE_DATE) {
            View view = inflater.inflate(
                    R.layout.item_date_header, parent, false);
            return new DateViewHolder(view);
        } else {
            View view = inflater.inflate(
                    R.layout.item_transection, parent, false);
            return new TransactionViewHolder(view);
        }
    }

    // ---------------- BIND ----------------

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position) {

        TransactionListItem item = list.get(position);

        if (holder instanceof DateViewHolder) {

            ((DateViewHolder) holder)
                    .txtDateHeader
                    .setText(item.getDateTitle());

        } else if (holder instanceof TransactionViewHolder) {

            TransactionViewHolder vh =
                    (TransactionViewHolder) holder;

            vh.txtCategory.setText(item.getCategory());
            vh.txtNote.setText(item.getNote());
            vh.txtAmount.setText(item.getAmount());
            vh.txtMeta.setText(item.getMeta());

            // Badge visibility
            if (item.isHighSpend()) {
                vh.txtBadge.setVisibility(View.VISIBLE);
            } else {
                vh.txtBadge.setVisibility(View.GONE);
            }
        }
    }

    // ---------------- COUNT ----------------

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ================= VIEW HOLDERS =================

    static class DateViewHolder
            extends RecyclerView.ViewHolder {

        TextView txtDateHeader;

        DateViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDateHeader =
                    itemView.findViewById(R.id.txtDateHeader);
        }
    }

    static class TransactionViewHolder
            extends RecyclerView.ViewHolder {

        TextView txtCategory, txtNote,
                txtAmount, txtMeta, txtBadge;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);

            txtCategory =
                    itemView.findViewById(R.id.txtCategory);
            txtNote =
                    itemView.findViewById(R.id.txtNote);
            txtAmount =
                    itemView.findViewById(R.id.txtAmount);
            txtMeta =
                    itemView.findViewById(R.id.txtMeta);
            txtBadge =
                    itemView.findViewById(R.id.txtBadge);
        }
    }
}
