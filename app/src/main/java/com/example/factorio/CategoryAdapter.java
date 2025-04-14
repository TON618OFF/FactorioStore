package com.example.factorio;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * CategoryAdapter - адаптер для управления отображением списка категорий в RecyclerView.
 *
 * Основные функции:
 * - Отображение карточек категорий с изображением и названием.
 * - Обработка нажатий на карточки для перехода к продуктам внутри категории.
 *
 * Поля:
 * - List<Category> categories: Список категорий для отображения.
 *
 * Методы:
 * - onCreateViewHolder(ViewGroup, int): Создает ViewHolder для элемента категории.
 * - onBindViewHolder(CategoryViewHolder, int): Привязывает данные категории к ViewHolder.
 * - getItemCount(): Возвращает количество категорий в списке.
 *
 * Вложенный класс:
 * - CategoryViewHolder:
 *   - Поля:
 *     - ImageView categoryImage: Изображение категории.
 *     - TextView categoryName: Название категории.
 *   - Методы:
 *     - CategoryViewHolder(View): Инициализирует ViewHolder, связывает элементы интерфейса.
 *
 * Логика:
 * - Каждая карточка категории содержит изображение и название.
 * - При нажатии на карточку происходит переход к активности `CategoryProductsActivity`,
 *   где отображаются продукты, относящиеся к выбранной категории.
 * - Glide используется для загрузки изображений с поддержкой плейсхолдера и обработки ошибок.
 */

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private List<Category> categories;

    public CategoryAdapter(List<Category> categories) {
        this.categories = categories;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.categoryName.setText(category.getName());
        Glide.with(holder.itemView.getContext())
                .load(category.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(holder.categoryImage);

        // Обработка нажатия на карточку категории
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), CategoryProductsActivity.class);
            intent.putExtra("categoryId", category.getId());
            intent.putExtra("categoryName", category.getName());
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryImage;
        TextView categoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.category_image);
            categoryName = itemView.findViewById(R.id.category_name);
        }
    }
}