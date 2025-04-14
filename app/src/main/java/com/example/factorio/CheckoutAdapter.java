package com.example.factorio;

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
 * CheckoutAdapter - адаптер для отображения списка товаров на экране оформления заказа.
 *
 * Основные функции:
 * - Отображение товаров с информацией о названии, изображении, количестве и общей стоимости.
 * - Возможность перехода к деталям товаров при клике на изображение.
 *
 * Поля:
 * - List<CartItem> cartItems: Список товаров в корзине.
 *
 * Методы:
 * - onCreateViewHolder(ViewGroup, int): Создает ViewHolder для элемента списка.
 * - onBindViewHolder(CheckoutViewHolder, int): Привязывает данные товара к ViewHolder.
 * - getItemCount(): Возвращает количество товаров в корзине.
 *
 * Вложенный класс:
 * - CheckoutViewHolder:
 *   - Поля:
 *     - ImageView itemImage: Изображение товара.
 *     - TextView itemName: Название товара.
 *     - TextView itemTotalPrice: Общая стоимость товара.
 *     - TextView itemQuantity: Количество товара.
 *   - Конструктор:
 *     - CheckoutViewHolder(View): Инициализирует ViewHolder и связывает элементы интерфейса.
 *
 * Логика:
 * - Товары отображаются с информацией о названии, количестве и общей стоимости.
 * - Используется Glide для загрузки изображений с поддержкой плейсхолдера.
 * - Клик на изображение товара открывает экран с деталями товара (ProductDetailsActivity).
 */

public class CheckoutAdapter extends RecyclerView.Adapter<CheckoutAdapter.CheckoutViewHolder> {

    private List<CartItem> cartItems;

    public CheckoutAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    @NonNull
    @Override
    public CheckoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_checkout, parent, false);
        return new CheckoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckoutViewHolder holder, int position) {
        CartItem item = cartItems.get(position);

        // Переход к деталям товара при клике на изображение
        holder.itemImage.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ProductDetailsActivity.class);
            intent.putExtra("productId", item.getProductId());
            holder.itemView.getContext().startActivity(intent);
        });
        holder.itemName.setText(item.getName());
        holder.itemTotalPrice.setText(item.getTotalPrice() + " ₽");
        holder.itemQuantity.setText("Количество: " + item.getQuantity());
        Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(holder.itemImage);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CheckoutViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemName, itemTotalPrice, itemQuantity;

        CheckoutViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.checkout_item_image);
            itemName = itemView.findViewById(R.id.checkout_item_name);
            itemTotalPrice = itemView.findViewById(R.id.checkout_item_total_price);
            itemQuantity = itemView.findViewById(R.id.checkout_item_quantity);
        }
    }
}