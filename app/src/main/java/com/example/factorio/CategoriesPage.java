package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CategoriesPage extends Fragment {

    private static final String TAG = "CategoriesPage";
    private RecyclerView categoriesRecyclerView;
    private CategoryAdapter categoryAdapter;
    private FirebaseFirestore db;
    private List<Category> categoriesList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories_page, container, false);

        db = FirebaseFirestore.getInstance();
        categoriesRecyclerView = view.findViewById(R.id.categories_recycler_view);
        categoriesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        categoriesList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(categoriesList);
        categoriesRecyclerView.setAdapter(categoryAdapter);

        loadCategoriesFromFirestore();

        return view;
    }

    private void loadCategoriesFromFirestore() {
        db.collection("categories")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Сортировка по времени добавления
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Ошибка загрузки категорий: " + e.getMessage());
                        Toast.makeText(getContext(), "Ошибка загрузки категорий: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        Log.d(TAG, "Получено категорий: " + snapshots.size());
                        categoriesList.clear();
                        for (QueryDocumentSnapshot document : snapshots) {
                            String id = document.getId();
                            String name = document.getString("name");
                            String imageUrl = document.getString("imageUrl");
                            if (name != null && imageUrl != null) {
                                Category category = new Category(id, name, imageUrl);
                                categoriesList.add(category);
                                Log.d(TAG, "Категория: " + name + ", ID: " + id);
                            }
                        }
                        categoryAdapter.notifyDataSetChanged();
                        if (categoriesList.isEmpty()) {
                            Toast.makeText(getContext(), "Категорий нет в базе данных", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Snapshots равен null");
                    }
                });
    }
}