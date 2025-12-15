package com.example.exmate;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    private Context context;
    private List<AdminUserModel> userList;
    private DatabaseReference usersRef;

    public AdminUserAdapter(Context context, List<AdminUserModel> userList) {
        this.context = context;
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

        holder.tvName.setText(user.getName());
        holder.tvEmail.setText(user.getEmail());
        holder.btnBlock.setText(user.isBlocked() ? "Unblock" : "Block");

        // 🔴 BLOCK / UNBLOCK (REAL FIREBASE)
        holder.btnBlock.setOnClickListener(v -> {

            boolean newStatus = !user.isBlocked();

            usersRef.child(user.getUid())
                    .child("blocked")
                    .setValue(newStatus)
                    .addOnSuccessListener(unused -> {

                        user.setBlocked(newStatus);
                        notifyItemChanged(position);

                        Toast.makeText(
                                context,
                                newStatus ? "User Blocked" : "User Unblocked",
                                Toast.LENGTH_SHORT
                        ).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(
                                    context,
                                    "Failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show()
                    );
        });

        // 👉 OPEN USER DETAILS
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserDetailsActivity.class);
            intent.putExtra("uid", user.getUid());
            intent.putExtra("name", user.getName());
            intent.putExtra("email", user.getEmail());
            intent.putExtra("phone", user.getPhone());
            intent.putExtra("blocked", user.isBlocked());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

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
