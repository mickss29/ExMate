package com.example.exmate;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    private Context context;
    private List<AdminUserModel> userList;

    public AdminUserAdapter(Context context, List<AdminUserModel> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        AdminUserModel user = userList.get(position);

        // Bind data
        holder.tvName.setText(user.getName());
        holder.tvEmail.setText(user.getEmail());
        holder.btnBlock.setText(user.isBlocked() ? "Unblock" : "Block");

        // Block / Unblock user
        holder.btnBlock.setOnClickListener(v -> {
            user.setBlocked(!user.isBlocked());
            notifyItemChanged(position);
        });

        // Open User Details screen
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserDetailsActivity.class);
            intent.putExtra("name", user.getName());
            intent.putExtra("email", user.getEmail());
            intent.putExtra("blocked", user.isBlocked());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // ================= VIEW HOLDER =================

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvEmail;
        Button btnBlock;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAdminUserName);
            tvEmail = itemView.findViewById(R.id.tvAdminUserEmail);
            btnBlock = itemView.findViewById(R.id.btnBlockUser);
        }
    }
}
