
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditTournamentActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextDescription;
    private Spinner spinnerCategory;
    private Spinner spinnerDifficulty;
    private Button buttonStartDate;
    private Button buttonEndDate;
    private Button buttonSave;

    private FirebaseFirestore firestore;
    private String tournamentId;

    private String startDate = "";
    private String endDate = "";

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
            "Easy", "Medium", "Hard"
    };

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_tournament);

        firestore = FirebaseFirestore.getInstance();

        tournamentId = getIntent().getStringExtra("tournamentId");

        editTextName = findViewById(R.id.editTextName);
        editTextDescription = findViewById(R.id.editTextDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);
        buttonStartDate = findViewById(R.id.buttonStartDate);
        buttonEndDate = findViewById(R.id.buttonEndDate);
        buttonSave = findViewById(R.id.buttonSave);

        if (tournamentId == null || tournamentId.isEmpty()) {
            Toast.makeText(this, "Tournament not found",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        buttonSave.setEnabled(false);

        setupSpinners();

        buttonStartDate.setOnClickListener(v -> showDatePicker(true));
        buttonEndDate.setOnClickListener(v -> showDatePicker(false));
        buttonSave.setOnClickListener(v -> saveTournament());

        loadTournament();
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

    private void loadTournament() {

        firestore.collection("tournaments")
                .document(tournamentId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        Toast.makeText(this, "Tournament no longer exists",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    editTextName.setText(document.getString("name"));
                    editTextDescription.setText(
                            document.getString("description"));

                    String category = document.getString("category");
                    String difficulty = document.getString("difficulty");

                    int categoryIndex =
                            Arrays.asList(categories).indexOf(category);

                    int difficultyIndex =
                            Arrays.asList(difficulties).indexOf(difficulty);

                    if (categoryIndex >= 0) {
                        spinnerCategory.setSelection(categoryIndex);
                    }

                    if (difficultyIndex >= 0) {
                        spinnerDifficulty.setSelection(difficultyIndex);
                    }

                    startDate = document.getString("startDate");
                    endDate = document.getString("endDate");

                    if (startDate == null) startDate = "";
                    if (endDate == null) endDate = "";

                    buttonStartDate.setText("Start Date: " + startDate);
                    buttonEndDate.setText("End Date: " + endDate);

                    buttonSave.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to load tournament: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
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
                        buttonStartDate.setText("Start Date: " + startDate);
                    } else {
                        endDate = selectedDate;
                        buttonEndDate.setText("End Date: " + endDate);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void saveTournament() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String name = editTextName.getText().toString().trim();
        String description =
                editTextDescription.getText().toString().trim();

        if (name.isEmpty()) {
            editTextName.setError("Enter tournament name");
            return;
        }

        if (description.isEmpty()) {
            editTextDescription.setError("Enter description");
            return;
        }

        if (startDate.isEmpty() || endDate.isEmpty() ||
                endDate.compareTo(startDate) < 0) {

            Toast.makeText(this, "Invalid tournament dates",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("description", description);
        updates.put("category",
                spinnerCategory.getSelectedItem().toString());
        updates.put("difficulty",
                spinnerDifficulty.getSelectedItem().toString());
        updates.put("startDate", startDate);
        updates.put("endDate", endDate);

        buttonSave.setEnabled(false);

        firestore.collection("tournaments")
                .document(tournamentId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Tournament updated successfully",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    buttonSave.setEnabled(true);
                    Toast.makeText(this,
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
