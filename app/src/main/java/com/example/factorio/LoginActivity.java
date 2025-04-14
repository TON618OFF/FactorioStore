package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.regex.Pattern;

/**
 * LoginActivity - активность для входа в приложение.
 *
 * Основные функции:
 * - Авторизация пользователя с использованием Firebase Authentication.
 * - Валидация ввода email и пароля.
 * - Переход к экрану регистрации или сброса пароля.
 *
 * Поля:
 * - TextInputLayout emailLayout, passwordLayout: Поля ввода для email и пароля.
 * - TextInputEditText emailInput, passwordInput: Текстовые поля для ввода email и пароля.
 * - MaterialButton loginButton: Кнопка для отправки данных авторизации.
 * - TextView registerLink, forgotPasswordLink: Ссылки для перехода на регистрацию и сброс пароля.
 * - FirebaseAuth auth: Объект для работы с Firebase Authentication.
 *
 * Методы:
 * - onCreate(Bundle): Инициализация активности, настройка интерфейса и обработчиков событий.
 * - initViews(): Инициализация элементов интерфейса.
 * - setupListeners(): Настройка обработчиков событий для кнопок и ссылок.
 * - attemptLogin(): Проверяет ввод пользователя и выполняет авторизацию.
 * - validateInput(String, String): Проверяет корректность ввода email и пароля.
 * - performLogin(String, String): Выполняет вход в Firebase Authentication.
 * - showResetEmailDialog(): Показывает диалог для ввода email для сброса пароля.
 * - sendPasswordResetEmail(String, AlertDialog, TextView): Отправляет письмо для сброса пароля.
 * - navigateToRegister(): Переход на экран регистрации.
 * - navigateToMain(): Переход на главный экран приложения после успешного входа.
 *
 * Логика:
 * - Пользователь вводит email и пароль, которые проверяются на корректность.
 * - В случае успешного входа приложение переходит на главный экран.
 * - Если пользователь забыл пароль, он может запросить сброс через email.
 * - Переход на экран регистрации доступен для новых пользователей.
 * - Интерфейс обновляется в зависимости от состояния (например, отключение кнопки входа при выполнении запроса).
 */

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{6,}$");

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private TextView registerLink;
    private TextView forgotPasswordLink;
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
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());
        registerLink.setOnClickListener(v -> navigateToRegister());
        forgotPasswordLink.setOnClickListener(v -> showResetEmailDialog());
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
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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
                        Log.d(TAG, "Login successful for: " + email);
                        Toast.makeText(this, "Вход успешен", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Log.e(TAG, "Login failed: ", task.getException());
                        Toast.makeText(this, "Ошибка входа: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Показать диалог для ввода email
    private void showResetEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reset_password_email, null);
        builder.setView(dialogView);

        TextInputLayout resetEmailLayout = dialogView.findViewById(R.id.resetEmailLayout);
        TextInputEditText resetEmailInput = dialogView.findViewById(R.id.resetEmailInput);
        TextView errorText = dialogView.findViewById(R.id.resetEmailError);
        MaterialButton sendCodeButton = dialogView.findViewById(R.id.sendCodeButton);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(true);

        sendCodeButton.setOnClickListener(v -> {
            String email = resetEmailInput.getText().toString().trim();
            resetEmailLayout.setError(null);
            errorText.setVisibility(View.GONE);

            if (TextUtils.isEmpty(email)) {
                resetEmailLayout.setError("Введите email");
                resetEmailInput.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                resetEmailLayout.setError("Некорректный email");
                resetEmailInput.requestFocus();
                return;
            }

            Log.d(TAG, "Attempting to send reset email to: " + email);
            sendPasswordResetEmail(email, dialog, errorText);
        });

        dialog.show();
    }

    // Отправка письма для сброса пароля
    private void sendPasswordResetEmail(String email, AlertDialog dialog, TextView errorText) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent to: " + email);
                        Toast.makeText(this, "Письмо для сброса пароля отправлено на " + email, Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    } else {
                        Log.e(TAG, "Failed to send reset email: ", task.getException());
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Неизвестная ошибка";
                        if (errorMessage.contains("There is no user record")) {
                            errorText.setVisibility(View.VISIBLE);
                            errorText.setText("Этот email не зарегистрирован");
                        } else {
                            errorText.setVisibility(View.VISIBLE);
                            errorText.setText("Ошибка: " + errorMessage);
                        }
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