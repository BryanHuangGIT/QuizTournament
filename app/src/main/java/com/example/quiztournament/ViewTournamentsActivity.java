
package com.example.quiztournament;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewTournamentsActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private LinearLayout tournamentContainer;
    private TextView textViewNoTournaments;

    private boolean isAdmin = false;
    private boolean isGenerating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_tournaments);

        firestore = FirebaseFirestore.getInstance();

        tournamentContainer =
                findViewById(R.id.tournamentContainer);

        textViewNoTournaments =
                findViewById(R.id.textViewNoTournaments);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        verifyAdmin();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isAdmin) {
            loadTournaments();
        }
    }

    private void verifyAdmin() {

        String uid = FirebaseAuth.getInstance()
                .getCurrentUser().getUid();

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if ("admin".equals(document.getString("role"))) {
                        isAdmin = true;
                        loadTournaments();
                    } else {
                        Toast.makeText(this,
                                "Administrator access required",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Unable to verify administrator",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void loadTournaments() {

        firestore.collection("tournaments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {

                    tournamentContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        textViewNoTournaments.setVisibility(View.VISIBLE);
                        return;
                    }

                    textViewNoTournaments.setVisibility(View.GONE);

                    for (DocumentSnapshot document : snapshot) {

                        String tournamentId = document.getId();

                        String category =
                                safe(document.getString("category"));

                        String difficulty =
                                safe(document.getString("difficulty"));

                        LinearLayout card = new LinearLayout(this);
                        card.setOrientation(LinearLayout.VERTICAL);
                        card.setPadding(24, 24, 24, 24);
                        card.setBackgroundColor(0xFFECEFF1);

                        LinearLayout.LayoutParams params =
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );

                        params.setMargins(0, 0, 0, 24);
                        card.setLayoutParams(params);

                        TextView title = new TextView(this);
                        title.setText(safe(document.getString("name")));
                        title.setTextSize(20);
                        title.setTextColor(0xFF111111);
                        title.setTypeface(null,
                                android.graphics.Typeface.BOLD);

                        TextView details = new TextView(this);

                        details.setText(
                                "Description: " +
                                        safe(document.getString("description")) +
                                        "\nCategory: " + category +
                                        "\nDifficulty: " + difficulty +
                                        "\nStart Date: " +
                                        safe(document.getString("startDate")) +
                                        "\nEnd Date: " +
                                        safe(document.getString("endDate"))
                        );

                        details.setTextColor(0xFF333333);
                        details.setTextSize(15);
                        details.setPadding(0, 12, 0, 12);

                        TextView questionStatus = new TextView(this);
                        questionStatus.setText("Questions: Checking...");
                        questionStatus.setTextSize(15);
                        questionStatus.setTextColor(0xFF333333);

                        Button generateButton = new Button(this);
                        generateButton.setText("Generate Questions");
                        generateButton.setEnabled(false);

                        DocumentReference tournamentRef =
                                firestore.collection("tournaments")
                                        .document(tournamentId);

                        tournamentRef.get()
                                .addOnSuccessListener(current -> {

                                    if (!current.exists()) return;

                                    if (Boolean.TRUE.equals(
                                            current.getBoolean("questionsGenerated"))) {

                                        questionStatus.setText(
                                                "Questions: 10 generated");

                                        generateButton.setText(
                                                "Questions Generated");

                                        generateButton.setEnabled(false);

                                    } else {

                                        questionStatus.setText(
                                                "Questions: Not generated");

                                        generateButton.setEnabled(true);
                                    }
                                })
                                .addOnFailureListener(e ->
                                        questionStatus.setText(
                                                "Questions: Unable to check"));

                        generateButton.setOnClickListener(v ->
                                confirmGenerate(
                                        tournamentId,
                                        category,
                                        difficulty,
                                        generateButton
                                ));

                        Button editButton = new Button(this);
                        editButton.setText("Edit");

                        editButton.setOnClickListener(v -> {

                            Intent intent = new Intent(
                                    this,
                                    EditTournamentActivity.class
                            );

                            intent.putExtra("tournamentId", tournamentId);
                            startActivity(intent);
                        });

                        Button deleteButton = new Button(this);
                        deleteButton.setText("Delete");

                        deleteButton.setOnClickListener(v ->
                                confirmDelete(tournamentId));

                        card.addView(title);
                        card.addView(details);
                        card.addView(questionStatus);
                        card.addView(generateButton);
                        card.addView(editButton);
                        card.addView(deleteButton);

                        tournamentContainer.addView(card);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load tournaments: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void confirmGenerate(
            String tournamentId,
            String category,
            String difficulty,
            Button generateButton
    ) {

        new AlertDialog.Builder(this)
                .setTitle("Generate Quiz Questions")
                .setMessage(
                        "Generate 10 questions for this tournament?\n\n" +
                                "Category: " + category +
                                "\nDifficulty: " + difficulty +
                                "\n\nAll players will receive these same questions."
                )
                .setPositiveButton("Generate", (dialog, which) ->
                        generateQuestions(
                                tournamentId,
                                category,
                                difficulty,
                                generateButton
                        ))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void generateQuestions(
            String tournamentId,
            String category,
            String difficulty,
            Button generateButton
    ) {

        if (isGenerating) {
            Toast.makeText(this,
                    "Please wait for question generation",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        isGenerating = true;
        generateButton.setEnabled(false);
        generateButton.setText("Generating...");

        DocumentReference tournamentRef =
                firestore.collection("tournaments")
                        .document(tournamentId);

        tournamentRef.get()
                .addOnSuccessListener(document -> {

                    if (!document.exists() ||
                            Boolean.TRUE.equals(
                                    document.getBoolean("questionsGenerated"))) {

                        isGenerating = false;
                        loadTournaments();
                        return;
                    }

                    String currentCategory =
                            document.getString("category");

                    String currentDifficulty =
                            document.getString("difficulty");

                    if (currentCategory == null ||
                            currentDifficulty == null) {

                        generationFailed(
                                generateButton,
                                "Tournament settings are missing");
                        return;
                    }

                    OpenTDBApi.fetchQuestions(
                            currentCategory,
                            currentDifficulty,
                            "Mixed",
                            new OpenTDBApi.QuestionsCallback() {

                                @Override
                                public void onSuccess(
                                        List<QuizQuestion> questions) {

                                    if (questions.size() != 10) {
                                        generationFailed(
                                                generateButton,
                                                "Expected exactly 10 questions");
                                        return;
                                    }

                                    saveQuestions(
                                            tournamentId,
                                            questions,
                                            generateButton
                                    );
                                }

                                @Override
                                public void onError(String message) {
                                    generationFailed(
                                            generateButton,
                                            message);
                                }
                            }
                    );
                })
                .addOnFailureListener(e ->
                        generationFailed(
                                generateButton,
                                e.getMessage()));
    }

    private void saveQuestions(
            String tournamentId,
            List<QuizQuestion> questions,
            Button generateButton
    ) {

        DocumentReference tournamentRef =
                firestore.collection("tournaments")
                        .document(tournamentId);

        WriteBatch batch = firestore.batch();

        for (int i = 0; i < questions.size(); i++) {

            QuizQuestion question = questions.get(i);

            Map<String, Object> questionData = new HashMap<>();

            questionData.put("question", question.getQuestion());
            questionData.put("type", question.getType());
            questionData.put("answers",
                    new ArrayList<>(question.getAnswers()));
            questionData.put("correctAnswer",
                    question.getCorrectAnswer());
            questionData.put("questionNumber", i + 1);

            DocumentReference questionRef =
                    tournamentRef.collection("questions")
                            .document("question_" + (i + 1));

            batch.set(questionRef, questionData);
        }

        batch.update(tournamentRef,
                "questionsGenerated", true);

        batch.commit()
                .addOnSuccessListener(unused -> {

                    isGenerating = false;

                    Toast.makeText(this,
                            "10 questions saved successfully!",
                            Toast.LENGTH_LONG).show();

                    loadTournaments();
                })
                .addOnFailureListener(e ->
                        generationFailed(
                                generateButton,
                                e.getMessage()));
    }

    private void generationFailed(
            Button generateButton,
            String message
    ) {

        isGenerating = false;

        generateButton.setEnabled(true);
        generateButton.setText("Generate Questions");

        Toast.makeText(this,
                "Generation failed: " + message,
                Toast.LENGTH_LONG).show();
    }

    private void confirmDelete(String tournamentId) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Tournament")
                .setMessage(
                        "Are you sure you want to delete this tournament?")
                .setPositiveButton("Delete", (dialog, which) -> {

                    firestore.collection("tournaments")
                            .document(tournamentId)
                            .delete()
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(this,
                                        "Tournament deleted",
                                        Toast.LENGTH_SHORT).show();

                                loadTournaments();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Delete failed: " +
                                                    e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
