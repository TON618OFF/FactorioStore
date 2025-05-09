package com.example.factorio;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.Locale;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * RegisterActivity - активность для регистрации нового пользователя.
 *
 * Основные функции:
 * - Валидация данных пользователя, таких как почта, пароль, никнейм, дата рождения и адрес.
 * - Проверка уникальности почты и никнейма в Firebase Authentication и Firestore.
 * - Отправка кода подтверждения на почту пользователя.
 * - Регистрация пользователя в Firebase Authentication и сохранение данных профиля в Firestore.
 *
 * Поля:
 * - TextInputEditText: Поля для ввода почты, пароля, подтверждения пароля, никнейма, даты рождения и адреса.
 * - TextInputLayout: Контейнеры для полей с валидацией ввода.
 * - MaterialButton: Кнопка для завершения регистрации.
 * - TextView: Ссылка для перехода на экран входа.
 * - FirebaseAuth auth: Firebase Authentication для работы с пользователями.
 * - FirebaseFirestore db: Firestore для сохранения данных профиля.
 * - String verificationCode: Код подтверждения для верификации почты.
 *
 * Методы:
 * - onCreate(Bundle): Инициализация интерфейса, настройка обработчиков событий.
 * - showDatePickerDialog(): Показывает диалог выбора даты рождения.
 * - initiateRegistration(): Начинает процесс регистрации, включая валидацию и проверку уникальности данных.
 * - sendVerificationCode(String, String, String, String, String): Отправляет код подтверждения на почту.
 * - generateVerificationCode(): Генерирует случайный шестизначный код подтверждения.
 * - sendVerificationEmail(String, String): Отправляет электронное письмо с кодом подтверждения.
 * - showVerificationDialog(String, String, String, String, String): Показывает диалог для ввода кода подтверждения.
 * - registerUser(String, String, String, String, String): Регистрирует пользователя в Firebase Authentication и сохраняет данные профиля в Firestore.
 * - validateInputs(String, String, String, String, String, String): Проверяет корректность введённых данных.
 * - clearErrors(): Сбрасывает ошибки валидации.
 *
 * Логика:
 * - Пользователь вводит данные, которые проверяются на корректность и уникальность.
 * - После валидации отправляется код подтверждения на почту.
 * - Пользователь вводит код подтверждения, после чего данные сохраняются в Firebase Authentication и Firestore.
 * - В случае ошибок отображаются соответствующие сообщения.
 */

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{6,}$");
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[A-Za-z]+[A-Za-z0-9]*$");

    private TextInputEditText emailInput, passwordInput, confirmPasswordInput, nicknameInput, birthdayInput, addressInput;
    private TextInputLayout emailLayout, passwordLayout, confirmPasswordLayout, nicknameLayout, birthdayLayout, addressLayout;
    private MaterialButton registerButton;
    private TextView loginLink;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String verificationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

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

        birthdayInput.setOnClickListener(v -> showDatePickerDialog());
        birthdayInput.setKeyListener(null);

        registerButton.setOnClickListener(v -> initiateRegistration());
        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    String formattedDate = sdf.format(selectedDate.getTime());
                    birthdayInput.setText(formattedDate);
                },
                year, month, day
        );

        Calendar minDate = Calendar.getInstance();
        minDate.set(1950, Calendar.JANUARY, 1);
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        datePickerDialog.show();
    }

    private void initiateRegistration() {
        clearErrors();

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String nickname = nicknameInput.getText().toString().trim();
        String birthday = birthdayInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();

        if (!validateInputs(email, password, confirmPassword, nickname, birthday, address)) {
            return;
        }

        // Сначала проверяем почту в Firebase Authentication
        auth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(emailTask -> {
                    if (emailTask.isSuccessful()) {
                        if (!emailTask.getResult().getSignInMethods().isEmpty()) {
                            // Почта уже зарегистрирована
                            emailLayout.setError("Эта почта уже зарегистрирована");
                            Toast.makeText(this, "Эта почта уже используется. Пожалуйста, войдите в систему.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Если почта свободна, проверяем никнейм в Firestore
                        db.collection("users")
                                .whereEqualTo("nickname", nickname)
                                .get()
                                .addOnCompleteListener(nicknameTask -> {
                                    if (nicknameTask.isSuccessful()) {
                                        QuerySnapshot snapshot = nicknameTask.getResult();
                                        if (!snapshot.isEmpty()) {
                                            nicknameLayout.setError("Этот никнейм уже занят");
                                        } else {
                                            // Никнейм и почта свободны, отправляем код верификации
                                            sendVerificationCode(email, password, nickname, birthday, address);
                                        }
                                    } else {
                                        Toast.makeText(this, "Ошибка проверки никнейма: " + nicknameTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Ошибка проверки почты: " + emailTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendVerificationCode(String email, String password, String nickname, String birthday, String address) {
        verificationCode = generateVerificationCode();
        new Thread(() -> {
            try {
                sendVerificationEmail(email, verificationCode);
                runOnUiThread(() -> showVerificationDialog(email, password, nickname, birthday, address));
            } catch (MessagingException e) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Ошибка отправки кода: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private void sendVerificationEmail(String email, String code) throws MessagingException {
        String host = "smtp.gmail.com";
        final String username = "factoriostore@gmail.com";
        final String password = "perv gdiy fjtb iely";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
        message.setSubject("Код подтверждения регистрации");
        message.setText("Ваш код подтверждения: " + code);

        Transport.send(message);
    }

    private void showVerificationDialog(String email, String password, String nickname, String birthday, String address) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_verification, null);
        builder.setView(dialogView);

        EditText codeInput = dialogView.findViewById(R.id.codeInput);
        TextView errorText = dialogView.findViewById(R.id.errorText);
        MaterialButton confirmButton = dialogView.findViewById(R.id.confirmButton);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        confirmButton.setOnClickListener(v -> {
            String enteredCode = codeInput.getText().toString().trim();
            if (enteredCode.equals(verificationCode)) {
                registerUser(email, password, nickname, birthday, address);
                dialog.dismiss();
            } else {
                errorText.setVisibility(View.VISIBLE);
                errorText.setText("Неверный код подтверждения");
            }
        });

        dialog.show();
    }

    private void registerUser(String email, String password, String nickname, String birthday, String address) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", email);
                            userData.put("nickname", nickname);
                            userData.put("birthday", birthday);
                            userData.put("address", address);
                            userData.put("isAdmin", false);

                            Log.d(TAG, "Подготовленные данные для Firestore: " + userData.toString());

                            db.collection("users").document(userId)
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Пользователь успешно зарегистрирован в Firestore: " + userId);
                                        Toast.makeText(this, "Регистрация успешна", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(this, MainActivity.class)); // Переход на главное окно
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Ошибка записи в Firestore: " + e.getMessage(), e);
                                        Toast.makeText(this, "Ошибка регистрации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        // Удаляем пользователя из Authentication, если Firestore не удалось записать
                                        user.delete();
                                    });
                        } else {
                            Log.e(TAG, "FirebaseUser is null после успешной регистрации");
                            Toast.makeText(this, "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Ошибка создания пользователя в Firebase Auth: " + task.getException().getMessage());
                        Toast.makeText(this, "Ошибка: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInputs(String email, String password, String confirmPassword, String nickname, String birthday, String address) {
        boolean isValid = true;

        if (email.isEmpty()) {
            emailLayout.setError("Введите почту");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Некорректный формат почты");
            isValid = false;
        }

        if (password.isEmpty()) {
            passwordLayout.setError("Введите пароль");
            isValid = false;
        } else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            passwordLayout.setError("Пароль должен содержать минимум 6 символов, заглавную букву, строчную букву, цифру и спецсимвол (!@#$%^&*)");
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.setError("Подтвердите пароль");
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            confirmPasswordLayout.setError("Пароли не совпадают");
            isValid = false;
        }

        if (nickname.isEmpty()) {
            nicknameLayout.setError("Введите никнейм");
            isValid = false;
        } else if (nickname.length() < 3) {
            nicknameLayout.setError("Никнейм должен содержать минимум 3 символа");
            isValid = false;
        } else if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            nicknameLayout.setError("Никнейм должен начинаться с буквы и содержать только буквы и цифры");
            isValid = false;
        }

        if (birthday.isEmpty()) {
            birthdayLayout.setError("Выберите дату рождения");
            isValid = false;
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            try {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.setTime(sdf.parse(birthday));
                Calendar minDate = Calendar.getInstance();
                minDate.set(1950, Calendar.JANUARY, 0);
                if (selectedDate.before(minDate)) {
                    birthdayLayout.setError("Дата рождения должна быть не ранее 01.01.1950");
                    isValid = false;
                }
            } catch (ParseException e) {
                birthdayLayout.setError("Некорректный формат даты");
                isValid = false;
            }
        }

        if (address.isEmpty()) {
            addressLayout.setError("Введите адрес");
            isValid = false;
        } else if (address.length() < 15) {
            addressLayout.setError("Адрес должен содержать минимум 15 символов");
            isValid = false;
        }

        return isValid;
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