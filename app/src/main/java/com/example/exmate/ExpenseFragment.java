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
import android.widget.Switch;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
// ✅ FIXED: Removed duplicate "import java.util.Date" — was imported TWICE before
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
public class ExpenseFragment extends Fragment {
    private static final int REQ_CODE_LOCATION  = 2001;
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
    // ✅ NEW: Recurring toggle
    private Switch switchRecurring;
    private android.widget.Spinner spRecurringType;
    private android.widget.LinearLayout layoutRecurringType;
    // 📍 Location
    private FusedLocationProviderClient fusedLocationClient;
    private String detectedPlaceName = null;
    private DatabaseReference expenseRef;
    private String userId;
    private long selectedDateMillis = -1;
    // Voice
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    // Voice Dialog
    private Dialog voiceDialog;
    private View pulse1, pulse2, pulse3, micCard;
    private android.widget.TextView tvDots;
    private Animation pulseAnim1, pulseAnim2, pulseAnim3, micBounceAnim, dotsAnim;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.expense_fragment, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupFirebase();
        setupCategorySpinner();
        setupPaymentSpinner();
        setupDatePicker();
        setupRecurringToggle();
        // ✅ FIX #6: createTransactionChannel only once (moved to Activity ideally,
        //            but kept here for safety — createNotificationChannel is idempotent)
        createTransactionChannel();
        setupDefaultDate();
        setupAmountWatcher();
        setupQuickAmountChips(view);   // ✅ NEW FEATURE
        focusAmountField();
        setupLocationSuggestion();
        btnSaveExpense.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            validateAndSave();
        });
        btnVoiceExpense.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            startVoiceInput();
        });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopVoiceInput();
        destroySpeechRecognizer(); // ✅ FIXED: extracted to method
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
        cardLocationSuggestion = view.findViewById(R.id.cardLocationSuggestion);
        tvLocationTitle        = view.findViewById(R.id.tvLocationTitle);
        tvLocationSubtitle     = view.findViewById(R.id.tvLocationSubtitle);
        btnLocationAdd         = view.findViewById(R.id.btnLocationAdd);
        btnLocationDismiss     = view.findViewById(R.id.btnLocationDismiss);
        // ✅ NEW: Recurring UI
        switchRecurring    = view.findViewById(R.id.switchRecurring);
        spRecurringType    = view.findViewById(R.id.spRecurringType);
        layoutRecurringType = view.findViewById(R.id.layoutRecurringType);
        progressSave.setVisibility(View.GONE);
        btnSaveExpense.setEnabled(false);
    }
    // ================= FIREBASE =================
    private void setupFirebase() {
        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(requireContext(), "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_premium, categories);
        adapter.setDropDownViewResource(R.layout.spinner_item_premium);
        spExpenseCategory.setAdapter(adapter);
    }
    private void setupPaymentSpinner() {
        String[] modes = {"Cash", "UPI", "Bank Transfer", "Card", "Wallet"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_premium, modes);
        adapter.setDropDownViewResource(R.layout.spinner_item_premium);
        spExpensePaymentMode.setAdapter(adapter);
    }
    // ✅ NEW FEATURE: Recurring Toggle
    private void setupRecurringToggle() {
        String[] recurringTypes = {"Daily", "Weekly", "Monthly"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, recurringTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRecurringType.setAdapter(adapter);
        spRecurringType.setSelection(2); // Default: Monthly
        layoutRecurringType.setVisibility(View.GONE);
        switchRecurring.setOnCheckedChangeListener((btn, isChecked) -> {
            layoutRecurringType.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }
    // ✅ NEW FEATURE: Quick Amount Chips
    private void setupQuickAmountChips(View view) {
        int[] chipIds = {
                R.id.chip50, R.id.chip100,
                R.id.chip200, R.id.chip500, R.id.chip1000
        };
        double[] amounts = {50, 100, 200, 500, 1000};
        for (int i = 0; i < chipIds.length; i++) {
            final double amt = amounts[i];
            View chip = view.findViewById(chipIds[i]);
            if (chip != null) {
                chip.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    // If field already has value, ADD to it
                    String existing = etExpenseAmount.getText().toString().trim();
                    if (!existing.isEmpty()) {
                        try {
                            double current = Double.parseDouble(existing);
                            etExpenseAmount.setText(String.valueOf(current + amt));
                        } catch (Exception e) {
                            etExpenseAmount.setText(String.valueOf(amt));
                        }
                    } else {
                        etExpenseAmount.setText(String.valueOf(amt));
                    }
                    etExpenseAmount.setSelection(etExpenseAmount.getText().length());
                });
            }
        }
    }
    // ================= DATE =================
    private void setupDatePicker() {
        etExpenseDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(requireContext(),
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
    private void setupDefaultDate() {
        Date now = new Date();
        selectedDateMillis = now.getTime();
        etExpenseDate.setText(
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now));
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
            InputMethodManager imm = (InputMethodManager)
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(etExpenseAmount, InputMethodManager.SHOW_IMPLICIT);
        }, 200);
    }
    // ================= VOICE =================
    // ✅ FIXED BUG #2: Destroy recognizer before every new start
    //    Previously: if (speechRecognizer == null) { create }  ← bug
    //    If mic was tapped again while recognizer != null,
    //    startListening() on a busy recognizer → ERROR_RECOGNIZER_BUSY (3)
    private void startVoiceInput() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO_PERMISSION);
            Toast.makeText(requireContext(), "Allow mic permission first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Voice input not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // ✅ FIX: Always destroy before creating new — prevents ERROR_RECOGNIZER_BUSY
            destroySpeechRecognizer();
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) { showVoiceDialog(); }
                @Override
                public void onRmsChanged(float rmsdB) { updateVoiceRms(rmsdB); }
                @Override
                public void onResults(Bundle results) {
                    hideVoiceDialog();
                    ArrayList<String> matches =
                            results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        applyVoiceParsedExpense(matches.get(0));
                    }
                }
                @Override
                public void onError(int error) {
                    hideVoiceDialog();
                    String msg = (error == SpeechRecognizer.ERROR_NO_MATCH)
                            ? "Could not understand. Try again."
                            : "Voice error: " + error + ". Try again 😅";
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                }
                @Override public void onEndOfSpeech() {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
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
            Toast.makeText(requireContext(), "Voice input failed. Try again.", Toast.LENGTH_SHORT).show();
        }
    }
    // ✅ FIXED: Extracted to dedicated method, called from both stopVoiceInput + onDestroyView
    private void destroySpeechRecognizer() {
        try {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
                speechRecognizer.cancel();
                speechRecognizer.destroy();
            }
        } catch (Exception ignored) {}
        speechRecognizer = null;
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
        // ✅ Uses VoiceExpenseParser (consistent with existing logic)
        ExpenseModel parsed = VoiceExpenseParser.parse(rawText);
        if (parsed.amount > 0) etExpenseAmount.setText(String.valueOf(parsed.amount));
        etExpenseNote.setText(parsed.note);
        selectedDateMillis = parsed.timestamp;
        etExpenseDate.setText(
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(new Date(parsed.timestamp)));
        setSpinnerByText(spExpensePaymentMode, parsed.paymentMode);
        String cat = parsed.category;
        if ("Travel".equalsIgnoreCase(cat)) cat = "Transport";
        setSpinnerByText(spExpenseCategory, cat);
        tvVoiceDetected.setVisibility(View.VISIBLE);
        tvVoiceDetected.setText("🎙️ Detected: " + rawText);
    }
    private void setSpinnerByText(Spinner spinner, String text) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter == null || text == null) return;
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(text)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
    // ================= VOICE DIALOG =================
    private void showVoiceDialog() {
        if (getContext() == null) return;
        if (voiceDialog == null) {
            voiceDialog = new Dialog(requireContext());
            voiceDialog.setContentView(R.layout.dialog_voice_listening);
            voiceDialog.setCancelable(false);
            if (voiceDialog.getWindow() != null) {
                voiceDialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
                voiceDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            pulse1 = voiceDialog.findViewById(R.id.pulse1);
            pulse2 = voiceDialog.findViewById(R.id.pulse2);
            pulse3 = voiceDialog.findViewById(R.id.pulse3);
            micCard = voiceDialog.findViewById(R.id.micCard);
            tvDots  = voiceDialog.findViewById(R.id.tvDots);
            pulseAnim1 = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_scale);
            pulseAnim1.setRepeatCount(Animation.INFINITE);
            pulseAnim2 = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_scale);
            pulseAnim2.setRepeatCount(Animation.INFINITE);
            pulseAnim2.setStartOffset(220);
            pulseAnim3 = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_scale);
            pulseAnim3.setRepeatCount(Animation.INFINITE);
            pulseAnim3.setStartOffset(420);
            micBounceAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.mic_bounce);
            dotsAnim      = AnimationUtils.loadAnimation(requireContext(), R.anim.dots_fade);
            android.widget.TextView btnCancel = voiceDialog.findViewById(R.id.btnCancelVoice);
            btnCancel.setOnClickListener(v -> stopVoiceInput());
        }
        try {
            if (!voiceDialog.isShowing()) voiceDialog.show();
        } catch (Exception ignored) {}
        if (pulse1 != null) pulse1.startAnimation(pulseAnim1);
        if (pulse2 != null) pulse2.startAnimation(pulseAnim2);
        if (pulse3 != null) pulse3.startAnimation(pulseAnim3);
        if (micCard != null) micCard.startAnimation(micBounceAnim);
        if (tvDots  != null) tvDots.startAnimation(dotsAnim);
    }
    private void hideVoiceDialog() {
        try {
            if (pulse1 != null) pulse1.clearAnimation();
            if (pulse2 != null) pulse2.clearAnimation();
            if (pulse3 != null) pulse3.clearAnimation();
            if (micCard != null) { micCard.clearAnimation(); micCard.setScaleX(1f); micCard.setScaleY(1f); }
            if (tvDots  != null) tvDots.clearAnimation();
            if (voiceDialog != null && voiceDialog.isShowing()) voiceDialog.dismiss();
        } catch (Exception ignored) {}
    }
    private void updateVoiceRms(float rmsdB) {
        if (micCard == null) return;
        float normalized = Math.min(10f, Math.max(0f, rmsdB));
        float scale = 1.0f + (normalized / 50f);
        micCard.animate().scaleX(scale).scaleY(scale).setDuration(120).start();
    }
    // ================= LOCATION =================
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
            if (lower.contains("starbucks") || lower.contains("cafe") || lower.contains("coffee"))
                setSpinnerByText(spExpenseCategory, "Food");
            else if (lower.contains("petrol") || lower.contains("fuel"))
                setSpinnerByText(spExpenseCategory, "Transport");
            else if (lower.contains("mall") || lower.contains("store") || lower.contains("shop"))
                setSpinnerByText(spExpenseCategory, "Shopping");
            else if (lower.contains("hospital") || lower.contains("clinic"))
                setSpinnerByText(spExpenseCategory, "Health");
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
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQ_CODE_LOCATION);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) detectPlaceName(location);
                });
    }
    private void detectPlaceName(Location location) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String place = address.getFeatureName();
                if (place == null || place.trim().isEmpty()) place = address.getSubLocality();
                if (place == null || place.trim().isEmpty()) place = address.getLocality();
                if (place != null && !place.trim().isEmpty()) {
                    detectedPlaceName = place;
                    tvLocationTitle.setText("📍 Looks like you're here");
                    tvLocationSubtitle.setText("Detected: " + place + " — Add an expense?");
                    cardLocationSuggestion.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception ignored) {}
    }
    // ================= PERMISSIONS =================
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                fetchLocationAndShowSuggestion();
        } else if (requestCode == REQ_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                startVoiceInput();
            else
                Toast.makeText(requireContext(), "Mic permission denied", Toast.LENGTH_SHORT).show();
        }
    }
    // ================= SAVE =================
    private void validateAndSave() {
        if (expenseRef == null) return;
        String amountStr = etExpenseAmount.getText().toString().trim();
        if (amountStr.isEmpty()) { etExpenseAmount.setError("Enter amount"); return; }
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) { etExpenseAmount.setError("Amount must be > 0"); return; }
        } catch (Exception e) {
            etExpenseAmount.setError("Invalid amount");
            return;
        }
        // ✅ FIX: Safety guard — never save -1 to Firebase
        if (selectedDateMillis <= 0) selectedDateMillis = System.currentTimeMillis();
        String selectedCategory = spExpenseCategory.getSelectedItem().toString();
        boolean isRecurring = switchRecurring.isChecked();
        String recurringType = isRecurring
                ? spRecurringType.getSelectedItem().toString()
                : "None";
        btnSaveExpense.setEnabled(false);
        progressSave.setVisibility(View.VISIBLE);
        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("time", selectedDateMillis);
        data.put("category", selectedCategory);
        data.put("paymentMode", spExpensePaymentMode.getSelectedItem().toString());
        data.put("note", etExpenseNote.getText().toString().trim());
        data.put("type", "Expense");
        data.put("recurring", isRecurring);           // ✅ NEW
        data.put("recurringType", recurringType);      // ✅ NEW
        expenseRef.push().setValue(data)
                .addOnSuccessListener(unused -> {
                    hideLoader();
                    resetFields();
                    NotificationHelper.showExpenseSummaryNotification(
                            requireContext(), amount, selectedCategory);
                    Toast.makeText(requireContext(), "Expense added ✅", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    hideLoader();
                    Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        switchRecurring.setChecked(false);
        detectedPlaceName = null;
        setupDefaultDate();
    }
    private void createTransactionChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    "transaction_alert", "Transaction Alerts", NotificationManager.IMPORTANCE_HIGH);
            requireContext().getSystemService(NotificationManager.class)
                    .createNotificationChannel(channel);
        }
    }
}