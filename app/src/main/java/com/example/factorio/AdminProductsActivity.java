package com.example.factorio;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminProductsActivity - активность для управления товарами в приложении.
 *
 * Основные функции:
 * - Загрузка списка товаров и категорий из Firestore в реальном времени.
 * - Добавление новых товаров через диалоговое окно.
 * - Обновление списка товаров с поддержкой фильтрации по названию.
 * - Использование RecyclerView для отображения списка товаров с адаптером AdminProductAdapter.
 *
 * Поля:
 * - RecyclerView productsRecyclerView: Отображение списка товаров.
 * - EditText searchProductsEditText: Поле для ввода поискового запроса.
 * - MaterialButton addProductButton: Кнопка для добавления нового товара.
 * - FirebaseFirestore db: База данных Firestore для взаимодействия с товарами и категориями.
 * - AdminProductAdapter productAdapter: Адаптер для управления списком товаров.
 * - List<Product> productList: Полный список товаров.
 * - List<Category> categories: Полный список категорий.
 * - ListenerRegistration productsListener: Слушатель изменений списка товаров в Firestore.
 * - ListenerRegistration categoriesListener: Слушатель изменений списка категорий в Firestore.
 *
 * Методы:
 * - onCreate(Bundle): Инициализация активности, настройка интерфейса и слушателей.
 * - loadCategories(): Загрузка списка категорий из Firestore и обновление в адаптере.
 * - loadProducts(): Загрузка списка товаров из Firestore и обновление в адаптере.
 * - showAddProductDialog(): Отображение диалога для добавления нового товара.
 * - onDestroy(): Очистка слушателей изменений при уничтожении активности.
 *
 * Логика:
 * - Сначала загружаются категории, чтобы их можно было использовать при добавлении и отображении товаров.
 * - После загрузки категорий вызывается метод loadProducts() для загрузки товаров.
 * - Поддерживается фильтрация товаров по названию с помощью TextWatcher.
 * - При добавлении нового товара проверяются поля на заполненность и корректность данных.
 * - Слушатели изменений (ListenerRegistration) удаляются при завершении активности для предотвращения утечек памяти.
 */

public class AdminProductsActivity extends AppCompatActivity {

    private static final String TAG = "AdminProductsActivity";
    private RecyclerView productsRecyclerView;
    private MaterialButton addProductButton;
    private EditText searchProductsEditText;
    private FirebaseFirestore db;
    private AdminProductAdapter productAdapter;
    private List<Product> productList;
    private List<Category> categories;
    private ListenerRegistration productsListener;
    private ListenerRegistration categoriesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_products);

        db = FirebaseFirestore.getInstance();
        productsRecyclerView = findViewById(R.id.products_recycler_view);
        addProductButton = findViewById(R.id.add_product_button);
        searchProductsEditText = findViewById(R.id.search_products_edit_text);

        productList = new ArrayList<>();
        categories = new ArrayList<>();
        productAdapter = new AdminProductAdapter(this, productList, categories);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productsRecyclerView.setAdapter(productAdapter);

        searchProductsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                productAdapter.filterByName(s.toString());
            }
        });

        loadCategories();
        addProductButton.setOnClickListener(v -> showAddProductDialog());
    }

    private void loadCategories() {
        if (categoriesListener != null) {
            categoriesListener.remove();
        }

        categoriesListener = db.collection("categories")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Ошибка загрузки категорий: " + e.getMessage());
                        Toast.makeText(this, "Ошибка загрузки категорий: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        categories.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Category category = doc.toObject(Category.class);
                            category.setId(doc.getId());
                            categories.add(category);
                            Log.d(TAG, "Категория загружена: " + category.getName() + ", ID: " + category.getId());
                        }
                        productAdapter.updateCategories(categories);
                        loadProducts();
                    } else {
                        Log.w(TAG, "Snapshots категорий равен null");
                    }
                });
    }

    private void loadProducts() {
        if (productsListener != null) {
            productsListener.remove();
        }

        productsListener = db.collection("products")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Ошибка загрузки товаров: " + e.getMessage());
                        Toast.makeText(this, "Ошибка загрузки товаров: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        productList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Product product = doc.toObject(Product.class);
                            product.setId(doc.getId());
                            String categoryId = product.getCategory();
                            for (Category category : categories) {
                                if (category.getId().equals(categoryId)) {
                                    product.setCategoryName(category.getName());
                                    break;
                                }
                            }
                            if (product.getCategoryName() == null) {
                                product.setCategoryName("Неизвестная категория");
                            }
                            productList.add(product);
                            Log.d(TAG, "Товар загружен: " + product.getName() + ", ID: " + product.getId());
                        }
                        productAdapter.updateProductList(productList); // Обновляем список без фильтра
                        String searchQuery = searchProductsEditText.getText().toString();
                        if (!searchQuery.isEmpty()) {
                            productAdapter.filterByName(searchQuery); // Применяем фильтр только если есть запрос
                        }
                        Log.d(TAG, "Список товаров обновлён, размер: " + productList.size());
                    } else {
                        Log.w(TAG, "Snapshots товаров равен null");
                    }
                });
    }

    private void showAddProductDialog() {
        if (categories.isEmpty()) {
            Toast.makeText(this, "Нет доступных категорий", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);
        builder.setView(dialogView);

        EditText nameInput = dialogView.findViewById(R.id.product_name_input);
        EditText priceInput = dialogView.findViewById(R.id.product_price_input);
        EditText imageUrlInput = dialogView.findViewById(R.id.product_image_url_input);
        EditText descriptionInput = dialogView.findViewById(R.id.product_description_input);
        Spinner categorySpinner = dialogView.findViewById(R.id.product_category_spinner);
        EditText quantityInput = dialogView.findViewById(R.id.product_quantity_input);
        MaterialButton saveButton = dialogView.findViewById(R.id.save_product_button);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productAdapter.getCategoryNames());
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String priceStr = priceInput.getText().toString().trim();
            String imageUrl = imageUrlInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            int categoryIndex = categorySpinner.getSelectedItemPosition();
            String categoryId = categories.get(categoryIndex).getId();
            String quantityStr = quantityInput.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty() || imageUrl.isEmpty() || description.isEmpty() || quantityStr.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            int price, quantity;
            try {
                price = Integer.parseInt(priceStr);
                quantity = Integer.parseInt(quantityStr);
                if (price <= 0) {
                    Toast.makeText(this, "Цена должна быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Цена и количество должны быть числами", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> productData = new HashMap<>();
            productData.put("name", name);
            productData.put("price", price);
            productData.put("imageUrl", imageUrl);
            productData.put("description", description);
            productData.put("category", categoryId);
            productData.put("quantity", quantity);
            productData.put("timestamp", FieldValue.serverTimestamp());

            db.collection("products")
                    .add(productData)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Товар добавлен", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Ошибка добавления товара: " + e.getMessage());
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (productsListener != null) {
            productsListener.remove();
        }
        if (categoriesListener != null) {
            categoriesListener.remove();
        }
    }
}