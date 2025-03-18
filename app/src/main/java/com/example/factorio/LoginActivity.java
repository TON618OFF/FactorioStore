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

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private TextView registerLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupListeners();
    }

    private void initViews() {
        // Инициализация полей ввода
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);

        // Установка начального состояния
        emailInput.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        passwordInput.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }

    private void setupListeners() {
        // Кнопка входа
        loginButton.setOnClickListener(v -> attemptLogin());

        // Ссылка регистрации
        registerLink.setOnClickListener(v -> navigateToRegister());
    }

    private void attemptLogin() {
        // Сброс ошибок
        emailLayout.setError(null);
        passwordLayout.setError(null);

        // Получение значений
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Валидация
        if (!validateInput(email, password)) {
            return;
        }

        // Попытка входа
        performLogin(email, password);
    }

    private boolean validateInput(String email, String password) {
        boolean isValid = true;

        // Проверка email
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Введите email");
            emailInput.requestFocus();
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Некорректный email");
            emailInput.requestFocus();
            isValid = false;
        }

        // Проверка пароля
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Введите пароль");
            if (isValid) {
                passwordInput.requestFocus();
            }
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Минимум 6 символов");
            if (isValid) {
                passwordInput.requestFocus();
            }
            isValid = false;
        }

        return isValid;
    }

    private void performLogin(String email, String password) {
        // Показываем прогресс
        loginButton.setEnabled(false);
        loginButton.setText("Выполняется вход...");

        // TODO: Здесь будет реализация входа через Firebase Auth
        // Пока имитируем задержку и успешный вход
        loginButton.postDelayed(() -> {
            // После успешного входа
            navigateToMain();
        }, 1500);
    }

    private void showForgotPasswordDialog() {
        Toast.makeText(this, "Функция восстановления пароля в разработке", Toast.LENGTH_SHORT).show();
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