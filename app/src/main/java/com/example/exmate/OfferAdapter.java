package com.example.exmate;

import android.content.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.ViewHolder> {

    private Context context;
    private List<DiscoverOfferModel> offerList;

    public OfferAdapter(Context context, List<DiscoverOfferModel> offerList) {
        this.context = context;
        this.offerList = offerList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_offer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        DiscoverOfferModel model = offerList.get(position);

        holder.title.setText(model.getTitle());
        holder.subtitle.setText(model.getSubtitle());
        holder.discount.setText(model.getDiscountPercent() + "% OFF");

        Glide.with(context)
                .load(model.getImageUrl())
                .placeholder(android.R.color.darker_gray)
                .into(holder.image);

        holder.copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("coupon", model.getCouponCode());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Coupon Copied!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return offerList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView title, subtitle, discount;
        Button copyButton;

        ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.offerImage);
            title = itemView.findViewById(R.id.offerTitle);
            subtitle = itemView.findViewById(R.id.offerSubtitle);
            discount = itemView.findViewById(R.id.offerDiscount);
            copyButton = itemView.findViewById(R.id.copyButton);
        }
    }
}