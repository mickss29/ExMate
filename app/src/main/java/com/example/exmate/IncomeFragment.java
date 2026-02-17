package com.example.exmate;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class IncomeFragment extends Fragment {

    // 🎙️ Voice Permission
    private static final int REQ_AUDIO_PERMISSION = 9001;

    private EditText etIncomeAmount, etIncomeDate, etIncomeNote;
    private Spinner spIncomeSource, spPaymentMode;
    private Button btnSaveIncome;
    private ProgressBar progressSaveIncome;

    // 🎙️ Voice
    private com.google.android.material.card.MaterialCardView btnVoiceIncome;
    private android.widget.TextView tvVoiceDetectedIncome;

    private long selectedDateMillis = -1;
    private DatabaseReference incomeRef;
    private String userId;

    // ================= VOICE (CUSTOM, NO GOOGLE POPUP) =================
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

    // ================= PREMIUM VOICE DIALOG =================
    private Dialog voiceDialog;
    private View pulse1, pulse2, pulse3, micCard;
    private android.widget.TextView tvDots;
    private Animation pulseAnim1, pulseAnim2, pulseAnim3, micBounceAnim, dotsAnim;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.income_fragment, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupFirebase();
        setupSourceSpinner();
        setupPaymentSpinner();
        setupDatePicker();
        createTransactionChannel();

        setupDefaultDate();
        setupAmountWatcher();
        focusAmountField();

        btnSaveIncome.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            validateAndSave();
        });

        // 🎙️ Voice button
        btnVoiceIncome.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            startVoiceInput();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        stopVoiceInput();

        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }
    }

    // ================= INIT =================
    private void initViews(View view) {
        etIncomeAmount     = view.findViewById(R.id.etIncomeAmount);
        etIncomeDate       = view.findViewById(R.id.etIncomeDate);
        etIncomeNote       = view.findViewById(R.id.etIncomeNote);
        spIncomeSource     = view.findViewById(R.id.spIncomeSource);
        spPaymentMode      = view.findViewById(R.id.spPaymentMode);
        btnSaveIncome      = view.findViewById(R.id.btnSaveIncome);
        progressSaveIncome = view.findViewById(R.id.progressSaveIncome);

        btnVoiceIncome = view.findViewById(R.id.btnVoiceIncome);
        tvVoiceDetectedIncome = view.findViewById(R.id.tvVoiceDetectedIncome);

        progressSaveIncome.setVisibility(View.GONE);
        btnSaveIncome.setEnabled(false);
    }

    // ================= FIREBASE =================
    private void setupFirebase() {
        userId = FirebaseAuth.getInstance().getUid();

        if (userId == null) {
            Toast.makeText(requireContext(),
                    "Session expired. Please login again.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        incomeRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("incomes");
    }

    // ================= SPINNERS =================
    private void setupSourceSpinner() {
        String[] sources = {
                "Salary", "Business", "Freelance", "Investment",
                "Rental Income", "Bonus", "Gift", "Other"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(requireContext(),
                        R.layout.spinner_item_premium,
                        sources);

        adapter.setDropDownViewResource(R.layout.spinner_item_premium);
        spIncomeSource.setAdapter(adapter);
    }

    private void setupPaymentSpinner() {
        String[] modes = {"Cash", "UPI", "Bank Transfer", "Card", "Cheque"};

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(requireContext(),
                        R.layout.spinner_item_premium,
                        modes);

        adapter.setDropDownViewResource(R.layout.spinner_item_premium);
        spPaymentMode.setAdapter(adapter);
    }

    // ================= DATE PICKER =================
    private void setupDatePicker() {
        etIncomeDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();

            new DatePickerDialog(
                    requireContext(),
                    (picker, y, m, d) -> {
                        Calendar cal = Calendar.getInstance();
                        cal.set(y, m, d, 0, 0, 0);
                        cal.set(Calendar.MILLISECOND, 0);

                        selectedDateMillis = cal.getTimeInMillis();
                        etIncomeDate.setText(d + "/" + (m + 1) + "/" + y);
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    // ================= UX HELPERS =================
    private void setupDefaultDate() {
        Date now = new Date();
        selectedDateMillis = now.getTime();
        etIncomeDate.setText(
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(now)
        );
    }

    private void setupAmountWatcher() {
        etIncomeAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                try {
                    double val = Double.parseDouble(s.toString());
                    btnSaveIncome.setEnabled(val > 0);
                } catch (Exception e) {
                    btnSaveIncome.setEnabled(false);
                }
            }
        });
    }

    private void focusAmountField() {
        etIncomeAmount.requestFocus();
        etIncomeAmount.postDelayed(() -> {
            InputMethodManager imm =
                    (InputMethodManager) requireContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etIncomeAmount, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    // =========================================================================================
    // 🎙️ VOICE (CUSTOM PREMIUM)
    // =========================================================================================

    private void startVoiceInput() {

        // ✅ Permission check
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQ_AUDIO_PERMISSION
            );

            Toast.makeText(requireContext(), "Allow mic permission first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {

            if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
                Toast.makeText(requireContext(), "Voice input not supported", Toast.LENGTH_SHORT).show();
                return;
            }

            if (speechRecognizer == null) {

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());

                speechRecognizer.setRecognitionListener(new RecognitionListener() {

                    @Override
                    public void onReadyForSpeech(Bundle params) {
                        showVoiceDialog();
                    }

                    @Override
                    public void onRmsChanged(float rmsdB) {
                        updateVoiceRms(rmsdB);
                    }

                    @Override
                    public void onResults(Bundle results) {
                        hideVoiceDialog();

                        ArrayList<String> matches =
                                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                        if (matches != null && !matches.isEmpty()) {
                            String rawText = matches.get(0);
                            applyVoiceParsedIncome(rawText);
                        }
                    }

                    @Override
                    public void onError(int error) {
                        hideVoiceDialog();
                        Toast.makeText(requireContext(), "Try again 😅", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onEndOfSpeech() {}

                    // required but unused
                    @Override public void onBeginningOfSpeech() {}
                    @Override public void onBufferReceived(byte[] buffer) {}
                    @Override public void onPartialResults(Bundle partialResults) {}
                    @Override public void onEvent(int eventType, Bundle params) {}
                });
            }

            if (speechIntent == null) {
                speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
                speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            }

            speechRecognizer.startListening(speechIntent);

        } catch (Exception e) {
            hideVoiceDialog();
            Toast.makeText(requireContext(), "Voice input not supported", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopVoiceInput() {
        try {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
                speechRecognizer.cancel();
            }
        } catch (Exception ignored) {}

        hideVoiceDialog();
    }

    private void applyVoiceParsedIncome(String rawText) {

        String lower = rawText.toLowerCase(Locale.ROOT);

        // Amount detect
        double amount = detectAmount(lower);
        if (amount > 0) etIncomeAmount.setText(String.valueOf(amount));

        // Note
        etIncomeNote.setText(cleanIncomeNote(lower));

        // Date
        long time = detectDateMillis(lower);
        selectedDateMillis = time;
        etIncomeDate.setText(
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(new Date(time))
        );

        // Payment Mode detect
        String pay = detectPaymentMode(lower);
        setSpinnerByText(spPaymentMode, pay);

        // Source detect
        String src = detectIncomeSource(lower);
        setSpinnerByText(spIncomeSource, src);

        // UI feedback
        tvVoiceDetectedIncome.setVisibility(View.VISIBLE);
        tvVoiceDetectedIncome.setText("🎙️ Detected: " + rawText);
    }

    // =========================================================================================
    // 🎨 PREMIUM VOICE DIALOG UI
    // =========================================================================================

    private void showVoiceDialog() {
        if (getContext() == null) return;

        if (voiceDialog == null) {
            voiceDialog = new Dialog(requireContext());
            voiceDialog.setContentView(R.layout.dialog_voice_listening);
            voiceDialog.setCancelable(false);

            if (voiceDialog.getWindow() != null) {
                voiceDialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                voiceDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            pulse1 = voiceDialog.findViewById(R.id.pulse1);
            pulse2 = voiceDialog.findViewById(R.id.pulse2);
            pulse3 = voiceDialog.findViewById(R.id.pulse3);

            micCard = voiceDialog.findViewById(R.id.micCard);
            tvDots = voiceDialog.findViewById(R.id.tvDots);

            android.widget.TextView btnCancel = voiceDialog.findViewById(R.id.btnCancelVoice);

            pulseAnim1 = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_scale);
            pulseAnim1.setRepeatCount(Animation.INFINITE);

            pulseAnim2 = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_scale);
            pulseAnim2.setRepeatCount(Animation.INFINITE);
            pulseAnim2.setStartOffset(220);

            pulseAnim3 = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_scale);
            pulseAnim3.setRepeatCount(Animation.INFINITE);
            pulseAnim3.setStartOffset(420);

            micBounceAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.mic_bounce);
            dotsAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.dots_fade);

            btnCancel.setOnClickListener(v -> stopVoiceInput());
        }

        try {
            if (!voiceDialog.isShowing()) voiceDialog.show();
        } catch (Exception ignored) {}

        if (pulse1 != null) pulse1.startAnimation(pulseAnim1);
        if (pulse2 != null) pulse2.startAnimation(pulseAnim2);
        if (pulse3 != null) pulse3.startAnimation(pulseAnim3);

        if (micCard != null) micCard.startAnimation(micBounceAnim);
        if (tvDots != null) tvDots.startAnimation(dotsAnim);
    }

    private void hideVoiceDialog() {
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

            if (voiceDialog != null && voiceDialog.isShowing()) {
                voiceDialog.dismiss();
            }
        } catch (Exception ignored) {}
    }

    // 🔥 Live RMS animation
    private void updateVoiceRms(float rmsdB) {
        if (micCard == null) return;

        float normalized = Math.min(10f, Math.max(0f, rmsdB));
        float scale = 1.0f + (normalized / 50f); // 1.0 -> 1.2

        micCard.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(120)
                .start();
    }

    // =========================================================================================
    // VOICE HELPERS (SAME LOGIC)
    // =========================================================================================

    private double detectAmount(String text) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d{1,2})?)");
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) return Double.parseDouble(m.group(1));
        } catch (Exception ignored) {}
        return 0;
    }

    private long detectDateMillis(String text) {
        Calendar cal = Calendar.getInstance();

        if (text.contains("parso") || text.contains("day before yesterday")) {
            cal.add(Calendar.DAY_OF_YEAR, -2);
        } else if (text.contains("kal") || text.contains("yesterday")) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return cal.getTimeInMillis();
    }

    private String detectPaymentMode(String text) {

        if (text.contains("upi") || text.contains("gpay") || text.contains("phonepe") || text.contains("paytm")) {
            return "UPI";
        }
        if (text.contains("cash")) return "Cash";
        if (text.contains("card") || text.contains("credit") || text.contains("debit")) return "Card";
        if (text.contains("bank") || text.contains("transfer")) return "Bank Transfer";
        if (text.contains("cheque")) return "Cheque";

        return "Cash";
    }

    private String detectIncomeSource(String text) {

        if (text.contains("salary")) return "Salary";
        if (text.contains("business")) return "Business";
        if (text.contains("freelance")) return "Freelance";
        if (text.contains("investment") || text.contains("stock") || text.contains("profit")) return "Investment";
        if (text.contains("rent") || text.contains("rental")) return "Rental Income";
        if (text.contains("bonus")) return "Bonus";
        if (text.contains("gift")) return "Gift";
        if (text.contains("refund") || text.contains("cashback")) return "Other";

        return "Other";
    }

    private String cleanIncomeNote(String text) {

        String clean = text;

        clean = clean.replace("today", "")
                .replace("aaj", "")
                .replace("yesterday", "")
                .replace("kal", "")
                .replace("parso", "")
                .replace("day before yesterday", "");

        clean = clean.replace("upi", "")
                .replace("gpay", "")
                .replace("phonepe", "")
                .replace("paytm", "")
                .replace("cash", "")
                .replace("card", "")
                .replace("debit", "")
                .replace("credit", "")
                .replace("bank transfer", "")
                .replace("transfer", "")
                .replace("bank", "")
                .replace("cheque", "");

        clean = clean.replace("rupees", "")
                .replace("rupaye", "")
                .replace("rs", "")
                .replace("₹", "");

        clean = clean.replaceAll("(\\d+(?:\\.\\d{1,2})?)", "");
        clean = clean.replaceAll("\\s+", " ").trim();

        if (clean.isEmpty()) return "Income";

        return clean.substring(0, 1).toUpperCase() + clean.substring(1);
    }

    private void setSpinnerByText(Spinner spinner, String text) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter == null) return;

        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(text)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    // =========================================================================================
    // PERMISSION
    // =========================================================================================

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput();
            } else {
                Toast.makeText(requireContext(), "Mic permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // =========================================================================================
    // SAVE (UNCHANGED)
    // =========================================================================================

    private void validateAndSave() {

        if (incomeRef == null) return;

        String amountStr = etIncomeAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            etIncomeAmount.setError("Enter amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etIncomeAmount.setError("Amount must be > 0");
                return;
            }
        } catch (Exception e) {
            etIncomeAmount.setError("Invalid amount");
            return;
        }

        String selectedSource = spIncomeSource.getSelectedItem().toString();

        btnSaveIncome.setEnabled(false);
        progressSaveIncome.setVisibility(View.VISIBLE);

        Map<String, Object> map = new HashMap<>();
        map.put("amount", amount);
        map.put("time", selectedDateMillis);
        map.put("source", selectedSource);
        map.put("paymentMode", spPaymentMode.getSelectedItem().toString());
        map.put("note", etIncomeNote.getText().toString().trim());

        incomeRef.push().setValue(map)
                .addOnSuccessListener(unused -> {
                    hideLoader();
                    resetFields();

                    NotificationHelper.showIncomeSummaryNotification(
                            requireContext(),
                            amount,
                            selectedSource
                    );

                    Toast.makeText(requireContext(),
                            "Income saved successfully",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    hideLoader();
                    Toast.makeText(requireContext(),
                            "Failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void hideLoader() {
        progressSaveIncome.setVisibility(View.GONE);
        btnSaveIncome.setEnabled(true);
    }

    private void resetFields() {
        etIncomeAmount.setText("");
        etIncomeNote.setText("");
        spIncomeSource.setSelection(0);
        spPaymentMode.setSelection(0);

        tvVoiceDetectedIncome.setText("");
        tvVoiceDetectedIncome.setVisibility(View.GONE);

        setupDefaultDate();
    }

    // ================= NOTIFICATION =================
    private void createTransactionChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel(
                            "transaction_alert",
                            "Transaction Alerts",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            requireContext()
                    .getSystemService(NotificationManager.class)
                    .createNotificationChannel(channel);
        }
    }
}
