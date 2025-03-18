package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация BottomNavigationView
        bottomNavigation = findViewById(R.id.bottomNavigationView);

        // Установка начального фрагмента
        if (savedInstanceState == null) {
            showFragment(new MainPage());
        }

        // Обработка кликов по нижнему меню
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                showFragment(new MainPage());
                return true;
            } else if (itemId == R.id.categories) {
                showFragment(new CategoriesPage());
                return true;
            } else if (itemId == R.id.cart) {
                showFragment(new CartPage());
                return true;
            } else if (itemId == R.id.profile) {
                // Запуск ProfileActivity вместо фрагмента
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            } else {
                return false;
            }
        });
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_fragment, fragment)
                .commit();
    }
}