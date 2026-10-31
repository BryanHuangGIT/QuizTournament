package com.example.quiztournament;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class PlayerDashboardActivity extends AppCompatActivity {

    private Button buttonViewTournaments;
    private Button buttonLogout;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_dashboard);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        buttonViewTournaments =
                findViewById(R.id.buttonViewTournaments);

        buttonLogout = findViewById(R.id.buttonLogout);

        buttonViewTournaments.setEnabled(false);

        if (firebaseAuth.getCurrentUser() == null) {
            goToLogin();
            return;
        }

        verifyPlayer();

        buttonViewTournaments.setOnClickListener(v -> {
            startActivity(new Intent(
                    this,
                    PlayerTournamentsActivity.class
            ));
        });

        buttonLogout.setOnClickListener(v -> {
            firebaseAuth.signOut();
            goToLogin();
        });
    }

    private void verifyPlayer() {

        String uid = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if ("player".equals(document.getString("role"))) {
                        buttonViewTournaments.setEnabled(true);
                    } else {
                        Toast.makeText(this,
                                "Player access required",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Unable to verify player",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }
}
