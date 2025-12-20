package com.example.exmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminUserSelectAdapter
        extends RecyclerView.Adapter<AdminUserSelectAdapter.UserVH> {

    public interface OnUserClickListener {
        void onUserClick(AdminUserModel user);
    }

    private List<AdminUserModel> list;
    private OnUserClickListener listener;

    public AdminUserSelectAdapter(List<AdminUserModel> list,
                                  OnUserClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_admin_user_select, parent, false);
        return new UserVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserVH h, int p) {
        AdminUserModel u = list.get(p);

        h.tvName.setText(u.getName());
        h.tvEmail.setText(u.getEmail());

        h.itemView.setOnClickListener(v -> listener.onUserClick(u));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class UserVH extends RecyclerView.ViewHolder {

        TextView tvName, tvEmail;

        UserVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
        }
    }
}
