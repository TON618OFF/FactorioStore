package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private TextView registerLink;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        initViews();
        setupListeners();
    }

    private void initViews() {
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());
        registerLink.setOnClickListener(v -> navigateToRegister());
    }

    private void attemptLogin() {
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        performLogin(email, password);
    }

    private boolean validateInput(String email, String password) {
        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Введите email");
            emailInput.requestFocus();
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Некорректный email");
            emailInput.requestFocus();
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Введите пароль");
            if (isValid) passwordInput.requestFocus();
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Минимум 6 символов");
            if (isValid) passwordInput.requestFocus();
            isValid = false;
        }

        return isValid;
    }

    private void performLogin(String email, String password) {
        loginButton.setEnabled(false);
        loginButton.setText("Выполняется вход...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Войти");

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Вход успешен", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Toast.makeText(this, "Ошибка входа: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}