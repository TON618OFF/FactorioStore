package com.example.factorio;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText emailText, nicknameText, birthDateText, addressText, passwordText;
    private TextInputLayout passwordLayout, addressLayout, birthDateLayout, nicknameLayout;
    private MaterialButton logoutButton, favoritesButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        initViews();
        setupListeners();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance();

        loadUserProfile();
    }

    private void initViews() {
        emailText = findViewById(R.id.email_text);
        nicknameText = findViewById(R.id.nickname_text);
        birthDateText = findViewById(R.id.birth_date_text);
        addressText = findViewById(R.id.address_text);
        passwordText = findViewById(R.id.password_text);
        passwordLayout = findViewById(R.id.password_layout);
        addressLayout = findViewById(R.id.address_layout);
        birthDateLayout = findViewById(R.id.birth_date_layout);
        nicknameLayout = findViewById(R.id.nickname_layout);
        logoutButton = findViewById(R.id.logout_button);
        favoritesButton = findViewById(R.id.favorites_button);
    }

    private void setupListeners() {
        passwordLayout.setEndIconOnClickListener(v -> showChangePasswordDialog());
        addressLayout.setEndIconOnClickListener(v -> showChangeAddressDialog());
        birthDateLayout.setEndIconOnClickListener(v -> showDatePickerDialog());
        nicknameLayout.setEndIconOnClickListener(v -> showChangeNicknameDialog());
        logoutButton.setOnClickListener(v -> logout());
        favoritesButton.setOnClickListener(v -> goToFavorites());
    }

    private void loadUserProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            emailText.setText(document.getString("email"));
                            nicknameText.setText(document.getString("nickname"));
                            birthDateText.setText(document.getString("birthday"));
                            addressText.setText(document.getString("address"));
                            passwordText.setText("********");
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка загрузки профиля: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateBirthDate();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateBirthDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String newBirthDate = sdf.format(calendar.getTime());

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            db.collection("users").document(userId)
                    .update("birthday", newBirthDate)
                    .addOnSuccessListener(aVoid -> {
                        birthDateText.setText(newBirthDate);
                        Toast.makeText(this, "Дата рождения обновлена", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка обновления даты: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void showChangeNicknameDialog() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    Long lastChangeTimestamp = document.getLong("lastNicknameChange");
                    long currentTime = System.currentTimeMillis();
                    long oneDayInMillis = 24 * 60 * 60 * 1000; // 24 часа в миллисекундах

                    if (lastChangeTimestamp != null && (currentTime - lastChangeTimestamp < oneDayInMillis)) {
                        long timeLeft = oneDayInMillis - (currentTime - lastChangeTimestamp);
                        int hoursLeft = (int) (timeLeft / (60 * 60 * 1000));
                        int minutesLeft = (int) ((timeLeft % (60 * 60 * 1000)) / (60 * 1000));
                        Toast.makeText(this, "Изменение никнейма доступно через " + hoursLeft + " ч " + minutesLeft + " мин", Toast.LENGTH_LONG).show();
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_nickname, null);
                    builder.setView(dialogView);

                    TextInputEditText newNicknameInput = dialogView.findViewById(R.id.new_nickname_input);
                    MaterialButton saveNicknameButton = dialogView.findViewById(R.id.save_nickname_button);

                    newNicknameInput.setText(nicknameText.getText().toString());

                    AlertDialog dialog = builder.create();

                    saveNicknameButton.setOnClickListener(v -> {
                        String newNickname = newNicknameInput.getText().toString().trim();

                        if (newNickname.isEmpty()) {
                            Toast.makeText(this, "Введите никнейм", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (newNickname.length() < 3) {
                            Toast.makeText(this, "Никнейм должен содержать минимум 3 символа", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("nickname", newNickname);
                        updates.put("lastNicknameChange", System.currentTimeMillis());

                        db.collection("users").document(userId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    nicknameText.setText(newNickname);
                                    Toast.makeText(this, "Никнейм изменён", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка изменения никнейма: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    });

                    dialog.show();
                });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        TextInputEditText oldPasswordInput = dialogView.findViewById(R.id.old_password_input);
        TextInputEditText newPasswordInput = dialogView.findViewById(R.id.new_password_input);
        TextInputEditText confirmNewPasswordInput = dialogView.findViewById(R.id.confirm_new_password_input);
        MaterialButton savePasswordButton = dialogView.findViewById(R.id.save_password_button);

        AlertDialog dialog = builder.create();

        savePasswordButton.setOnClickListener(v -> {
            String oldPassword = oldPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmNewPassword = confirmNewPasswordInput.getText().toString().trim();

            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmNewPassword)) {
                Toast.makeText(this, "Новые пароли не совпадают", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(this, "Новый пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                auth.signInWithEmailAndPassword(user.getEmail(), oldPassword)
                        .addOnSuccessListener(authResult -> {
                            user.updatePassword(newPassword)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Пароль изменён", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка изменения пароля: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Неверный старый пароль", Toast.LENGTH_SHORT).show());
            }
        });

        dialog.show();
    }

    private void showChangeAddressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_address, null);
        builder.setView(dialogView);

        TextInputEditText newAddressInput = dialogView.findViewById(R.id.new_address_input);
        MaterialButton saveAddressButton = dialogView.findViewById(R.id.save_address_button);

        newAddressInput.setText(addressText.getText().toString());

        AlertDialog dialog = builder.create();

        saveAddressButton.setOnClickListener(v -> {
            String newAddress = newAddressInput.getText().toString().trim();

            if (newAddress.isEmpty()) {
                Toast.makeText(this, "Введите адрес", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newAddress.length() < 5) {
                Toast.makeText(this, "Адрес должен содержать минимум 5 символов", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                String userId = user.getUid();
                db.collection("users").document(userId)
                        .update("address", newAddress)
                        .addOnSuccessListener(aVoid -> {
                            addressText.setText(newAddress);
                            Toast.makeText(this, "Адрес изменён", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Ошибка изменения адреса: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        dialog.show();
    }

    private void logout() {
        auth.signOut();
        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void goToFavorites() {
        Intent intent = new Intent(this, FavoritesActivity.class);
        startActivity(intent);
    }
}