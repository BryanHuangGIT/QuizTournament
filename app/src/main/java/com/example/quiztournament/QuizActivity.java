
package com.example.quiztournament;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuizActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private TextView textViewQuizTitle;
    private TextView textViewQuestionNumber;
    private TextView textViewQuestion;
    private TextView textViewProgress;

    private RadioGroup radioGroupAnswers;
    private Button buttonNext;

    private String tournamentId;
    private String tournamentName;

    private List<DocumentSnapshot> questions = new ArrayList<>();

    private int currentQuestionIndex = 0;
    private int score = 0;

    private boolean quizFinished = false;
    private boolean answerSubmitted = false;

    private String selectedAnswer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        textViewQuizTitle =
                findViewById(R.id.textViewQuizTitle);

        textViewQuestionNumber =
                findViewById(R.id.textViewQuestionNumber);

        textViewQuestion =
                findViewById(R.id.textViewQuestion);

        textViewProgress =
                findViewById(R.id.textViewProgress);

        radioGroupAnswers =
                findViewById(R.id.radioGroupAnswers);

        buttonNext =
                findViewById(R.id.buttonNext);

        tournamentId = getIntent().getStringExtra("tournamentId");

        if (tournamentId == null || tournamentId.isEmpty()) {
            Toast.makeText(this,
                    "Tournament not found",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        buttonNext.setEnabled(false);
        buttonNext.setOnClickListener(v -> checkAnswer());

        verifyPlayerAndLoadTournament(user.getUid());
    }

    private void verifyPlayerAndLoadTournament(String uid) {

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!"player".equals(document.getString("role"))) {
                        Toast.makeText(this,
                                "Player access required",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    checkExistingResult(uid);
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void checkExistingResult(String uid) {

        firestore.collection("tournaments")
                .document(tournamentId)
                .collection("results")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {
                        Toast.makeText(this,
                                "You have already completed this tournament",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        loadTournament();
                    }
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void loadTournament() {

        firestore.collection("tournaments")
                .document(tournamentId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        showError("Tournament does not exist");
                        return;
                    }

                    tournamentName = document.getString("name");

                    String startDate = document.getString("startDate");
                    String endDate = document.getString("endDate");

                    String today = new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    ).format(new Date());

                    if (startDate == null || endDate == null ||
                            today.compareTo(startDate) < 0 ||
                            today.compareTo(endDate) > 0) {

                        showError("This tournament is not currently available");
                        return;
                    }

                    if (!Boolean.TRUE.equals(
                            document.getBoolean("questionsGenerated"))) {

                        showError("Questions have not been generated yet");
                        return;
                    }

                    textViewQuizTitle.setText(tournamentName);

                    loadQuestions();
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void loadQuestions() {

        firestore.collection("tournaments")
                .document(tournamentId)
                .collection("questions")
                .orderBy("questionNumber")
                .get()
                .addOnSuccessListener(snapshot -> {

                    questions.clear();

                    for (DocumentSnapshot document : snapshot) {
                        questions.add(document);
                    }

                    if (questions.size() != 10) {
                        showError("This tournament must contain 10 questions");
                        return;
                    }

                    currentQuestionIndex = 0;
                    score = 0;

                    showQuestion();
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void showQuestion() {

        if (currentQuestionIndex >= questions.size()) {
            finishQuiz();
            return;
        }

        answerSubmitted = false;
        selectedAnswer = null;

        DocumentSnapshot question =
                questions.get(currentQuestionIndex);

        textViewQuestionNumber.setText(
                "Question " + (currentQuestionIndex + 1) +
                        " of " + questions.size()
        );

        textViewProgress.setText(
                "Score: " + score + " / 10"
        );

        textViewQuestion.setText(
                decode(question.getString("question"))
        );

        radioGroupAnswers.removeAllViews();
        radioGroupAnswers.clearCheck();

        List<String> answers =
                (List<String>) question.get("answers");

        if (answers == null || answers.isEmpty()) {
            showError("Invalid question answers");
            return;
        }

        for (String answer : answers) {

            RadioButton radioButton = new RadioButton(this);

            radioButton.setText(decode(answer));
            radioButton.setTag(answer);
            radioButton.setTextSize(17);
            radioButton.setPadding(8, 12, 8, 12);

            radioGroupAnswers.addView(radioButton);
        }

        buttonNext.setEnabled(true);

        if (currentQuestionIndex == questions.size() - 1) {
            buttonNext.setText("Finish Quiz");
        } else {
            buttonNext.setText("Next Question");
        }
    }

    private void checkAnswer() {

        if (answerSubmitted || quizFinished) {
            return;
        }

        int checkedId = radioGroupAnswers.getCheckedRadioButtonId();

        if (checkedId == -1) {
            Toast.makeText(this,
                    "Please select an answer",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selected =
                findViewById(checkedId);

        selectedAnswer = (String) selected.getTag();

        DocumentSnapshot question =
                questions.get(currentQuestionIndex);

        String correctAnswer =
                question.getString("correctAnswer");

        if (selectedAnswer.equals(correctAnswer)) {
            score++;
        }

        answerSubmitted = true;
        currentQuestionIndex++;

        if (currentQuestionIndex < questions.size()) {
            showQuestion();
        } else {
            finishQuiz();
        }
    }

    private void finishQuiz() {

        if (quizFinished) return;

        quizFinished = true;
        buttonNext.setEnabled(false);

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        Map<String, Object> result = new HashMap<>();

        result.put("playerId", user.getUid());
        result.put("tournamentId", tournamentId);
        result.put("tournamentName", tournamentName);
        result.put("score", score);
        result.put("totalQuestions", 10);
        result.put("completedAt",
                com.google.firebase.firestore.FieldValue.serverTimestamp());

        firestore.collection("tournaments")
                .document(tournamentId)
                .collection("results")
                .document(user.getUid())
                .set(result)
                .addOnSuccessListener(unused -> {

                    new AlertDialog.Builder(this)
                            .setTitle("Quiz Completed!")
                            .setMessage(
                                    "Tournament: " + tournamentName +
                                            "\n\nYour Score: " +
                                            score + " / 10"
                            )
                            .setPositiveButton("Done",
                                    (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {

                    quizFinished = false;
                    buttonNext.setEnabled(true);
                    buttonNext.setText("Retry Saving Result");

                    buttonNext.setOnClickListener(v -> finishQuiz());

                    Toast.makeText(this,
                            "Unable to save score: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private String decode(String text) {
        if (text == null) return "";

        return Html.fromHtml(
                text,
                Html.FROM_HTML_MODE_LEGACY
        ).toString();
    }

    private void showError(String message) {
        Toast.makeText(this,
                message == null ? "Unknown error" : message,
                Toast.LENGTH_LONG).show();
        finish();
    }
}
