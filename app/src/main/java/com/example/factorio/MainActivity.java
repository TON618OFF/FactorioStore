package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigation;
    private FirebaseAuth auth;
    private MainPage mainPageFragment;
    private CategoriesPage categoriesPage;
    private CartPageFragment cartPageFragment;
    private View headerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Log.d(TAG, "User not authenticated, redirecting to LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Инициализация UI-элементов
        bottomNavigation = findViewById(R.id.bottomNavigationView);
        headerLayout = findViewById(R.id.header);

        if (bottomNavigation == null || headerLayout == null) {
            Log.e(TAG, "BottomNavigationView or headerLayout not found");
            finish();
            return;
        }

        FloatingActionButton favoriteFab = headerLayout.findViewById(R.id.favorite_fab);
        if (favoriteFab == null) {
            Log.e(TAG, "favorite_fab not found in header");
            finish();
            return;
        }

        TextInputEditText searchInput = headerLayout.findViewById(R.id.search_input_edit_text);
        if (searchInput == null) {
            Log.e(TAG, "search_input_edit_text not found in header");
            finish();
            return;
        }

        mainPageFragment = new MainPage();
        cartPageFragment = new CartPageFragment();
        categoriesPage = new CategoriesPage();

        if (savedInstanceState == null) {
            Log.d(TAG, "Showing initial fragment: MainPage");
            showFragment(mainPageFragment);
            bottomNavigation.setSelectedItemId(R.id.home); // Устанавливаем "Главная" активной
        }

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                mainPageFragment.searchProducts(s.toString().trim().toLowerCase());
            }
        });

        favoriteFab.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FavoritesActivity.class)));

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Log.d(TAG, "Navigation item selected: " + itemId);
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
                Intent intent = new Intent(MainActivity.this, auth.getCurrentUser() != null ? ProfileActivity.class : LoginActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        Log.d(TAG, "onCreate completed");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");

        if (auth.getCurrentUser() == null) {
            Log.d(TAG, "User not authenticated in onResume, redirecting to LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Проверяем текущий фрагмент и синхронизируем навигацию
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_fragment);
        if (currentFragment == null || currentFragment instanceof MainPage) {
            Log.d(TAG, "No fragment or MainPage detected, showing MainPage");
            showFragment(mainPageFragment);
            bottomNavigation.setSelectedItemId(R.id.home);
        } else {
            syncNavigationWithFragment(currentFragment);
        }
    }

    private void showFragment(Fragment fragment) {
        if (fragment == null) {
            Log.e(TAG, "Attempted to show null fragment");
            return;
        }
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "Activity is finishing or destroyed, skipping fragment transaction");
            return;
        }
        Log.d(TAG, "Showing fragment: " + fragment.getClass().getSimpleName());
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_fragment, fragment)
                .commitAllowingStateLoss();
        headerLayout.setVisibility(fragment instanceof MainPage ? View.VISIBLE : View.GONE);
        // Убрали syncNavigationWithFragment, чтобы избежать рекурсии
    }

    private void syncNavigationWithFragment(Fragment fragment) {
        if (fragment instanceof MainPage) {
            bottomNavigation.setSelectedItemId(R.id.home);
        } else if (fragment instanceof CategoriesPage) {
            bottomNavigation.setSelectedItemId(R.id.categories);
        } else if (fragment instanceof CartPageFragment) {
            bottomNavigation.setSelectedItemId(R.id.cart);
        }
    }
}