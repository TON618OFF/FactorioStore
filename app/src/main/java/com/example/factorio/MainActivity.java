package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private FirebaseAuth auth;
    private MainPage mainPageFragment;
    private CategoriesPage categoriesPage;
    private CartPageFragment cartPageFragment;
    private View headerLayout; // Добавляем ссылку на header_layout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        // Проверка авторизации
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Инициализация компонентов
        FloatingActionButton favoriteFab = findViewById(R.id.favorite_fab);
        bottomNavigation = findViewById(R.id.bottomNavigationView);
        headerLayout = findViewById(R.id.header); // Получаем header_layout

        // Инициализация фрагментов
        mainPageFragment = new MainPage();
        cartPageFragment = new CartPageFragment();
        categoriesPage = new CategoriesPage();

        // Показываем MainPage по умолчанию
        if (savedInstanceState == null) {
            showFragment(mainPageFragment);
        }

        // Настройка поиска из header.xml
        TextInputEditText searchInput = findViewById(R.id.header)
                .findViewById(R.id.search_input_edit_text);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                mainPageFragment.searchProducts(query); // Передаём запрос в MainPage
            }
        });

        // Переход в FavoritesActivity через FAB
        favoriteFab.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
        });

        // Обработка переключения фрагментов
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                showFragment(mainPageFragment);
                return true;
            } else if (itemId == R.id.categories) {
                showFragment(categoriesPage);
                return true;
            } else if (itemId == R.id.cart) {
                showFragment(cartPageFragment);
                return true;
            } else if (itemId == R.id.profile) {
                if (auth.getCurrentUser() != null) {
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                } else {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    return false;
                }
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

        // Управление видимостью headerLayout
        if (fragment instanceof MainPage) {
            headerLayout.setVisibility(View.VISIBLE);
        } else {
            headerLayout.setVisibility(View.GONE);
        }
    }
}