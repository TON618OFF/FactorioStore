package com.example.factorio;

import android.app.AlertDialog;
import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminPanelActivity extends AppCompatActivity {

    private static final String TAG = "AdminPanelActivity";

    private RecyclerView productsRecyclerView;
    private MaterialButton addProductButton;
    private FirebaseFirestore db;
    private AdminProductAdapter productAdapter;
    private List<Product> productList;

    // Список категорий
    private static final String[] CATEGORY_NAMES = {
            "Космос", "Производство", "Компоненты", "Военная промышленность",
            "Фауна", "Электричество", "Продукты", "Механизмы", "Научные Пакеты"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        db = FirebaseFirestore.getInstance();

        productsRecyclerView = findViewById(R.id.products_recycler_view);
        addProductButton = findViewById(R.id.add_product_button);

        productList = new ArrayList<>();
        productAdapter = new AdminProductAdapter(this, productList, CATEGORY_NAMES);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productsRecyclerView.setAdapter(productAdapter);

        loadProducts();

        addProductButton.setOnClickListener(v -> showAddProductDialog());
    }

    private void loadProducts() {
        db.collection("products")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Ошибка загрузки товаров: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        productList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Product product = new Product();
                            product.setId(doc.getId());
                            product.setName(doc.getString("name"));
                            product.setPrice(doc.getLong("price") != null ? doc.getLong("price").intValue() : 0);
                            product.setImageUrl(doc.getString("imageUrl"));
                            product.setDescription(doc.getString("description"));
                            String categoryId = doc.getString("category");
                            product.setCategory(categoryId);
                            // Устанавливаем текстовое название категории
                            try {
                                int categoryIndex = Integer.parseInt(categoryId);
                                if (categoryIndex >= 0 && categoryIndex < CATEGORY_NAMES.length) {
                                    product.setCategoryName(CATEGORY_NAMES[categoryIndex]);
                                } else {
                                    product.setCategoryName("Неизвестная категория");
                                }
                            } catch (NumberFormatException ex) {
                                product.setCategoryName("Ошибка категории");
                            }
                            product.setQuantity(doc.getLong("quantity") != null ? doc.getLong("quantity").intValue() : 0);
                            productList.add(product);
                        }
                        productAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void showAddProductDialog() {
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

        // Настройка выпадающего списка категорий
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, CATEGORY_NAMES);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String priceStr = priceInput.getText().toString().trim();
            String imageUrl = imageUrlInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            int categoryIndex = categorySpinner.getSelectedItemPosition(); // Индекс выбранной категории
            String categoryId = String.valueOf(categoryIndex); // Преобразуем в строку для Firestore
            String quantityStr = quantityInput.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty() || imageUrl.isEmpty() || description.isEmpty() || quantityStr.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            int price, quantity;
            try {
                price = Integer.parseInt(priceStr);
                quantity = Integer.parseInt(quantityStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Цена и количество должны быть числами", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> productData = new HashMap<>();
            productData.put("name", name);
            productData.put("price", price);
            productData.put("imageUrl", imageUrl);
            productData.put("description", description);
            productData.put("category", categoryId); // Сохраняем числовой ID
            productData.put("quantity", quantity);

            db.collection("products")
                    .add(productData)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Товар добавлен", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка добавления: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }
}