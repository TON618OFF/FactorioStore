package com.example.factorio;

import android.app.AlertDialog;
import android.content.Context;
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

public class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.ProductViewHolder> {

    private Context context;
    private List<Product> productList;
    private List<String> categoryNames;
    private List<Category> categories; // Список категорий
    private FirebaseFirestore db;

    public AdminProductAdapter(Context context, List<Product> productList, List<Category> categories) {
        this.context = context;
        this.productList = productList;
        this.categories = categories;
        this.categoryNames = getCategoryNames();
        this.db = FirebaseFirestore.getInstance();
    }

    // Изменяем модификатор доступа на public
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

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
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
            nameText.setText(product.getName());
            priceText.setText(String.valueOf(product.getPrice()));
            imageUrlText.setText(product.getImageUrl());
            descriptionText.setText(product.getDescription());
            categoryText.setText(product.getCategoryName());
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
                        .addOnFailureListener(e -> Toast.makeText(context, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                                .addOnFailureListener(e -> Toast.makeText(context, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Нет", null)
                    .show();
        }
    }
}