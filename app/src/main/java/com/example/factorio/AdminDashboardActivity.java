package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        MaterialButton productsButton = findViewById(R.id.products_button);
        MaterialButton categoriesButton = findViewById(R.id.categories_button);
        MaterialButton usersButton = findViewById(R.id.users_button);

        // Проверка прав администратора
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists() && Boolean.TRUE.equals(document.getBoolean("isAdmin"))) {
                            // Админ подтверждён
                        } else {
                            Toast.makeText(this, "Доступ только для администраторов", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка проверки прав", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } else {
            Toast.makeText(this, "Войдите в систему", Toast.LENGTH_SHORT).show();
            finish();
        }

        productsButton.setOnClickListener(v -> startActivity(new Intent(this, AdminProductsActivity.class)));
        categoriesButton.setOnClickListener(v -> startActivity(new Intent(this, AdminCategoriesActivity.class)));
        usersButton.setOnClickListener(v -> startActivity(new Intent(this, AdminUsersActivity.class)));
    }
}