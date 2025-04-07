package com.example.factorio;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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

public class AdminCategoriesActivity extends AppCompatActivity {

    private static final String TAG = "AdminCategoriesActivity";
    private RecyclerView categoriesRecyclerView;
    private MaterialButton addCategoryButton;
    private FirebaseFirestore db;
    private AdminCategoryAdapter categoryAdapter;
    private List<Category> categoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_categories);

        db = FirebaseFirestore.getInstance();
        categoriesRecyclerView = findViewById(R.id.categories_recycler_view);
        addCategoryButton = findViewById(R.id.add_category_button);

        categoryList = new ArrayList<>();
        categoryAdapter = new AdminCategoryAdapter(this, categoryList);
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoriesRecyclerView.setAdapter(categoryAdapter);

        loadCategories();

        addCategoryButton.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void loadCategories() {
        db.collection("categories")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Ошибка загрузки категорий: " + e.getMessage());
                        Toast.makeText(this, "Ошибка загрузки категорий: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        Log.d(TAG, "Получено документов: " + snapshots.size());
                        categoryList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Category category = doc.toObject(Category.class);
                            category.setId(doc.getId());
                            Log.d(TAG, "Категория: " + category.getName() + ", ID: " + category.getId());
                            categoryList.add(category);
                        }
                        categoryAdapter.notifyDataSetChanged();
                        if (categoryList.isEmpty()) {
                            Toast.makeText(this, "Категорий нет в базе данных", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Snapshots равен null");
                    }
                });
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        builder.setView(dialogView);

        EditText nameInput = dialogView.findViewById(R.id.category_name_input);
        EditText imageUrlInput = dialogView.findViewById(R.id.category_image_url_input);
        MaterialButton saveButton = dialogView.findViewById(R.id.save_category_button);

        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String imageUrl = imageUrlInput.getText().toString().trim();
            if (name.isEmpty() || imageUrl.isEmpty()) {
                Toast.makeText(this, "Введите название и URL изображения", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("name", name);
            categoryData.put("imageUrl", imageUrl);
            categoryData.put("timestamp", FieldValue.serverTimestamp());

            db.collection("categories")
                    .add(categoryData)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Категория добавлена", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Ошибка добавления категории: " + e.getMessage());
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }
}