package com.example.factorio;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

public class AdminCategoryAdapter extends RecyclerView.Adapter<AdminCategoryAdapter.CategoryViewHolder> {

    private static final String TAG = "AdminCategoryAdapter";
    private Context context;
    private List<Category> categoryList; // Полный список категорий
    private List<Category> filteredCategoryList; // Отфильтрованный список для отображения
    private FirebaseFirestore db;

    public AdminCategoryAdapter(Context context, List<Category> categoryList) {
        this.context = context;
        this.categoryList = new ArrayList<>(categoryList);
        this.filteredCategoryList = new ArrayList<>(categoryList); // Изначально отображаем все категории
        this.db = FirebaseFirestore.getInstance();
    }

    // Метод для обновления полного списка категорий
    public void updateCategoryList(List<Category> newCategoryList) {
        this.categoryList.clear();
        this.categoryList.addAll(newCategoryList);
        this.filteredCategoryList.clear();
        this.filteredCategoryList.addAll(newCategoryList); // Синхронизируем отфильтрованный список
        notifyDataSetChanged();
        Log.d(TAG, "Список категорий обновлён, размер: " + filteredCategoryList.size());
    }

    // Метод для фильтрации по названию
    public void filterByName(String query) {
        filteredCategoryList.clear();
        String lowerQuery = query.trim().toLowerCase();

        if (lowerQuery.isEmpty()) {
            filteredCategoryList.addAll(categoryList); // Если запрос пуст, показываем все категории
        } else {
            for (Category category : categoryList) {
                String name = category.getName() != null ? category.getName().toLowerCase() : "";
                if (name.contains(lowerQuery)) {
                    filteredCategoryList.add(category);
                }
            }
        }
        notifyDataSetChanged();
        Log.d(TAG, "Фильтрация завершена, размер filteredCategoryList: " + filteredCategoryList.size());
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = filteredCategoryList.get(position); // Используем отфильтрованный список
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return filteredCategoryList.size(); // Размер отфильтрованного списка
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private Button updateButton, deleteButton;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.category_name_text);
            updateButton = itemView.findViewById(R.id.update_category_button);
            deleteButton = itemView.findViewById(R.id.delete_category_button);
        }

        public void bind(Category category) {
            nameText.setText(category.getName() != null ? category.getName() : "Без названия");
            updateButton.setOnClickListener(v -> showUpdateCategoryDialog(category));
            deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(category.getId()));
            Log.d(TAG, "Привязка категории: " + category.getName());
        }

        private void showUpdateCategoryDialog(Category category) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_category, null);
            builder.setView(dialogView);

            EditText nameInput = dialogView.findViewById(R.id.category_name_input);
            EditText imageUrlInput = dialogView.findViewById(R.id.category_image_url_input);
            MaterialButton saveButton = dialogView.findViewById(R.id.save_category_button);

            nameInput.setText(category.getName());
            imageUrlInput.setText(category.getImageUrl());

            AlertDialog dialog = builder.create();

            saveButton.setOnClickListener(v -> {
                String name = nameInput.getText().toString().trim();
                String imageUrl = imageUrlInput.getText().toString().trim();
                if (name.isEmpty() || imageUrl.isEmpty()) {
                    Toast.makeText(context, "Введите название и URL изображения", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> categoryData = new HashMap<>();
                categoryData.put("name", name);
                categoryData.put("imageUrl", imageUrl);
                categoryData.put("timestamp", FieldValue.serverTimestamp());

                db.collection("categories").document(category.getId())
                        .set(categoryData)
                        .addOnSuccessListener(aVoid -> {
                            category.setName(name);
                            category.setImageUrl(imageUrl);
                            notifyItemChanged(getAdapterPosition());
                            Toast.makeText(context, "Категория обновлена", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Ошибка обновления категории: " + e.getMessage());
                            Toast.makeText(context, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });

            dialog.show();
        }

        private void showDeleteConfirmationDialog(String categoryId) {
            new AlertDialog.Builder(context)
                    .setTitle("Удаление категории")
                    .setMessage("Вы уверены, что хотите удалить эту категорию?")
                    .setPositiveButton("Да", (dialog, which) -> {
                        db.collection("categories").document(categoryId)
                                .delete()
                                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Категория удалена", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Ошибка удаления категории: " + e.getMessage());
                                    Toast.makeText(context, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Нет", null)
                    .show();
        }
    }
}