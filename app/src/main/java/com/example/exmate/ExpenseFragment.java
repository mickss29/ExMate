package com.example.exmate;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// ❗ If your parser/model are directly in com.example.exmate, keep these:
import com.example.exmate.ExpenseModel;
import com.example.exmate.VoiceExpenseParser;

public class ExpenseFragment extends Fragment {

    private static final int REQ_CODE_LOCATION = 2001;

    // 🎙️ Voice Permission
    private static final int REQ_AUDIO_PERMISSION = 9001;

    private EditText etExpenseAmount, etExpenseDate, etExpenseNote;
    private Spinner spExpenseCategory, spExpensePaymentMode;
    private Button btnSaveExpense;
    private ProgressBar progressSave;
    private MaterialCardView btnVoiceExpense;

    private android.widget.TextView tvVoiceDetected;

    // 📍 Location Suggestion UI
    private MaterialCardView cardLocationSuggestion;
    private android.widget.TextView tvLocationTitle, tvLocationSubtitle;
    private Button btnLocationAdd, btnLocationDismiss;

    // 📍 Location Client
    private FusedLocationProviderClient fusedLocationClient;
    private String detectedPlaceName = null;

    private DatabaseReference expenseRef;
    private String userId;
    private long selectedDateMillis = -1;

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

        return inflater.inflate(R.layout.expense_fragment, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupFirebase();
        setupCategorySpinner();
        setupPaymentSpinner();
        setupDatePicker();
        createTransactionChannel();

        setupDefaultDate();
        setupAmountWatcher();
        focusAmountField();

        // 📍 Location suggestion setup
        setupLocationSuggestion();

        btnSaveExpense.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            validateAndSave();
        });

        // 🎙️ Voice button
        btnVoiceExpense.setOnClickListener(v -> {
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
        etExpenseAmount      = view.findViewById(R.id.etExpenseAmount);
        etExpenseDate        = view.findViewById(R.id.etExpenseDate);
        etExpenseNote        = view.findViewById(R.id.etExpenseNote);
        spExpenseCategory    = view.findViewById(R.id.spExpenseCategory);
        spExpensePaymentMode = view.findViewById(R.id.spExpensePaymentMode);
        btnSaveExpense       = view.findViewById(R.id.btnSaveExpense);
        progressSave         = view.findViewById(R.id.progressSave);
        btnVoiceExpense      = view.findViewById(R.id.btnVoiceExpense);
        tvVoiceDetected      = view.findViewById(R.id.tvVoiceDetected);

        // 📍 Location Suggestion Views
        cardLocationSuggestion = view.findViewById(R.id.cardLocationSuggestion);
        tvLocationTitle = view.findViewById(R.id.tvLocationTitle);
        tvLocationSubtitle = view.findViewById(R.id.tvLocationSubtitle);
        btnLocationAdd = view.findViewById(R.id.btnLocationAdd);
        btnLocationDismiss = view.findViewById(R.id.btnLocationDismiss);

        progressSave.setVisibility(View.GONE);
        btnSaveExpense.setEnabled(false);
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

        expenseRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("expenses");
    }

    // ================= SPINNERS =================
    private void setupCategorySpinner() {
        String[] categories = {
                "Food", "Transport", "Shopping", "Bills",
                "Entertainment", "Health", "Education", "Other"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(requireContext(),
                        R.layout.spinner_item_premium,
                        categories);

        adapter.setDropDownViewResource(R.layout.spinner_item_premium);
        spExpenseCategory.setAdapter(adapter);
    }

    private void setupPaymentSpinner() {
        String[] modes = {"Cash", "UPI", "Bank Transfer", "Card", "Wallet"};

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(requireContext(),
                        R.layout.spinner_item_premium,
                        modes);

        adapter.setDropDownViewResource(R.layout.spinner_item_premium);
        spExpensePaymentMode.setAdapter(adapter);
    }

    // ================= DATE PICKER =================
    private void setupDatePicker() {
        etExpenseDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            new DatePickerDialog(
                    requireContext(),
                    (picker, y, m, d) -> {
                        Calendar cal = Calendar.getInstance();
                        cal.set(y, m, d, 0, 0, 0);
                        cal.set(Calendar.MILLISECOND, 0);

                        selectedDateMillis = cal.getTimeInMillis();
                        etExpenseDate.setText(d + "/" + (m + 1) + "/" + y);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    // ================= UX HELPERS =================
    private void setupDefaultDate() {
        Date now = new Date();
        selectedDateMillis = now.getTime();
        etExpenseDate.setText(
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(now)
        );
    }

    private void setupAmountWatcher() {
        etExpenseAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                try {
                    double val = Double.parseDouble(s.toString());
                    btnSaveExpense.setEnabled(val > 0);
                } catch (Exception e) {
                    btnSaveExpense.setEnabled(false);
                }
            }
        });
    }

    private void focusAmountField() {
        etExpenseAmount.requestFocus();
        etExpenseAmount.postDelayed(() -> {
            InputMethodManager imm =
                    (InputMethodManager) requireContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etExpenseAmount, InputMethodManager.SHOW_IMPLICIT);
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
                            applyVoiceParsedExpense(rawText);
                        }
                    }

                    @Override
                    public void onError(int error) {
                        hideVoiceDialog();
                        Toast.makeText(requireContext(), "Try again 😅", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onEndOfSpeech() {
                        // handled by results/error
                    }

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

    private void applyVoiceParsedExpense(String rawText) {

        ExpenseModel parsed = VoiceExpenseParser.parse(rawText);

        if (parsed.amount > 0) {
            etExpenseAmount.setText(String.valueOf(parsed.amount));
        }

        etExpenseNote.setText(parsed.note);

        selectedDateMillis = parsed.timestamp;
        etExpenseDate.setText(
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(new Date(parsed.timestamp))
        );

        setSpinnerByText(spExpensePaymentMode, parsed.paymentMode);

        String cat = parsed.category;
        if ("Travel".equalsIgnoreCase(cat)) cat = "Transport";
        setSpinnerByText(spExpenseCategory, cat);

        tvVoiceDetected.setVisibility(View.VISIBLE);
        tvVoiceDetected.setText("🎙️ Detected: " + rawText);
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

    // 🔥 Live RMS animation (Siri vibe)
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
    // 📍 LOCATION SUGGESTION
    // =========================================================================================

    private void setupLocationSuggestion() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        btnLocationDismiss.setOnClickListener(v -> cardLocationSuggestion.setVisibility(View.GONE));

        btnLocationAdd.setOnClickListener(v -> {

            if (detectedPlaceName == null || detectedPlaceName.trim().isEmpty()) {
                cardLocationSuggestion.setVisibility(View.GONE);
                return;
            }

            etExpenseNote.setText(detectedPlaceName);

            String lower = detectedPlaceName.toLowerCase(Locale.ROOT);

            if (lower.contains("starbucks") || lower.contains("cafe") || lower.contains("coffee")) {
                setSpinnerByText(spExpenseCategory, "Food");
            } else if (lower.contains("petrol") || lower.contains("fuel")) {
                setSpinnerByText(spExpenseCategory, "Transport");
            } else if (lower.contains("mall") || lower.contains("store") || lower.contains("shop")) {
                setSpinnerByText(spExpenseCategory, "Shopping");
            } else if (lower.contains("hospital") || lower.contains("clinic") || lower.contains("doctor")) {
                setSpinnerByText(spExpenseCategory, "Health");
            }

            cardLocationSuggestion.setVisibility(View.GONE);

            Toast.makeText(requireContext(), "Suggestion applied ✨", Toast.LENGTH_SHORT).show();
        });

        fetchLocationAndShowSuggestion();
    }

    private void fetchLocationAndShowSuggestion() {

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQ_CODE_LOCATION
            );
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        detectPlaceName(location);
                    }
                });
    }

    private void detectPlaceName(Location location) {

        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses =
                    geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            if (addresses != null && !addresses.isEmpty()) {

                Address address = addresses.get(0);

                String place = address.getFeatureName();

                if (place == null || place.trim().isEmpty()) {
                    place = address.getSubLocality();
                }
                if (place == null || place.trim().isEmpty()) {
                    place = address.getLocality();
                }

                if (place != null && !place.trim().isEmpty()) {

                    detectedPlaceName = place;

                    tvLocationTitle.setText("📍 Looks like you're here");
                    tvLocationSubtitle.setText("Detected: " + place + " — Add an expense?");

                    cardLocationSuggestion.setVisibility(View.VISIBLE);
                }
            }

        } catch (Exception ignored) {
            // No crash
        }
    }

    // =========================================================================================
    // PERMISSIONS
    // =========================================================================================

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_CODE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndShowSuggestion();
            }
            return;
        }

        if (requestCode == REQ_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput();
            } else {
                Toast.makeText(requireContext(), "Mic permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // =========================================================================================
    // SAVE
    // =========================================================================================

    private void validateAndSave() {

        if (expenseRef == null) return;

        String amountStr = etExpenseAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            etExpenseAmount.setError("Enter amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etExpenseAmount.setError("Amount must be > 0");
                return;
            }
        } catch (Exception e) {
            etExpenseAmount.setError("Invalid amount");
            return;
        }

        String selectedCategory = spExpenseCategory.getSelectedItem().toString();

        btnSaveExpense.setEnabled(false);
        progressSave.setVisibility(View.VISIBLE);

        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("time", selectedDateMillis);
        data.put("category", selectedCategory);
        data.put("paymentMode", spExpensePaymentMode.getSelectedItem().toString());
        data.put("note", etExpenseNote.getText().toString().trim());

        expenseRef.push().setValue(data)
                .addOnSuccessListener(unused -> {
                    hideLoader();
                    resetFields();

                    NotificationHelper.showExpenseSummaryNotification(
                            requireContext(),
                            amount,
                            selectedCategory
                    );

                    Toast.makeText(requireContext(),
                            "Expense added successfully",
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
        progressSave.setVisibility(View.GONE);
        btnSaveExpense.setEnabled(true);
    }

    private void resetFields() {
        etExpenseAmount.setText("");
        etExpenseNote.setText("");
        spExpenseCategory.setSelection(0);
        spExpensePaymentMode.setSelection(0);

        tvVoiceDetected.setText("");
        tvVoiceDetected.setVisibility(View.GONE);

        cardLocationSuggestion.setVisibility(View.GONE);
        detectedPlaceName = null;

        setupDefaultDate();
    }

    // ================= NOTIFICATION CHANNEL =================
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
