package com.example.factorio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainPage extends Fragment {

    private RecyclerView categoriesRecyclerView;
    private RecyclerView productsRecyclerView;
    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;
    private FirebaseFirestore db;
    private List<Category> categoriesList;
    private List<Product> productsList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_page, container, false);

        // Инициализация Firestore
        db = FirebaseFirestore.getInstance();

        // Настройка RecyclerView для категорий (горизонтальный)
        categoriesRecyclerView = view.findViewById(R.id.categories_recycler_view);
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        categoriesList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(categoriesList);
        categoriesRecyclerView.setAdapter(categoryAdapter);

        // Настройка RecyclerView для товаров (вертикальный)
        productsRecyclerView = view.findViewById(R.id.products_recycler_view);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        productsList = new ArrayList<>();
        productAdapter = new ProductAdapter(productsList);
        productsRecyclerView.setAdapter(productAdapter);

        // Загрузка данных из Firestore
        loadCategoriesFromFirestore();
        loadProductsFromFirestore();

        return view;
    }

    private void loadCategoriesFromFirestore() {
        db.collection("categories")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        categoriesList.clear(); // Очищаем список перед загрузкой
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String name = document.getString("name");
                            String imageUrl = document.getString("imageUrl");
                            if (name != null && imageUrl != null) {
                                categoriesList.add(new Category(name, imageUrl));
                            }
                        }
                        // Сортировка по ID документа (0, 1, 2, 3, 4)
                        categoriesList.sort((c1, c2) -> {
                            int id1 = Integer.parseInt(task.getResult().getDocuments().get(categoriesList.indexOf(c1)).getId());
                            int id2 = Integer.parseInt(task.getResult().getDocuments().get(categoriesList.indexOf(c2)).getId());
                            return Integer.compare(id1, id2);
                        });
                        categoryAdapter.notifyDataSetChanged(); // Обновляем адаптер
                    } else {
                        Toast.makeText(getContext(), "Ошибка загрузки категорий: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadProductsFromFirestore() {
        db.collection("products")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        productsList.clear(); // Очищаем список перед загрузкой
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String name = document.getString("name");
                            String description = document.getString("description");
                            Long priceLong = document.getLong("price"); // Price как Long в Firestore
                            String imageUrl = document.getString("imageUrl");
                            if (name != null && description != null && priceLong != null && imageUrl != null) {
                                int price = priceLong.intValue();
                                productsList.add(new Product(name, description, price, imageUrl));
                            }
                        }
                        productAdapter.notifyDataSetChanged(); // Обновляем адаптер
                    } else {
                        Toast.makeText(getContext(), "Ошибка загрузки товаров: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Модель для категории
    public static class Category {
        String name;
        String imageUrl;

        public Category(String name, String imageUrl) {
            this.name = name;
            this.imageUrl = imageUrl;
        }
    }

    // Модель для товара
    public static class Product {
        String name;
        String description;
        int price;
        String imageUrl;
        boolean isFavorite; // Добавлено для "Избранное"

        public Product(String name, String description, int price, String imageUrl) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.imageUrl = imageUrl;
            this.isFavorite = false; // По умолчанию не в избранном
        }
    }

    // Адаптер для товаров
    private static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
        private List<Product> products;

        public ProductAdapter(List<Product> products) {
            this.products = products;
        }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product, parent, false);
            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            Product product = products.get(position);

            // Заполнение данных
            holder.productName.setText(product.name);
            holder.productDescription.setText(product.description);
            holder.productPrice.setText(String.format("%d руб.", product.price));
            Glide.with(holder.itemView.getContext())
                    .load(product.imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(holder.productImage);

            // Логика для "Избранное"
            holder.favoriteIcon.setSelected(product.isFavorite);
            holder.favoriteIcon.setOnClickListener(v -> {
                product.isFavorite = !product.isFavorite;
                holder.favoriteIcon.setSelected(product.isFavorite);
                String message = product.isFavorite ? "Добавлено в избранное" : "Удалено из избранного";
                Toast.makeText(holder.itemView.getContext(), message, Toast.LENGTH_SHORT).show();
            });

            // Обработка нажатий на кнопки
            holder.detailsButton.setOnClickListener(v ->
                    Toast.makeText(holder.itemView.getContext(), "Подробнее о " + product.name, Toast.LENGTH_SHORT).show());

            holder.buyButton.setOnClickListener(v ->
                    Toast.makeText(holder.itemView.getContext(), "Купить " + product.name, Toast.LENGTH_SHORT).show());
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        static class ProductViewHolder extends RecyclerView.ViewHolder {
            ImageView productImage;
            TextView productName;
            TextView productDescription;
            TextView productPrice;
            ImageView favoriteIcon;
            Button detailsButton;
            Button buyButton;

            public ProductViewHolder(@NonNull View itemView) {
                super(itemView);
                productImage = itemView.findViewById(R.id.product_image);
                productName = itemView.findViewById(R.id.product_name);
                productDescription = itemView.findViewById(R.id.product_description);
                productPrice = itemView.findViewById(R.id.product_price);
                favoriteIcon = itemView.findViewById(R.id.favorite_icon);
                detailsButton = itemView.findViewById(R.id.details_button);
                buyButton = itemView.findViewById(R.id.buy_button);
            }
        }
    }
}