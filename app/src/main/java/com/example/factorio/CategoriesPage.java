package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CategoriesPage extends Fragment {

    private RecyclerView categoriesRecyclerView;
    private CategoryAdapter categoryAdapter;
    private FirebaseFirestore db;
    private List<Category> categoriesList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories_page, container, false);

        // Инициализация Firestore
        db = FirebaseFirestore.getInstance();

        // Настройка RecyclerView
        categoriesRecyclerView = view.findViewById(R.id.categories_recycler_view);
        categoriesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 2 столбца
        categoriesList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(categoriesList);
        categoriesRecyclerView.setAdapter(categoryAdapter);

        // Загрузка данных из Firestore
        loadCategoriesFromFirestore();

        return view;
    }

    private void loadCategoriesFromFirestore() {
        db.collection("categories")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        categoriesList.clear(); // Очищаем список перед загрузкой
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String id = document.getId(); // Получаем ID документа
                            String name = document.getString("name");
                            String imageUrl = document.getString("imageUrl");
                            if (name != null && imageUrl != null) {
                                categoriesList.add(new Category(id, name, imageUrl));
                            }
                        }
                        // Сортировка по ID документа (0, 1, 2, 3, 4)
                        categoriesList.sort((c1, c2) -> {
                            int id1 = Integer.parseInt(task.getResult().getDocuments().get(categoriesList.indexOf(c1)).getId());
                            int id2 = Integer.parseInt(task.getResult().getDocuments().get(categoriesList.indexOf(c2)).getId());
                            return Integer.compare(id1, id2);
                        });
                        categoryAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Ошибка загрузки категорий: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


}