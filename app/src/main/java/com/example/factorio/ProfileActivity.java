package com.example.factorio;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ProfileActivity - активность для управления профилем пользователя.
 *
 * Основные функции:
 * - Отображение и редактирование профиля пользователя: никнейм, адрес, дата рождения и пароль.
 * - Просмотр и переход к избранным товарам, истории заказов и панели администратора.
 * - Удаление аккаунта с подтверждением пароля.
 * - Выход из аккаунта.
 *
 * Поля:
 * - TextInputEditText: Поля для отображения и редактирования email, никнейма, даты рождения, адреса и пароля.
 * - TextInputLayout: Контейнеры для полей с иконками действий (например, изменение пароля).
 * - MaterialButton: Кнопки для действий (например, выход, переход к избранным товарам, удаление аккаунта).
 * - FirebaseAuth auth: Firebase Authentication для работы с пользователями.
 * - FirebaseFirestore db: Firestore для взаимодействия с данными пользователя.
 * - Calendar calendar: Календарь для выбора даты рождения.
 *
 * Методы:
 * - onCreate(Bundle): Инициализация интерфейса, настройка обработчиков событий и загрузка данных профиля.
 * - initViews(): Инициализация элементов интерфейса.
 * - setupListeners(): Настройка обработчиков событий для кнопок и иконок.
 * - loadUserProfile(): Загружает данные профиля текущего пользователя из Firestore.
 * - showDeleteAccountDialog(): Показывает диалог для подтверждения удаления аккаунта.
 * - deleteAccountWithReauth(String, AlertDialog): Выполняет удаление аккаунта после повторной аутентификации.
 * - showDatePickerDialog(): Показывает диалог выбора даты рождения.
 * - updateBirthDate(): Обновляет дату рождения в Firestore.
 * - showChangeNicknameDialog(): Показывает диалог для изменения никнейма с проверкой уникальности.
 * - showChangePasswordDialog(): Показывает диалог для изменения пароля с валидацией.
 * - showChangeAddressDialog(): Показывает диалог для изменения адреса.
 * - logout(): Выполняет выход из аккаунта.
 * - goToFavorites(): Переход к экрану избранных товаров.
 * - goToOrdersHistory(): Переход к экрану истории заказов.
 * - goToAdminPanel(): Переход к панели администратора.
 *
 * Логика:
 * - Если пользователь не авторизован, он перенаправляется на экран входа.
 * - Данные профиля (email, никнейм, дата рождения, адрес) загружаются из Firestore.
 * - Изменение никнейма доступно не чаще одного раза в сутки.
 * - Удаление аккаунта требует повторной аутентификации паролем.
 * - Кнопка панели администратора видна только для пользователей с правами администратора.
 */

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private TextInputEditText emailText, nicknameText, birthDateText, addressText, passwordText;
    private TextInputLayout passwordLayout, addressLayout, birthDateLayout, nicknameLayout;
    private MaterialButton logoutButton, favoritesButton, ordersHistoryButton, adminPanelButton, deleteAccountButton;
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
        ordersHistoryButton = findViewById(R.id.orders_history_button);
        adminPanelButton = findViewById(R.id.admin_panel_button);
        deleteAccountButton = findViewById(R.id.delete_account_button);
    }

    private void setupListeners() {
        passwordLayout.setEndIconOnClickListener(v -> showChangePasswordDialog());
        addressLayout.setEndIconOnClickListener(v -> showChangeAddressDialog());
        birthDateLayout.setEndIconOnClickListener(v -> showDatePickerDialog());
        nicknameLayout.setEndIconOnClickListener(v -> showChangeNicknameDialog());
        logoutButton.setOnClickListener(v -> logout());
        favoritesButton.setOnClickListener(v -> goToFavorites());
        ordersHistoryButton.setOnClickListener(v -> goToOrdersHistory());
        adminPanelButton.setOnClickListener(v -> goToAdminPanel());
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
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

                            Boolean isAdmin = document.getBoolean("isAdmin");
                            Log.d(TAG, "isAdmin для пользователя " + userId + ": " + isAdmin);
                            if (isAdmin != null && isAdmin) {
                                adminPanelButton.setVisibility(View.VISIBLE);
                            } else {
                                adminPanelButton.setVisibility(View.GONE);
                                if (isAdmin == null) {
                                    db.collection("users").document(userId)
                                            .update("isAdmin", false)
                                            .addOnSuccessListener(aVoid -> Log.d(TAG, "isAdmin установлен в false для " + userId))
                                            .addOnFailureListener(e -> Log.e(TAG, "Ошибка установки isAdmin: " + e.getMessage()));
                                }
                            }
                        } else {
                            Log.w(TAG, "Документ пользователя не существует для " + userId);
                            Toast.makeText(this, "Профиль не найден", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Ошибка загрузки профиля: " + e.getMessage());
                        Toast.makeText(this, "Ошибка загрузки профиля: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.w(TAG, "Пользователь не авторизован");
            Toast.makeText(this, "Пожалуйста, войдите в аккаунт", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Удаление аккаунта");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_account, null);
        builder.setView(dialogView);

        TextInputEditText passwordInput = dialogView.findViewById(R.id.password_input);

        builder.setPositiveButton("Удалить", null); // Устанавливаем null, чтобы перехватить клик вручную
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Перехватываем кнопку "Удалить"
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = passwordInput.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(this, "Введите пароль для подтверждения", Toast.LENGTH_SHORT).show();
            } else {
                deleteAccountWithReauth(password, dialog);
            }
        });
    }

    private void deleteAccountWithReauth(String password, AlertDialog dialog) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            if (email == null) {
                Toast.makeText(this, "Email не найден", Toast.LENGTH_SHORT).show();
                return;
            }

            AuthCredential credential = EmailAuthProvider.getCredential(email, password);
            user.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> {
                        String userId = user.getUid();

                        // Удаление данных из Firestore
                        db.collection("users").document(userId)
                                .delete()
                                .addOnSuccessListener(aVoid1 -> {
                                    Log.d(TAG, "Данные пользователя удалены из Firestore: " + userId);
                                    // Удаление аккаунта из Authentication
                                    user.delete()
                                            .addOnSuccessListener(aVoid2 -> {
                                                Log.d(TAG, "Аккаунт удалён из Authentication: " + userId);
                                                Toast.makeText(this, "Аккаунт успешно удалён", Toast.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                                Intent intent = new Intent(this, LoginActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Ошибка удаления аккаунта из Authentication: " + e.getMessage());
                                                Toast.makeText(this, "Ошибка удаления аккаунта: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Ошибка удаления данных из Firestore: " + e.getMessage());
                                    Toast.makeText(this, "Ошибка удаления данных: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Ошибка повторной аутентификации: " + e.getMessage());
                        Toast.makeText(this, "Неверный пароль: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }
    }

    // Остальные методы остаются без изменений
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
                    long oneDayInMillis = 24 * 60 * 60 * 1000;

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

                        // Проверка уникальности никнейма
                        db.collection("users")
                                .whereEqualTo("nickname", newNickname)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (!querySnapshot.isEmpty()) {
                                        // Никнейм уже существует у другого пользователя
                                        for (var doc : querySnapshot.getDocuments()) {
                                            if (!doc.getId().equals(userId)) {
                                                Toast.makeText(this, "Этот никнейм уже занят", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                        }
                                    }

                                    // Если никнейм уникален или принадлежит текущему пользователю, обновляем
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
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Ошибка проверки уникальности никнейма: " + e.getMessage());
                                    Toast.makeText(this, "Ошибка проверки никнейма: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    });

                    dialog.show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка загрузки данных пользователя: " + e.getMessage());
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void goToOrdersHistory() {
        Intent intent = new Intent(this, OrdersHistoryActivity.class);
        startActivity(intent);
    }

    private void goToAdminPanel() {
        Intent intent = new Intent(this, AdminDashboardActivity.class);
        startActivity(intent);
    }
}