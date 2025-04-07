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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminProductsActivity extends AppCompatActivity {

    private RecyclerView productsRecyclerView;
    private MaterialButton addProductButton;
    private FirebaseFirestore db;
    private AdminProductAdapter productAdapter;
    private List<Product> productList;
    private List<Category> categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_products);

        db = FirebaseFirestore.getInstance();
        productsRecyclerView = findViewById(R.id.products_recycler_view);
        addProductButton = findViewById(R.id.add_product_button);

        productList = new ArrayList<>();
        categories = new ArrayList<>();
        productAdapter = new AdminProductAdapter(this, productList, categories);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productsRecyclerView.setAdapter(productAdapter);

        loadCategories();
        loadProducts();

        addProductButton.setOnClickListener(v -> showAddProductDialog());
    }

    private void loadCategories() {
        db.collection("categories")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Ошибка загрузки категорий: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        categories.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Category category = doc.toObject(Category.class);
                            category.setId(doc.getId());
                            categories.add(category);
                        }
                        productAdapter.updateCategories(categories);
                    }
                });
    }

    private void loadProducts() {
        db.collection("products")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
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
                        }
                        productAdapter.notifyDataSetChanged();
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
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }
}