package com.example.quiztournament;

import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PlayerTournamentsActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private LinearLayout tournamentContainer;
    private TextView textViewNoTournaments;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_tournaments);

        firestore = FirebaseFirestore.getInstance();

        tournamentContainer =
                findViewById(R.id.tournamentContainer);

        textViewNoTournaments =
                findViewById(R.id.textViewNoTournaments);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadTournaments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTournaments();
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

                    String today = dateFormat.format(new Date());

                    for (DocumentSnapshot document : snapshot) {

                        String tournamentId = document.getId();

                        String name = safe(document.getString("name"));
                        String description =
                                safe(document.getString("description"));
                        String category =
                                safe(document.getString("category"));
                        String difficulty =
                                safe(document.getString("difficulty"));
                        String startDate =
                                safe(document.getString("startDate"));
                        String endDate =
                                safe(document.getString("endDate"));

                        String status;

                        if (startDate.equals("-") || endDate.equals("-")) {
                            status = "Unavailable";
                        } else if (today.compareTo(startDate) < 0) {
                            status = "Upcoming";
                        } else if (today.compareTo(endDate) > 0) {
                            status = "Ended";
                        } else {
                            status = "Available";
                        }

                        addTournamentCard(
                                tournamentId,
                                name,
                                description,
                                category,
                                difficulty,
                                startDate,
                                endDate,
                                status
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to load tournaments: " +
                                    e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void addTournamentCard(
            String tournamentId,
            String name,
            String description,
            String category,
            String difficulty,
            String startDate,
            String endDate,
            String status
    ) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(32, 32, 32, 32);
        card.setBackgroundColor(0xFFECEFF1);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 24);
        card.setLayoutParams(cardParams);

        TextView title = new TextView(this);
        title.setText(name);
        title.setTextSize(22);
        title.setTextColor(Color.BLACK);
        title.setTypeface(null, Typeface.BOLD);

        TextView details = new TextView(this);
        details.setText(
                description +
                        "\n\nCategory: " + category +
                        "\nDifficulty: " + difficulty +
                        "\nStart Date: " + startDate +
                        "\nEnd Date: " + endDate
        );
        details.setTextSize(15);
        details.setTextColor(Color.DKGRAY);
        details.setPadding(0, 12, 0, 12);

        TextView statusText = new TextView(this);
        statusText.setText("Status: " + status);
        statusText.setTextSize(16);
        statusText.setTypeface(null, Typeface.BOLD);

        if (status.equals("Available")) {
            statusText.setTextColor(0xFF2E7D32);
        } else if (status.equals("Upcoming")) {
            statusText.setTextColor(0xFF1565C0);
        } else {
            statusText.setTextColor(0xFFC62828);
        }

        Button playButton = new Button(this);
        playButton.setText("Play Quiz");

        boolean available = status.equals("Available");
        playButton.setEnabled(available);

        playButton.setOnClickListener(v -> {

            Toast.makeText(this,
                    "Quiz functionality will be added next!",
                    Toast.LENGTH_SHORT).show();

            // Later:
            // Intent intent = new Intent(this, QuizActivity.class);
            // intent.putExtra("tournamentId", tournamentId);
            // startActivity(intent);
        });

        card.addView(title);
        card.addView(details);
        card.addView(statusText);
        card.addView(playButton);

        tournamentContainer.addView(card);
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
