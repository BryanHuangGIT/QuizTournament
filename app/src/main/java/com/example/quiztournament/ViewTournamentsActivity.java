package com.example.quiztournament;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ViewTournamentsActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private LinearLayout tournamentContainer;
    private TextView textViewNoTournaments;

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
            Toast.makeText(this, "Please log in first",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadTournaments();
    }

    private void loadTournaments() {

        firestore.collection("tournaments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    tournamentContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        textViewNoTournaments.setVisibility(
                                android.view.View.VISIBLE);
                        return;
                    }

                    textViewNoTournaments.setVisibility(
                            android.view.View.GONE);

                    for (DocumentSnapshot document : querySnapshot) {

                        String name = document.getString("name");
                        String description = document.getString("description");
                        String category = document.getString("category");
                        String difficulty = document.getString("difficulty");
                        String startDate = document.getString("startDate");
                        String endDate = document.getString("endDate");

                        LinearLayout card = new LinearLayout(this);
                        card.setOrientation(LinearLayout.VERTICAL);
                        card.setPadding(24, 24, 24, 24);

                        LinearLayout.LayoutParams cardParams =
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );

                        cardParams.setMargins(0, 0, 0, 24);
                        card.setLayoutParams(cardParams);

                        card.setBackgroundColor(0xFFECEFF1);

                        TextView title = new TextView(this);
                        title.setText(name != null ? name : "Untitled");
                        title.setTextSize(20);
                        title.setTextColor(0xFF111111);
                        title.setTypeface(null, android.graphics.Typeface.BOLD);

                        TextView details = new TextView(this);

                        details.setText(
                                "Description: " + safe(description) +
                                        "\nCategory: " + safe(category) +
                                        "\nDifficulty: " + safe(difficulty) +
                                        "\nStart Date: " + safe(startDate) +
                                        "\nEnd Date: " + safe(endDate)
                        );

                        details.setTextSize(15);
                        details.setTextColor(0xFF333333);
                        details.setPadding(0, 12, 0, 0);

                        card.addView(title);
                        card.addView(details);

                        tournamentContainer.addView(card);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to load tournaments: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
