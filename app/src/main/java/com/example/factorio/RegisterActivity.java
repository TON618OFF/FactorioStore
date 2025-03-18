package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextInputLayout emailLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText emailInput, passwordInput, confirmPasswordInput;
    private MaterialButton registerButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();

        Log.d(TAG, "FirebaseAuth initialized: " + (auth != null));
        Log.d(TAG, "FirebaseFirestore initialized: " + (db != null));
    }

    private void initViews() {
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);

        emailInput.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        passwordInput.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        confirmPasswordInput.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }

    private void setupListeners() {
        registerButton.setOnClickListener(v -> registerUser());
        findViewById(R.id.loginLink).setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Валидация
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Введите email");
            emailInput.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Некорректный email");
            emailInput.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Введите пароль");
            passwordInput.requestFocus();
            return;
        }
        if (password.length() < 6) {
            passwordLayout.setError("Пароль должен быть не менее 6 символов");
            passwordInput.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError("Подтвердите пароль");
            confirmPasswordInput.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Пароли не совпадают");
            confirmPasswordInput.requestFocus();
            return;
        }

        registerButton.setEnabled(false);
        registerButton.setText("Регистрация...");

        Log.d(TAG, "Attempting registration with email: " + email);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = auth.getCurrentUser().getUid();
                        Log.d(TAG, "Registration successful, UID: " + uid);

                        // Сохранение данных в Firestore
                        Map<String, Object> user = new HashMap<>();
                        user.put("email", email);
                        user.put("createdAt", System.currentTimeMillis());

                        db.collection("users").document(uid)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User data saved to Firestore for UID: " + uid);
                                    Toast.makeText(RegisterActivity.this,
                                            "Регистрация успешна",
                                            Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to save to Firestore: " + e.getMessage(), e);
                                    Toast.makeText(RegisterActivity.this, "Ошибка сохранения данных: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    // Всё равно переходим, так как аутентификация прошла
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                });
                    } else {
                        registerButton.setEnabled(true);
                        registerButton.setText("Зарегистрироваться");
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Неизвестная ошибка";
                        Log.e(TAG, "Registration failed: " + errorMessage);
                        Toast.makeText(RegisterActivity.this,
                                "Ошибка регистрации: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}