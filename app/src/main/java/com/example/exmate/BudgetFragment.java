package com.example.exmate;

import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BudgetFragment extends Fragment {

    private View root;
    private DatabaseReference budgetRef, expenseRef;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        root = inflater.inflate(R.layout.fragment_budget, container, false);

        setupFirebase();
        setupAllCategories();
        playEntryAnimation();
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    101
            );
        }

        createNotificationChannel();


        return root;
    }

    // ================= FIREBASE =================

    private void setupFirebase() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String monthKey = new SimpleDateFormat(
                "yyyy-MM", Locale.getDefault()
        ).format(new Date());

        budgetRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("budgets")
                .child(monthKey);

        expenseRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("expenses");
    }

    // ================= CATEGORIES =================

    private void setupAllCategories() {

        bind("Food", R.id.tvFoodAmount, R.id.tilFood, R.id.etFood,
                R.id.btnFoodAction, R.id.progressFood);

        bind("Transport", R.id.tvTransportAmount, R.id.tilTransport, R.id.etTransport,
                R.id.btnTransportAction, R.id.progressTransport);

        bind("Shopping", R.id.tvShoppingAmount, R.id.tilShopping, R.id.etShopping,
                R.id.btnShoppingAction, R.id.progressShopping);

        bind("Bills", R.id.tvBillsAmount, R.id.tilBills, R.id.etBills,
                R.id.btnBillsAction, R.id.progressBills);

        bind("Entertainment", R.id.tvEntertainmentAmount, R.id.tilEntertainment, R.id.etEntertainment,
                R.id.btnEntertainmentAction, R.id.progressEntertainment);

        bind("Health", R.id.tvHealthAmount, R.id.tilHealth, R.id.etHealth,
                R.id.btnHealthAction, R.id.progressHealth);

        bind("Education", R.id.tvEducationAmount, R.id.tilEducation, R.id.etEducation,
                R.id.btnEducationAction, R.id.progressEducation);

        bind("Other", R.id.tvOtherAmount, R.id.tilOther, R.id.etOther,
                R.id.btnOtherAction, R.id.progressOther);

    }

    // ================= CORE =================

    private void bind(
            String category,
            int tvAmountId,
            int tilId,
            int etId,
            int btnId,
            int progressId
    ) {

        TextView tvAmount = root.findViewById(tvAmountId);
        TextInputLayout til = root.findViewById(tilId);
        EditText et = root.findViewById(etId);
        MaterialButton btn = root.findViewById(btnId);
        ProgressBar progress = root.findViewById(progressId);

        // 🔄 Budget listener (real-time)
        budgetRef.child(category).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {

                double budget = snap.exists() ? snap.getValue(Double.class) : 0;
                tvAmount.setText(budget > 0 ? "₹ " + budget : "₹ Not set");
                btn.setText(budget > 0 ? "UPDATE" : "ADD");

                attachExpenseListener(category, budget, progress);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 🟢 Add / Update button
        btn.setOnClickListener(v -> {

            if (til.getVisibility() == View.GONE) {
                til.setVisibility(View.VISIBLE);
                et.requestFocus();
                return;
            }

            String val = et.getText().toString().trim();
            if (TextUtils.isEmpty(val)) return;

            try {
                double amt = Double.parseDouble(val);
                if (amt <= 0) return;

                budgetRef.child(category).setValue(amt);
                til.setVisibility(View.GONE);
                et.setText("");

            } catch (Exception ignored) {}
        });
    }

    // ================= EXPENSE LISTENER =================

    private void attachExpenseListener(
            String category,
            double budget,
            ProgressBar bar
    ) {

        expenseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                double spent = 0;
                long start = getMonthStartMillis();
                long end = getMonthEndMillis();

                for (DataSnapshot s : snapshot.getChildren()) {

                    String cat = s.child("category").getValue(String.class);
                    Double amt = s.child("amount").getValue(Double.class);
                    Long time = s.child("time").getValue(Long.class);

                    if (cat == null || amt == null || time == null) continue;
                    if (!cat.equals(category)) continue;
                    if (time < start || time > end) continue;

                    spent += amt;
                }

                updateProgressUI(spent, budget, bar, category);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ================= PROGRESS UI =================

    private void updateProgressUI(
            double spent,
            double budget,
            ProgressBar bar,
            String category
    )
 {

        if (budget <= 0) {
            bar.setProgress(0);
            bar.setProgressTintList(
                    ColorStateList.valueOf(
                            getResources().getColor(R.color.purple_500)
                    )
            );
            return;
        }

        int percent = (int) ((spent / budget) * 100);
        bar.setProgress(Math.min(percent, 100));

        if (spent > budget) {
            bar.setProgressTintList(ColorStateList.valueOf(Color.RED));
            pulse(bar);
        } else if (percent >= 80) {

            bar.setProgressTintList(
                    ColorStateList.valueOf(Color.parseColor("#FFA500"))
            );

            if (canNotify(category)) {
                send80PercentNotification(category);
            }
        }
        else {
            bar.setProgressTintList(
                    ColorStateList.valueOf(
                            getResources().getColor(R.color.purple_500)
                    )
            );
        }
    }

    // ================= MONTH HELPERS =================

    private long getMonthStartMillis() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long getMonthEndMillis() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH,
                c.getActualMaximum(Calendar.DAY_OF_MONTH));
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    // ================= ANIM =================

    private void pulse(View v) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.4f, 1f);
        anim.setDuration(600);
        anim.start();
    }

    private void playEntryAnimation() {
        root.setAlpha(0f);
        root.setTranslationY(40f);
        root.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel(
                            "budget_alert",
                            "Budget Alerts",
                            android.app.NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription("Budget usage alerts");

            android.app.NotificationManager manager =
                    requireContext().getSystemService(
                            android.app.NotificationManager.class
                    );

            manager.createNotificationChannel(channel);
        }
    }
    private void send80PercentNotification(String category) {

        android.app.Notification notification =
                new androidx.core.app.NotificationCompat.Builder(
                        requireContext(), "budget_alert"
                )
                        .setSmallIcon(R.drawable.ic_budget) // use your icon
                        .setContentTitle("⚠️ Budget Alert")
                        .setContentText("You have used 80% of your " + category + " budget")
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build();

        android.app.NotificationManager manager =
                (android.app.NotificationManager)
                        requireContext().getSystemService(
                                android.app.NotificationManager.class
                        );

        manager.notify(category.hashCode(), notification);
    }
    private boolean canNotify(String category) {

        String monthKey = new SimpleDateFormat(
                "yyyy-MM", Locale.getDefault()
        ).format(new Date());

        String key = category + "_" + monthKey;

        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences(
                        "budget_notify", android.content.Context.MODE_PRIVATE
                );

        if (prefs.getBoolean(key, false)) return false;

        prefs.edit().putBoolean(key, true).apply();
        return true;
    }


}
