
package com.example.quiztournament;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizQuestion {

    private final String question;
    private final String correctAnswer;
    private final String type;
    private final List<String> answers;

    public QuizQuestion(
            String question,
            String correctAnswer,
            String type,
            List<String> answers
    ) {
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.type = type;

        this.answers = new ArrayList<>(answers);
        Collections.shuffle(this.answers);
    }

    public String getQuestion() {
        return question;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String getType() {
        return type;
    }

    public List<String> getAnswers() {
        return answers;
    }
}
