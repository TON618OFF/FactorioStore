package com.example.factorio;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminProductAdapter - адаптер для управления товарами в RecyclerView.
 *
 * Основные функции:
 * - Отображение списка товаров с поддержкой фильтрации по названию.
 * - Обновление и удаление товаров с использованием Firestore.
 * - Редактирование товара через диалоговое окно.
 *
 * Поля:
 * - Context context: Контекст активности, в которой используется адаптер.
 * - List<Product> productList: Полный список товаров.
 * - List<Product> filteredProductList: Отфильтрованный список товаров для отображения.
 * - List<String> categoryNames: Список названий категорий.
 * - List<Category> categories: Полный список категорий.
 * - FirebaseFirestore db: База данных Firestore для взаимодействия с товарами.
 *
 * Методы:
 * - getCategoryNames(): Получение списка названий категорий для отображения в Spinner.
 * - updateCategories(List<Category>): Обновление списка категорий и их названий.
 * - updateProductList(List<Product>): Обновление полного списка товаров в адаптере.
 * - filterByName(String): Фильтрация товаров по названию.
 * - onCreateViewHolder(ViewGroup, int): Создание ViewHolder для отображения элемента списка.
 * - onBindViewHolder(ProductViewHolder, int): Привязка данных товара к ViewHolder.
 * - getItemCount(): Возвращает размер отфильтрованного списка товаров.
 *
 * Вложенный класс:
 * - ProductViewHolder:
 *   - Отображает данные товара (название, цена, URL изображения, описание, категория, количество).
 *   - Предоставляет кнопки для обновления и удаления товара.
 *   - Методы:
 *     - bind(Product): Привязка данных товара к элементу списка.
 *     - showUpdateProductDialog(Product): Отображение диалога для редактирования товара.
 *     - showDeleteConfirmationDialog(String): Отображение диалога для подтверждения удаления товара.
 *
 * Взаимодействие с Firestore:
 * - Обновление и удаление товаров через методы Firestore.
 * - Слушатели успеха и ошибок для отображения сообщений пользователю.
 */

public class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.ProductViewHolder> {

    private static final String TAG = "AdminProductAdapter";
    private Context context;
    private List<Product> productList;
    private List<Product> filteredProductList;
    private List<String> categoryNames;
    private List<Category> categories;
    private FirebaseFirestore db;

    public AdminProductAdapter(Context context, List<Product> productList, List<Category> categories) {
        this.context = context;
        this.productList = new ArrayList<>(productList);
        this.filteredProductList = new ArrayList<>(productList);
        this.categories = categories;
        this.categoryNames = getCategoryNames();
        this.db = FirebaseFirestore.getInstance();
    }

    public List<String> getCategoryNames() {
        List<String> names = new ArrayList<>();
        for (Category category : categories) {
            names.add(category.getName());
        }
        return names;
    }

    public void updateCategories(List<Category> newCategories) {
        this.categories = newCategories;
        this.categoryNames = getCategoryNames();
        notifyDataSetChanged();
    }

    // Обновление списка товаров
    public void updateProductList(List<Product> newProductList) {
        this.productList.clear();
        this.productList.addAll(newProductList);
        this.filteredProductList.clear();
        this.filteredProductList.addAll(newProductList); // Изначально показываем все товары
        notifyDataSetChanged();
        Log.d(TAG, "Список товаров обновлён в адаптере, размер: " + filteredProductList.size());
    }

    public void filterByName(String query) {
        filteredProductList.clear();
        String lowerQuery = query.trim().toLowerCase();

        if (lowerQuery.isEmpty()) {
            filteredProductList.addAll(productList); // Если запрос пуст, показываем все
        } else {
            for (Product product : productList) {
                String name = product.getName() != null ? product.getName().toLowerCase() : "";
                if (name.contains(lowerQuery)) {
                    filteredProductList.add(product);
                }
            }
        }
        notifyDataSetChanged();
        Log.d(TAG, "Фильтрация завершена, размер filteredProductList: " + filteredProductList.size());
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = filteredProductList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return filteredProductList.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText, priceText, imageUrlText, descriptionText, categoryText, quantityText;
        private Button updateButton, deleteButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.product_name_text);
            priceText = itemView.findViewById(R.id.product_price_text);
            imageUrlText = itemView.findViewById(R.id.product_image_url_text);
            descriptionText = itemView.findViewById(R.id.product_description_text);
            categoryText = itemView.findViewById(R.id.product_category_text);
            quantityText = itemView.findViewById(R.id.product_quantity_text);
            updateButton = itemView.findViewById(R.id.update_product_button);
            deleteButton = itemView.findViewById(R.id.delete_product_button);
        }

        public void bind(Product product) {
            nameText.setText(product.getName() != null ? product.getName() : "Без названия");
            priceText.setText(String.valueOf(product.getPrice()));
            imageUrlText.setText(product.getImageUrl() != null ? product.getImageUrl() : "Нет URL");
            descriptionText.setText(product.getDescription() != null ? product.getDescription() : "Без описания");
            categoryText.setText(product.getCategoryName() != null ? product.getCategoryName() : "Неизвестная категория");
            quantityText.setText(String.valueOf(product.getQuantity()));

            updateButton.setOnClickListener(v -> showUpdateProductDialog(product));
            deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(product.getId()));
        }

        private void showUpdateProductDialog(Product product) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_product, null);
            builder.setView(dialogView);

            EditText nameInput = dialogView.findViewById(R.id.product_name_input);
            EditText priceInput = dialogView.findViewById(R.id.product_price_input);
            EditText imageUrlInput = dialogView.findViewById(R.id.product_image_url_input);
            EditText descriptionInput = dialogView.findViewById(R.id.product_description_input);
            Spinner categorySpinner = dialogView.findViewById(R.id.product_category_spinner);
            EditText quantityInput = dialogView.findViewById(R.id.product_quantity_input);
            MaterialButton saveButton = dialogView.findViewById(R.id.save_product_button);

            nameInput.setText(product.getName());
            priceInput.setText(String.valueOf(product.getPrice()));
            imageUrlInput.setText(product.getImageUrl());
            descriptionInput.setText(product.getDescription());
            quantityInput.setText(String.valueOf(product.getQuantity()));

            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, categoryNames);
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            categorySpinner.setAdapter(categoryAdapter);
            int currentCategoryIndex = -1;
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getId().equals(product.getCategory())) {
                    currentCategoryIndex = i;
                    break;
                }
            }
            if (currentCategoryIndex != -1) {
                categorySpinner.setSelection(currentCategoryIndex);
            }

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
                    Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show();
                    return;
                }

                int price, quantity;
                try {
                    price = Integer.parseInt(priceStr);
                    quantity = Integer.parseInt(quantityStr);
                    if (price <= 0) {
                        Toast.makeText(context, "Цена должна быть больше 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Цена и количество должны быть числами", Toast.LENGTH_SHORT).show();
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

                db.collection("products").document(product.getId())
                        .set(productData)
                        .addOnSuccessListener(aVoid -> {
                            product.setName(name);
                            product.setPrice(price);
                            product.setImageUrl(imageUrl);
                            product.setDescription(description);
                            product.setCategory(categoryId);
                            product.setCategoryName(categoryNames.get(categoryIndex));
                            product.setQuantity(quantity);
                            notifyItemChanged(getAdapterPosition());
                            Toast.makeText(context, "Товар обновлён", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Ошибка обновления товара: " + e.getMessage());
                            Toast.makeText(context, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });

            dialog.show();
        }

        private void showDeleteConfirmationDialog(String productId) {
            new AlertDialog.Builder(context)
                    .setTitle("Удаление товара")
                    .setMessage("Вы уверены, что хотите удалить этот товар?")
                    .setPositiveButton("Да", (dialog, which) -> {
                        db.collection("products").document(productId)
                                .delete()
                                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Товар удалён", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Ошибка удаления товара: " + e.getMessage());
                                    Toast.makeText(context, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Нет", null)
                    .show();
        }
    }
}