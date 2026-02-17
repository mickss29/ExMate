package com.example.exmate;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class VoiceListeningDialog {

    private final Dialog dialog;

    private View pulse1, pulse2, pulse3;
    private View micCard;
    private TextView tvDots;

    private Animation pulseAnim1, pulseAnim2, pulseAnim3;
    private Animation micBounceAnim, dotsAnim;

    public VoiceListeningDialog(Context context, Runnable onCancel) {

        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_voice_listening);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        pulse1 = dialog.findViewById(R.id.pulse1);
        pulse2 = dialog.findViewById(R.id.pulse2);
        pulse3 = dialog.findViewById(R.id.pulse3);

        micCard = dialog.findViewById(R.id.micCard);

        tvDots = dialog.findViewById(R.id.tvDots);
        TextView btnCancel = dialog.findViewById(R.id.btnCancelVoice);

        pulseAnim1 = AnimationUtils.loadAnimation(context, R.anim.pulse_scale);
        pulseAnim1.setRepeatCount(Animation.INFINITE);

        pulseAnim2 = AnimationUtils.loadAnimation(context, R.anim.pulse_scale);
        pulseAnim2.setRepeatCount(Animation.INFINITE);
        pulseAnim2.setStartOffset(220);

        pulseAnim3 = AnimationUtils.loadAnimation(context, R.anim.pulse_scale);
        pulseAnim3.setRepeatCount(Animation.INFINITE);
        pulseAnim3.setStartOffset(420);

        micBounceAnim = AnimationUtils.loadAnimation(context, R.anim.mic_bounce);
        dotsAnim = AnimationUtils.loadAnimation(context, R.anim.dots_fade);

        btnCancel.setOnClickListener(v -> {
            if (onCancel != null) onCancel.run();
        });
    }

    public void show() {
        try {
            if (!dialog.isShowing()) dialog.show();
        } catch (Exception ignored) {}

        if (pulse1 != null) pulse1.startAnimation(pulseAnim1);
        if (pulse2 != null) pulse2.startAnimation(pulseAnim2);
        if (pulse3 != null) pulse3.startAnimation(pulseAnim3);

        if (micCard != null) micCard.startAnimation(micBounceAnim);
        if (tvDots != null) tvDots.startAnimation(dotsAnim);
    }

    public void hide() {
        try {
            if (pulse1 != null) pulse1.clearAnimation();
            if (pulse2 != null) pulse2.clearAnimation();
            if (pulse3 != null) pulse3.clearAnimation();

            if (micCard != null) {
                micCard.clearAnimation();
                micCard.setScaleX(1f);
                micCard.setScaleY(1f);
            }

            if (tvDots != null) tvDots.clearAnimation();

            if (dialog.isShowing()) dialog.dismiss();
        } catch (Exception ignored) {}
    }

    // 🔥 Live RMS animation
    public void updateRms(float rmsdB) {
        if (micCard == null) return;

        float normalized = Math.min(10f, Math.max(0f, rmsdB));
        float scale = 1.0f + (normalized / 50f); // 1.0 -> 1.2

        micCard.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(120)
                .start();
    }
}
