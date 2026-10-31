package com.example.quiztournament;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private Button buttonCreateTournament;
    private Button buttonViewTournaments;
    private Button buttonTestOpenTDB;
    private Button buttonLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        buttonCreateTournament =
                findViewById(R.id.buttonCreateTournament);

        buttonViewTournaments =
                findViewById(R.id.buttonViewTournaments);

        buttonTestOpenTDB =
                findViewById(R.id.buttonTestOpenTDB);

        buttonLogout =
                findViewById(R.id.buttonLogout);

        // Disable admin buttons until role verification succeeds
        buttonCreateTournament.setEnabled(false);
        buttonViewTournaments.setEnabled(false);
        buttonTestOpenTDB.setEnabled(false);

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            redirectToHome();
            return;
        }

        // Verify administrator role from Firestore
        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if ("admin".equals(document.getString("role"))) {

                        buttonCreateTournament.setEnabled(true);
                        buttonViewTournaments.setEnabled(true);
                        buttonTestOpenTDB.setEnabled(true);

                    } else {

                        Toast.makeText(
                                this,
                                "Administrator access required",
                                Toast.LENGTH_LONG
                        ).show();

                        firebaseAuth.signOut();
                        redirectToHome();
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Unable to verify administrator",
                            Toast.LENGTH_LONG
                    ).show();

                    redirectToHome();
                });

        // Create Tournament
        buttonCreateTournament.setOnClickListener(v -> {

            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    CreateTournamentActivity.class
            );

            startActivity(intent);
        });

        // View Tournaments
        buttonViewTournaments.setOnClickListener(v -> {

            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    ViewTournamentsActivity.class
            );

            startActivity(intent);
        });

        // Test OpenTDB API
        buttonTestOpenTDB.setOnClickListener(v -> {

            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    OpenTDBTestActivity.class
            );

            startActivity(intent);
        });

        // Logout
        buttonLogout.setOnClickListener(v -> {

            firebaseAuth.signOut();
            redirectToHome();
        });
    }

    private void redirectToHome() {

        Intent intent = new Intent(
                AdminDashboardActivity.this,
                MainActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
