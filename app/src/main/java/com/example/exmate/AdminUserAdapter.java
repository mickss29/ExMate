package com.example.exmate;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    private final Context context;
    private final List<AdminUserModel> userList;
    private final DatabaseReference usersRef;

    public AdminUserAdapter(Context context, List<AdminUserModel> userList) {
        this.context  = context;
        this.userList = userList;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
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

        // ── Name & Email ───────────────────────────────────────────────────
        String name  = user.getName()  != null ? user.getName()  : "Unknown";
        String email = user.getEmail() != null ? user.getEmail() : "—";
        String phone = user.getPhone() != null ? user.getPhone() : "—";

        holder.tvUserName.setText(name);
        holder.tvUserEmail.setText(email);
        holder.tvUserPhone.setText(phone);

        // ── Avatar initial ─────────────────────────────────────────────────
        holder.tvUserInitial.setText(
                name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase()
        );

        // ── Joined date ────────────────────────────────────────────────────
        String joined = user.getCreatedAt() != null ? user.getCreatedAt() : "—";
        holder.tvUserJoined.setText(joined);

        // ── Status badge ───────────────────────────────────────────────────
        boolean blocked = user.isBlocked();
        holder.tvUserStatus.setText(blocked ? "Blocked" : "Active");
        holder.tvUserStatus.setTextColor(
                blocked ? 0xFFFF5C6A : 0xFF22CC66
        );
        holder.badgeStatus.setStrokeColor(
                blocked ? 0xFF4A0D1B : 0xFF0B3D2E
        );
        holder.badgeStatus.setCardBackgroundColor(
                blocked ? 0xFF120820 : 0xFF051A10
        );

        // ── View button → open UserDetailsActivity ─────────────────────────
        holder.btnViewUser.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserDetailsActivity.class);
            intent.putExtra("uid",     user.getUid());
            intent.putExtra("name",    name);
            intent.putExtra("email",   email);
            intent.putExtra("phone",   phone);
            intent.putExtra("blocked", blocked);
            context.startActivity(intent);
        });

        // ── Delete button → confirm then remove from Firebase ──────────────
        holder.btnDeleteUser.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete User")
                    .setMessage("Are you sure you want to delete " + name + "?")
                    .setPositiveButton("Delete", (d, w) -> {
                        usersRef.child(user.getUid()).removeValue()
                                .addOnSuccessListener(unused -> {
                                    int pos = userList.indexOf(user);
                                    if (pos != -1) {
                                        userList.remove(pos);
                                        notifyItemRemoved(pos);
                                    }
                                    Toast.makeText(context,
                                            name + " deleted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context,
                                                "Failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show()
                                );
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // ── Row click → same as View button ───────────────────────────────
        holder.itemView.setOnClickListener(v ->
                holder.btnViewUser.performClick()
        );
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // ══════════════════════════════════════════════════════════════════════
    // ViewHolder — IDs match item_admin_user.xml exactly
    // ══════════════════════════════════════════════════════════════════════
    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView         tvUserName, tvUserEmail, tvUserPhone,
                tvUserJoined, tvUserInitial, tvUserStatus;
        MaterialCardView btnViewUser, btnDeleteUser, badgeStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName    = itemView.findViewById(R.id.tvUserName);
            tvUserEmail   = itemView.findViewById(R.id.tvUserEmail);
            tvUserPhone   = itemView.findViewById(R.id.tvUserPhone);
            tvUserJoined  = itemView.findViewById(R.id.tvUserJoined);
            tvUserInitial = itemView.findViewById(R.id.tvUserInitial);
            tvUserStatus  = itemView.findViewById(R.id.tvUserStatus);
            btnViewUser   = itemView.findViewById(R.id.btnViewUser);
            btnDeleteUser = itemView.findViewById(R.id.btnDeleteUser);
            badgeStatus   = itemView.findViewById(R.id.badgeStatus);
        }
    }
}