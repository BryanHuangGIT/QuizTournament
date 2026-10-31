
package com.example.quiztournament;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class OpenTDBApi {

    public interface QuestionsCallback {
        void onSuccess(List<QuizQuestion> questions);
        void onError(String message);
    }

    public static void fetchQuestions(
            String category,
            String difficulty,
            String questionType,
            QuestionsCallback callback
    ) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {
                int categoryId = getCategoryId(category);

                StringBuilder urlBuilder = new StringBuilder(
                        "https://opentdb.com/api.php?amount=10"
                );

                if (categoryId > 0) {
                    urlBuilder.append("&category=").append(categoryId);
                }

                urlBuilder.append("&difficulty=")
                        .append(URLEncoder.encode(
                                difficulty.toLowerCase(
                                        java.util.Locale.US),
                                "UTF-8"
                        ));

                if ("Multiple Choice".equals(questionType)) {
                    urlBuilder.append("&type=multiple");
                } else if ("True / False".equals(questionType)) {
                    urlBuilder.append("&type=boolean");
                }

                URL url = new URL(urlBuilder.toString());

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();

                if (responseCode != 200) {
                    throw new Exception(
                            "HTTP error: " + responseCode);
                }

                InputStream inputStream =
                        connection.getInputStream();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                inputStream, "UTF-8"
                        )
                );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject json =
                        new JSONObject(response.toString());

                int apiResponseCode =
                        json.getInt("response_code");

                if (apiResponseCode != 0) {
                    throw new Exception(
                            "OpenTDB response code: " +
                                    apiResponseCode
                    );
                }

                JSONArray results =
                        json.getJSONArray("results");

                List<QuizQuestion> questions =
                        new ArrayList<>();

                for (int i = 0; i < results.length(); i++) {

                    JSONObject item =
                            results.getJSONObject(i);

                    String question =
                            item.getString("question");

                    String correctAnswer =
                            item.getString("correct_answer");

                    String type =
                            item.getString("type");

                    JSONArray incorrect =
                            item.getJSONArray(
                                    "incorrect_answers");

                    List<String> answers =
                            new ArrayList<>();

                    answers.add(correctAnswer);

                    for (int j = 0;
                         j < incorrect.length(); j++) {

                        answers.add(
                                incorrect.getString(j)
                        );
                    }

                    questions.add(
                            new QuizQuestion(
                                    question,
                                    correctAnswer,
                                    type,
                                    answers
                            )
                    );
                }

                new Handler(Looper.getMainLooper()).post(
                        () -> callback.onSuccess(questions)
                );

            } catch (Exception e) {

                new Handler(Looper.getMainLooper()).post(
                        () -> callback.onError(
                                e.getMessage() == null
                                        ? "Unknown API error"
                                        : e.getMessage()
                        )
                );

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private static int getCategoryId(String category) {

        switch (category) {
            case "General Knowledge": return 9;
            case "Books": return 10;
            case "Film": return 11;
            case "Music": return 12;
            case "Video Games": return 15;
            case "Science & Nature": return 17;
            case "Computers": return 18;
            case "Mathematics": return 19;
            case "Sports": return 21;
            case "Geography": return 22;
            case "History": return 23;
            case "Animals": return 27;
            default: return -1;
        }
    }
}

