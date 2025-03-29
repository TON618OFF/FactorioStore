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
    private View headerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        FloatingActionButton favoriteFab = findViewById(R.id.favorite_fab);
        bottomNavigation = findViewById(R.id.bottomNavigationView);
        headerLayout = findViewById(R.id.header);

        mainPageFragment = new MainPage();
        cartPageFragment = new CartPageFragment();
        categoriesPage = new CategoriesPage();

        if (savedInstanceState == null) {
            showFragment(mainPageFragment);
        }

        // Оптимизированный поиск
        TextInputEditText searchInput = headerLayout.findViewById(R.id.search_input_edit_text);
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
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_fragment, fragment)
                .commit();
        headerLayout.setVisibility(fragment instanceof MainPage ? View.VISIBLE : View.GONE);
    }
}

