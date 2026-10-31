package com.example.quiztournament;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateTournamentActivity extends AppCompatActivity {

    private EditText editTextTournamentName;
    private EditText editTextDescription;
    private Spinner spinnerCategory;
    private Spinner spinnerDifficulty;
    private Button buttonStartDate;
    private Button buttonEndDate;
    private Button buttonCreateTournament;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private String startDate = "";
    private String endDate = "";

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final String[] categories = {
            "General Knowledge",
            "Books",
            "Film",
            "Music",
            "Video Games",
            "Science & Nature",
            "Computers",
            "Mathematics",
            "Sports",
            "Geography",
            "History",
            "Animals"
    };

    private final String[] difficulties = {
            "Easy",
            "Medium",
            "Hard"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_tournament);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        editTextTournamentName =
                findViewById(R.id.editTextTournamentName);

        editTextDescription =
                findViewById(R.id.editTextDescription);

        spinnerCategory =
                findViewById(R.id.spinnerCategory);

        spinnerDifficulty =
                findViewById(R.id.spinnerDifficulty);

        buttonStartDate =
                findViewById(R.id.buttonStartDate);

        buttonEndDate =
                findViewById(R.id.buttonEndDate);

        buttonCreateTournament =
                findViewById(R.id.buttonCreateTournament);

        setupSpinners();

        buttonStartDate.setOnClickListener(v ->
                showDatePicker(true));

        buttonEndDate.setOnClickListener(v ->
                showDatePicker(false));

        buttonCreateTournament.setOnClickListener(v ->
                createTournament());
    }

    private void setupSpinners() {

        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        categories
                );

        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> difficultyAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        difficulties
                );

        difficultyAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerDifficulty.setAdapter(difficultyAdapter);
    }

    private void showDatePicker(boolean isStartDate) {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    Calendar selected = Calendar.getInstance();
                    selected.clear();
                    selected.set(year, month, dayOfMonth);

                    String selectedDate =
                            dateFormat.format(selected.getTime());

                    if (isStartDate) {

                        startDate = selectedDate;

                        buttonStartDate.setText(
                                "Start Date: " + startDate
                        );

                    } else {

                        endDate = selectedDate;

                        buttonEndDate.setText(
                                "End Date: " + endDate
                        );
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.getDatePicker().setMinDate(
                System.currentTimeMillis() - 1000
        );

        dialog.show();
    }

    private void createTournament() {

        String name = editTextTournamentName
                .getText().toString().trim();

        String description = editTextDescription
                .getText().toString().trim();

        String category =
                spinnerCategory.getSelectedItem().toString();

        String difficulty =
                spinnerDifficulty.getSelectedItem().toString();

        if (name.isEmpty()) {
            editTextTournamentName.setError(
                    "Enter tournament name"
            );
            return;
        }

        if (description.isEmpty()) {
            editTextDescription.setError(
                    "Enter tournament description"
            );
            return;
        }

        if (startDate.isEmpty() || endDate.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please select start and end dates",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);

            if (start == null || end == null ||
                    end.before(start)) {

                Toast.makeText(
                        this,
                        "End date cannot be before start date",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Invalid tournament dates",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please log in first",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        buttonCreateTournament.setEnabled(false);

        // Verify the administrator's role before saving.
        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (!"admin".equals(
                            document.getString("role"))) {

                        buttonCreateTournament.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Administrator access required",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                        return;
                    }

                    Map<String, Object> tournament =
                            new HashMap<>();

                    tournament.put("name", name);
                    tournament.put("description", description);
                    tournament.put("category", category);
                    tournament.put("difficulty", difficulty);
                    tournament.put("startDate", startDate);
                    tournament.put("endDate", endDate);
                    tournament.put("createdBy", user.getUid());
                    tournament.put(
                            "createdAt",
                            FieldValue.serverTimestamp()
                    );

                    firestore.collection("tournaments")
                            .add(tournament)
                            .addOnSuccessListener(reference -> {

                                buttonCreateTournament.setEnabled(true);

                                Toast.makeText(
                                        this,
                                        "Tournament created successfully",
                                        Toast.LENGTH_LONG
                                ).show();

                                finish();
                            })
                            .addOnFailureListener(e -> {

                                buttonCreateTournament.setEnabled(true);

                                Toast.makeText(
                                        this,
                                        "Failed to create tournament: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {

                    buttonCreateTournament.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Unable to verify administrator: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
