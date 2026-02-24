package com.example.exmate;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class AdminOfferAdapter
        extends RecyclerView.Adapter<AdminOfferAdapter.ViewHolder> {

    private Context context;
    private List<DiscoverOfferModel> list;

    public AdminOfferAdapter(Context context,
                             List<DiscoverOfferModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_admin_offer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 int position) {

        DiscoverOfferModel model = list.get(position);

        holder.tvTitle.setText(model.getTitle());
        holder.tvDiscount.setText(
                model.getDiscountPercent() + "% OFF");

        Glide.with(context)
                .load(model.getImageUrl())
                .into(holder.imgOffer);

        // DELETE
        holder.btnDelete.setOnClickListener(v -> {

            new AlertDialog.Builder(context)
                    .setTitle("Delete Offer")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes", (dialog, which) -> {

                        FirebaseDatabase.getInstance()
                                .getReference("DiscoverOffers")
                                .child(model.getId())
                                .removeValue();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        // EDIT
        holder.btnEdit.setOnClickListener(v -> {

            Intent intent =
                    new Intent(context,
                            AddOfferActivity.class);

            intent.putExtra("offerId", model.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder
            extends RecyclerView.ViewHolder {

        ImageView imgOffer;
        TextView tvTitle, tvDiscount;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgOffer = itemView.findViewById(R.id.imgOffer);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}