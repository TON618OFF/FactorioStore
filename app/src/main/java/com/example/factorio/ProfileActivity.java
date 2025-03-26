package com.example.factorio;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText emailText, nicknameText, birthDateText, addressText, passwordText;
    private TextInputLayout passwordLayout;
    private MaterialButton logoutButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        initViews();
        setupListeners();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserProfile();
    }

    private void initViews() {
        emailText = findViewById(R.id.email_text);
        nicknameText = findViewById(R.id.nickname_text);
        birthDateText = findViewById(R.id.birth_date_text);
        addressText = findViewById(R.id.address_text);
        passwordText = findViewById(R.id.password_text);
        passwordLayout = findViewById(R.id.password_layout);
        logoutButton = findViewById(R.id.logout_button);
    }

    private void setupListeners() {
        passwordLayout.setEndIconOnClickListener(v -> showChangePasswordDialog());
        logoutButton.setOnClickListener(v -> logout());
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



    private void logout() {
        auth.signOut();
        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}