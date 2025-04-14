package com.example.factorio;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CategoryProductsActivity - активность для отображения товаров из выбранной категории.
 *
 * Основные функции:
 * - Загрузка товаров, относящихся к выбранной категории, из Firestore.
 * - Отображение товаров в RecyclerView с возможностью отображать избранное.
 * - Синхронизация состояния корзины с отображением товаров.
 *
 * Поля:
 * - RecyclerView categoryProductsRecyclerView: Список товаров.
 * - TextView categoryTitle: Название текущей категории.
 * - FloatingActionButton backFab: Кнопка для возврата к предыдущему экрану.
 * - ProductAdapter productAdapter: Адаптер для управления отображением товаров.
 * - List<Product> productsList: Список товаров для отображения.
 * - FirebaseFirestore db: Ссылка на Firestore для работы с данными.
 * - FirebaseAuth auth: Ссылка на FirebaseAuth для проверки текущего пользователя.
 * - String categoryId: Идентификатор категории товаров.
 * - String categoryName: Название категории товаров.
 * - Set<String> favoriteIds: Множество идентификаторов избранных товаров.
 *
 * Методы:
 * - onCreate(Bundle): Инициализация активности, настройка RecyclerView и загрузка данных.
 * - onDestroy(): Удаление слушателей при уничтожении активности.
 * - onCartChanged(List<CartItem>): Обновление интерфейса при изменении корзины.
 * - loadFavoritesAndProducts(): Загрузка избранных товаров и товаров из категории.
 * - loadProductsByCategory(Set<String>): Загрузка товаров из Firestore для текущей категории.
 *
 * Логика:
 * - При загрузке активности загружается список товаров, относящихся к категории, а также избранные товары для авторизованных пользователей.
 * - Если категория имеет ID "all", загружаются все товары, иначе только товары, относящиеся к категории.
 * - Обновление состояния корзины отображается в реальном времени благодаря CartManager.
 * - Товары отображаются с информацией о наличии, принадлежности к категории и статусе избранного.
 */

public class CategoryProductsActivity extends AppCompatActivity implements CartManager.OnCartChangedListener {
    private static final String TAG = "CategoryProductsActivity";
    private RecyclerView categoryProductsRecyclerView;
    private TextView categoryTitle;
    private FloatingActionButton backFab;
    private ProductAdapter productAdapter;
    private List<Product> productsList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String categoryId;
    private String categoryName;
    private Set<String> favoriteIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_products);

        categoryProductsRecyclerView = findViewById(R.id.category_products_recycler_view);
        categoryTitle = findViewById(R.id.category_title);
        backFab = findViewById(R.id.back_fab);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        categoryId = getIntent().getStringExtra("categoryId");
        categoryName = getIntent().getStringExtra("categoryName");

        Log.d(TAG, "Открыта категория: " + categoryName + ", ID: " + categoryId);

        productsList = new ArrayList<>();
        favoriteIds = new HashSet<>();
        productAdapter = new ProductAdapter(this, productsList);

        categoryProductsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryProductsRecyclerView.setAdapter(productAdapter);

        categoryTitle.setText(categoryName != null ? categoryName : "Товары");

        CartManager.getInstance().addOnCartChangedListener(this);
        CartManager.getInstance().loadCartFromFirestore(items -> {
            Log.d(TAG, "Корзина загружена в CategoryProductsActivity, элементов: " + items.size());
            loadFavoritesAndProducts();
        });

        backFab.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CartManager.getInstance().removeOnCartChangedListener(this);
    }

    @Override
    public void onCartChanged(List<CartItem> cartItems) {
        Log.d(TAG, "Корзина изменилась в CategoryProductsActivity, элементов: " + cartItems.size());
        productAdapter.notifyDataSetChanged();
    }

    private void loadFavoritesAndProducts() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.d(TAG, "Пользователь не авторизован, загружаем товары без избранного");
            loadProductsByCategory(new HashSet<>());
            return;
        }

        String userId = user.getUid();
        db.collection("users").document(userId).collection("favorites")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Ошибка загрузки избранного: " + e.getMessage());
                        Toast.makeText(this, "Ошибка загрузки избранного", Toast.LENGTH_SHORT).show();
                        loadProductsByCategory(new HashSet<>());
                        return;
                    }
                    favoriteIds.clear();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot document : snapshot) {
                            String productId = document.getString("productId");
                            if (productId != null) {
                                favoriteIds.add(productId);
                            }
                        }
                        Log.d(TAG, "Избранное загружено: " + favoriteIds.size() + " элементов");
                    }
                    loadProductsByCategory(favoriteIds);
                });
    }

    private void loadProductsByCategory(Set<String> favoriteIds) {
        Query query;
        if ("all".equals(categoryId)) {
            query = db.collection("products")
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        } else {
            query = db.collection("products")
                    .whereEqualTo("category", categoryId)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }

        query.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Ошибка загрузки товаров для категории " + categoryId + ": " + e.getMessage());
                Toast.makeText(this, "Ошибка загрузки товаров", Toast.LENGTH_SHORT).show();
                return;
            }
            productsList.clear();
            if (snapshot != null) {
                Log.d(TAG, "Получено товаров: " + snapshot.size());
                for (QueryDocumentSnapshot document : snapshot) {
                    Product product = document.toObject(Product.class);
                    if (product != null) {
                        product.setId(document.getId());
                        product.setFavorite(favoriteIds.contains(product.getId()));
                        product.setCategoryName(categoryName != null ? categoryName : "Без категории");
                        Long quantity = document.getLong("quantity");
                        product.setQuantity(quantity != null ? quantity.intValue() : 0);
                        productsList.add(product);
                        Log.d(TAG, "Добавлен товар: " + product.getName() + ", ID: " + product.getId() + ", В наличии: " + product.getQuantity() + ", В корзине: " + CartManager.getInstance().getItemQuantity(product.getId())); // Исправлено: getItemQuantity
                    }
                }
            }
            productAdapter.notifyDataSetChanged();
            Log.d(TAG, "UI обновлён, товаров в списке: " + productsList.size());
        });
    }
}