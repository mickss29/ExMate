package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminFeedbackActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private RecyclerView rvFeedback;
    private LinearLayout emptyState;
    private TextView tvFeedbackCount, tvUnreadCount;
    private MaterialCardView btnBack, badgeUnread;
    private MaterialCardView tabAll, tabUnread, tabReplied;

    // ── Data ───────────────────────────────────────────────────────────────
    private DatabaseReference feedbackRef;
    private final List<FeedbackModel> allFeedbacks = new ArrayList<>();
    private final List<FeedbackModel> feedbackList = new ArrayList<>();
    private FeedbackAdapter adapter;

    private String currentFilter = "all"; // "all" | "unread" | "replied"

    // ── Tab colors (hardcoded — no color resource needed) ──────────────────
    private static final int COLOR_TAB_ACTIVE_BG      = 0xFF0D1F4A;
    private static final int COLOR_TAB_INACTIVE_BG    = 0xFF0A1228;
    private static final int COLOR_TAB_ACTIVE_STROKE  = 0xFF1A3BCC;
    private static final int COLOR_TAB_INACTIVE_STROKE = 0xFF131F3A;
    private static final int COLOR_TAB_ACTIVE_TEXT    = 0xFF5B8BFF;
    private static final int COLOR_TAB_INACTIVE_TEXT  = 0xFF3D5A80;

    // ══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_feedback);

        initViews();
        setupRecyclerView();
        setClickListeners();
        loadFeedbacks();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Init
    // ══════════════════════════════════════════════════════════════════════

    private void initViews() {
        rvFeedback      = findViewById(R.id.rvFeedback);
        emptyState      = findViewById(R.id.emptyState);
        tvFeedbackCount = findViewById(R.id.tvFeedbackCount);
        tvUnreadCount   = findViewById(R.id.tvUnreadCount);
        btnBack         = findViewById(R.id.btnBack);
        badgeUnread     = findViewById(R.id.badgeUnread);
        tabAll          = findViewById(R.id.tabAll);
        tabUnread       = findViewById(R.id.tabUnread);
        tabReplied      = findViewById(R.id.tabReplied);
    }

    private void setupRecyclerView() {
        adapter = new FeedbackAdapter(feedbackList, feedback -> {
            Intent i = new Intent(this, AdminFeedbackDetailActivity.class);
            i.putExtra("feedbackId", feedback.id);
            startActivity(i);
        });
        rvFeedback.setLayoutManager(new LinearLayoutManager(this));
        rvFeedback.setAdapter(adapter);
    }

    private void setClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        tabAll.setOnClickListener(v     -> applyFilter("all"));
        tabUnread.setOnClickListener(v  -> applyFilter("unread"));
        tabReplied.setOnClickListener(v -> applyFilter("replied"));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Firebase
    // ══════════════════════════════════════════════════════════════════════

    private void loadFeedbacks() {
        feedbackRef = FirebaseDatabase.getInstance().getReference("feedbacks");
        tvFeedbackCount.setText("Loading...");

        feedbackRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allFeedbacks.clear();

                for (DataSnapshot s : snapshot.getChildren()) {
                    FeedbackModel model = s.getValue(FeedbackModel.class);
                    if (model != null) {
                        model.id = s.getKey();
                        allFeedbacks.add(model);
                    }
                }

                updateUnreadBadge();
                applyFilter(currentFilter);
                tvFeedbackCount.setText(allFeedbacks.size() + " total messages");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvFeedbackCount.setText("Failed to load");
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Filter
    // ══════════════════════════════════════════════════════════════════════

    private void applyFilter(String filter) {
        currentFilter = filter;
        feedbackList.clear();

        for (FeedbackModel f : allFeedbacks) {
            boolean replied = getFeedbackRepliedStatus(f);
            switch (filter) {
                case "unread":
                    if (!replied) feedbackList.add(f);
                    break;
                case "replied":
                    if (replied) feedbackList.add(f);
                    break;
                default: // "all"
                    feedbackList.add(f);
                    break;
            }
        }

        adapter.notifyDataSetChanged();
        updateTabHighlight(filter);
        updateEmptyState();
    }

    /**
     * Safely reads the replied status from FeedbackModel via reflection.
     * Works whether the field is boolean, Boolean, or doesn't exist yet.
     * Once you confirm your FeedbackModel field name, you can replace
     * this with a direct field access: return f.replied;
     */
    private boolean getFeedbackRepliedStatus(FeedbackModel f) {
        try {
            java.lang.reflect.Field field = FeedbackModel.class.getField("replied");
            Object val = field.get(f);
            if (val instanceof Boolean) return (Boolean) val;
            if (val instanceof String)  return "true".equalsIgnoreCase((String) val);
        } catch (Exception ignored) {
            // 'replied' field not found — treat all as unread
        }
        return false;
    }

    // ══════════════════════════════════════════════════════════════════════
    // UI helpers
    // ══════════════════════════════════════════════════════════════════════

    private void updateTabHighlight(String active) {
        setTabActive(tabAll,     active.equals("all"));
        setTabActive(tabUnread,  active.equals("unread"));
        setTabActive(tabReplied, active.equals("replied"));
    }

    private void setTabActive(MaterialCardView tab, boolean active) {
        tab.setCardBackgroundColor(active ? COLOR_TAB_ACTIVE_BG    : COLOR_TAB_INACTIVE_BG);
        tab.setStrokeColor        (active ? COLOR_TAB_ACTIVE_STROKE : COLOR_TAB_INACTIVE_STROKE);

        View inner = tab.getChildAt(0);
        if (inner instanceof LinearLayout) {
            View child = ((LinearLayout) inner).getChildAt(0);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(
                        active ? COLOR_TAB_ACTIVE_TEXT : COLOR_TAB_INACTIVE_TEXT);
            }
        }
    }

    private void updateUnreadBadge() {
        int unreadCount = 0;
        for (FeedbackModel f : allFeedbacks) {
            if (!getFeedbackRepliedStatus(f)) unreadCount++;
        }
        if (unreadCount > 0) {
            badgeUnread.setVisibility(View.VISIBLE);
            tvUnreadCount.setText(unreadCount + " New");
        } else {
            badgeUnread.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        boolean empty = feedbackList.isEmpty();
        rvFeedback.setVisibility(empty ? View.GONE    : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}