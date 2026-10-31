package com.example.quiztournament;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class OpenTDBTestActivity extends AppCompatActivity {

    private LinearLayout questionsContainer;
    private TextView textViewStatus;
    private Button buttonFetch;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opentdb_test);

        questionsContainer =
                findViewById(R.id.questionsContainer);

        textViewStatus =
                findViewById(R.id.textViewStatus);

        buttonFetch =
                findViewById(R.id.buttonFetch);

        firestore = FirebaseFirestore.getInstance();

        buttonFetch.setEnabled(false);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        String uid = FirebaseAuth.getInstance()
                .getCurrentUser().getUid();

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if ("admin".equals(document.getString("role"))) {
                        buttonFetch.setEnabled(true);
                    } else {
                        Toast.makeText(this,
                                "Administrator access required",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> finish());

        buttonFetch.setOnClickListener(v -> fetchQuestions());
    }

    private void fetchQuestions() {

        buttonFetch.setEnabled(false);
        textViewStatus.setText("Loading questions...");
        questionsContainer.removeAllViews();

        OpenTDBApi.fetchQuestions(
                "Science & Nature",
                "Medium",
                "Mixed",
                new OpenTDBApi.QuestionsCallback() {

                    @Override
                    public void onSuccess(
                            List<QuizQuestion> questions) {

                        if (isFinishing() || isDestroyed()) return;

                        textViewStatus.setText(
                                "Loaded " + questions.size() +
                                        " questions"
                        );

                        for (int i = 0;
                             i < questions.size(); i++) {

                            addQuestion(
                                    i + 1,
                                    questions.get(i)
                            );
                        }

                        buttonFetch.setEnabled(true);
                    }

                    @Override
                    public void onError(String message) {

                        if (isFinishing() || isDestroyed()) return;

                        textViewStatus.setText(
                                "Error: " + message
                        );

                        buttonFetch.setEnabled(true);
                    }
                }
        );
    }

    private void addQuestion(
            int number,
            QuizQuestion question
    ) {

        TextView questionText = new TextView(this);

        questionText.setText(
                number + ". " + decode(
                        question.getQuestion()
                )
        );

        questionText.setTextSize(18);
        questionText.setPadding(0, 20, 0, 8);

        questionsContainer.addView(questionText);

        for (String answer : question.getAnswers()) {

            TextView answerText = new TextView(this);

            answerText.setText("• " + decode(answer));
            answerText.setTextSize(16);
            answerText.setPadding(16, 4, 0, 4);

            questionsContainer.addView(answerText);
        }
    }

    private String decode(String text) {
        return Html.fromHtml(
                text,
                Html.FROM_HTML_MODE_LEGACY
        ).toString();
    }
}
