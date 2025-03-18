package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthActivity extends AppCompatActivity {

    private MaterialButton loginButton;
    private MaterialButton registerButton;
    private View supportText;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); // Замените на R.layout.activity_auth, если есть

        auth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
        checkAuthState();
    }

    private void initViews() {
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        supportText = findViewById(R.id.supportText);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> navigateToLogin());
        registerButton.setOnClickListener(v -> navigateToRegister());
        supportText.setOnClickListener(v -> showSupportDialog());
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showSupportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Поддержка")
                .setMessage("Свяжитесь с нами: support@factoriostore.com")
                .setPositiveButton("ОК", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void checkAuthState() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            navigateToMain();
        } else {
            Toast.makeText(this, "Пожалуйста, войдите или зарегистрируйтесь", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAuthState();
    }
}