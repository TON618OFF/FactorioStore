package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText emailInput, passwordInput, confirmPasswordInput, nicknameInput, birthdayInput, addressInput;
    private TextInputLayout emailLayout, passwordLayout, confirmPasswordLayout, nicknameLayout, birthdayLayout, addressLayout;
    private MaterialButton registerButton;
    private TextView loginLink;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Инициализация компонентов
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        nicknameLayout = findViewById(R.id.nicknameLayout);
        birthdayLayout = findViewById(R.id.birthdayLayout);
        addressLayout = findViewById(R.id.addressLayout);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        nicknameInput = findViewById(R.id.nicknameInput);
        birthdayInput = findViewById(R.id.birthdayInput);
        addressInput = findViewById(R.id.addressInput);

        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Обработка клика по кнопке регистрации
        registerButton.setOnClickListener(v -> registerUser());

        // Обработка клика по ссылке "Войти"
        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        // Сброс ошибок перед валидацией
        clearErrors();

        // Получение данных из полей
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String nickname = nicknameInput.getText().toString().trim();
        String birthday = birthdayInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();

        // Валидация полей
        if (!validateInputs(email, password, confirmPassword, nickname, birthday, address)) {
            return;
        }

        // Регистрация в Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();

                        // Создание объекта пользователя для Firestore
                        Map<String, Object> user = new HashMap<>();
                        user.put("email", email);
                        user.put("nickname", nickname);
                        user.put("birthday", birthday);
                        user.put("address", address);

                        // Сохранение данных в Firestore
                        db.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Регистрация успешна, войдите в аккаунт", Toast.LENGTH_SHORT).show();
                                    auth.signOut(); // Выход после регистрации
                                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Ошибка сохранения данных: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    // Удаляем пользователя из Auth, если Firestore не записался
                                    auth.getCurrentUser().delete();
                                });
                    } else {
                        Toast.makeText(this, "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInputs(String email, String password, String confirmPassword, String nickname, String birthday, String address) {
        boolean isValid = true;

        // Валидация почты
        if (email.isEmpty()) {
            emailLayout.setError("Введите почту");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Некорректный формат почты");
            isValid = false;
        }

        // Валидация пароля
        if (password.isEmpty()) {
            passwordLayout.setError("Введите пароль");
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Пароль должен содержать минимум 6 символов");
            isValid = false;
        }

        // Валидация подтверждения пароля
        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.setError("Подтвердите пароль");
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            confirmPasswordLayout.setError("Пароли не совпадают");
            isValid = false;
        }

        // Валидация никнейма
        if (nickname.isEmpty()) {
            nicknameLayout.setError("Введите никнейм");
            isValid = false;
        } else if (nickname.length() < 3) {
            nicknameLayout.setError("Никнейм должен содержать минимум 3 символа");
            isValid = false;
        }

        // Валидация даты рождения
        if (birthday.isEmpty()) {
            birthdayLayout.setError("Введите дату рождения");
            isValid = false;
        } else if (!isValidDate(birthday)) {
            birthdayLayout.setError("Некорректный формат даты (ДД.ММ.ГГГГ)");
            isValid = false;
        }

        // Валидация адреса
        if (address.isEmpty()) {
            addressLayout.setError("Введите адрес");
            isValid = false;
        } else if (address.length() < 5) {
            addressLayout.setError("Адрес должен содержать минимум 5 символов");
            isValid = false;
        }

        return isValid;
    }

    private boolean isValidDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        sdf.setLenient(false); // Строгая проверка
        try {
            sdf.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private void clearErrors() {
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);
        nicknameLayout.setError(null);
        birthdayLayout.setError(null);
        addressLayout.setError(null);
    }
}