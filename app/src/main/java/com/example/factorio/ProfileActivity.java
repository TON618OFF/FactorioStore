package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private MaterialButton loginButton;
    private MaterialButton registerButton;
    private TextView supportText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupListeners();
    }

    private void initViews() {
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        supportText = findViewById(R.id.supportText);
    }

    private void setupListeners() {
        // Обработчик кнопки входа
        loginButton.setOnClickListener(v -> navigateToLogin());

        // Обработчик кнопки регистрации
        registerButton.setOnClickListener(v -> navigateToRegister());

        // Обработчик кнопки поддержки
        supportText.setOnClickListener(v -> showSupportDialog());
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        // Не закрываем активити, чтобы можно было вернуться
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
        // Не закрываем активити, чтобы можно было вернуться
    }

    private void showSupportDialog() {
        Toast.makeText(this, "Служба поддержки в разработке", Toast.LENGTH_SHORT).show();
    }

}