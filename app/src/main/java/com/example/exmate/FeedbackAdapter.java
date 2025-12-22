package com.example.exmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FeedbackAdapter
        extends RecyclerView.Adapter<FeedbackAdapter.VH> {

    public interface OnItemClick {
        void onClick(FeedbackModel model);
    }

    private final List<FeedbackModel> list;
    private final OnItemClick listener;

    public FeedbackAdapter(List<FeedbackModel> list, OnItemClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_feedback, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int p) {
        FeedbackModel m = list.get(p);

        h.tvUser.setText(m.userName);
        h.tvMessage.setText(m.message);
        h.tvStatus.setText(m.status.toUpperCase());

        h.itemView.setOnClickListener(v -> listener.onClick(m));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView tvUser, tvMessage, tvStatus;

        VH(@NonNull View v) {
            super(v);
            tvUser = v.findViewById(R.id.tvUser);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvStatus = v.findViewById(R.id.tvStatus);
        }
    }
}
