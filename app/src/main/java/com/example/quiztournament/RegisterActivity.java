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

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextConfirmPassword;

    private Button buttonRegister;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);

        buttonRegister = findViewById(R.id.buttonRegister);

        buttonRegister.setOnClickListener(v -> registerPlayer());
    }

    private void registerPlayer() {

        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();
        String confirmPassword = editTextConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(name)) {
            editTextName.setError("Enter your name");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Enter your email");
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Passwords do not match");
            return;
        }

        buttonRegister.setEnabled(false);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = firebaseAuth.getCurrentUser();

                        if (user == null) {
                            buttonRegister.setEnabled(true);
                            Toast.makeText(this,
                                    "Unable to retrieve user account",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        String userId = user.getUid();

                        Map<String, Object> userData = new HashMap<>();

                        userData.put("name", name);
                        userData.put("email", email);
                        userData.put("role", "player");

                        firestore.collection("users")
                                .document(userId)
                                .set(userData)
                                .addOnSuccessListener(unused -> {

                                    buttonRegister.setEnabled(true);

                                    Toast.makeText(this,
                                            "Registration successful! Please log in.",
                                            Toast.LENGTH_LONG).show();

                                    // Sign out the newly registered user
                                    firebaseAuth.signOut();

                                    // Redirect to Home Page
                                    Intent intent = new Intent(
                                            RegisterActivity.this,
                                            MainActivity.class
                                    );

                                    // Clear previous activities
                                    intent.addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    );

                                    startActivity(intent);
                                    finish();
                                })

                                .addOnFailureListener(e -> {

                                    buttonRegister.setEnabled(true);

                                    Toast.makeText(this,
                                            "Account created, but profile could not be saved: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });

                    } else {

                        buttonRegister.setEnabled(true);

                        Toast.makeText(this,
                                "Registration failed: "
                                        + (task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
