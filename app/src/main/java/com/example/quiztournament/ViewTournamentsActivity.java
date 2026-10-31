
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ViewTournamentsActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private LinearLayout tournamentContainer;
    private TextView textViewNoTournaments;

    private boolean isAdmin = false;

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
                                        "\nCategory: " +
                                        safe(document.getString("category")) +
                                        "\nDifficulty: " +
                                        safe(document.getString("difficulty")) +
                                        "\nStart Date: " +
                                        safe(document.getString("startDate")) +
                                        "\nEnd Date: " +
                                        safe(document.getString("endDate"))
                        );

                        details.setTextColor(0xFF333333);
                        details.setTextSize(15);
                        details.setPadding(0, 12, 0, 12);

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
                        card.addView(editButton);
                        card.addView(deleteButton);

                        tournamentContainer.addView(card);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to load tournaments: " +
                                    e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
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
                            .addOnFailureListener(e -> {
                                Toast.makeText(this,
                                        "Delete failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
