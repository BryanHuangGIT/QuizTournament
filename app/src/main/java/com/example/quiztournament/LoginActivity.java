package com.example.quiztournament;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        buttonLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Enter email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Enter password");
            return;
        }

        buttonLogin.setEnabled(false);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = firebaseAuth.getCurrentUser();

                        if (user == null) {
                            buttonLogin.setEnabled(true);
                            Toast.makeText(this,
                                    "Unable to retrieve user",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        firestore.collection("users")
                                .document(user.getUid())
                                .get()
                                .addOnSuccessListener(document -> {

                                    buttonLogin.setEnabled(true);

                                    if (!document.exists()) {
                                        firebaseAuth.signOut();
                                        Toast.makeText(this,
                                                "User profile not found",
                                                Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    String role = document.getString("role");

                                    Intent intent;

                                    if ("admin".equals(role)) {

                                        intent = new Intent(
                                                LoginActivity.this,
                                                AdminDashboardActivity.class
                                        );

                                    } else if ("player".equals(role)) {

                                        intent = new Intent(
                                                LoginActivity.this,
                                                PlayerDashboardActivity.class
                                        );

                                    } else {

                                        firebaseAuth.signOut();
                                        Toast.makeText(this,
                                                "Invalid user role",
                                                Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    intent.addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    );

                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {

                                    buttonLogin.setEnabled(true);

                                    Toast.makeText(this,
                                            "Failed to load user profile: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });

                    } else {

                        buttonLogin.setEnabled(true);

                        Toast.makeText(this,
                                "Login failed: "
                                        + (task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
