package com.example.factorio;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * CartAdapter - адаптер для отображения и управления элементами корзины в RecyclerView.
 *
 * Основные функции:
 * - Отображение списка товаров в корзине с информацией о названии, изображении, количестве и общей стоимости.
 * - Обновление количества товаров в корзине.
 * - Удаление товаров из корзины.
 * - Переход к деталям продукта при нажатии на изображение товара.
 *
 * Поля:
 * - List<CartItem> cartItems: Список товаров в корзине.
 * - UpdateTotalPriceListener updateListener: Интерфейс для уведомления об обновлении общей стоимости корзины.
 * - Context context: Контекст активности, в которой используется адаптер.
 * - FirebaseFirestore db: Для проверки доступного количества товаров в Firestore.
 *
 * Методы:
 * - updateCartItems(List<CartItem>): Обновление списка товаров в корзине.
 * - onCreateViewHolder(ViewGroup, int): Создание ViewHolder для отображения элемента списка.
 * - onBindViewHolder(CartViewHolder, int): Привязка данных товара к ViewHolder.
 * - getItemCount(): Возвращает количество элементов в корзине.
 *
 * Вложенный класс:
 * - CartViewHolder:
 *   - Отображает данные товара (изображение, имя, количество, общая стоимость).
 *   - Слушатели для кнопок увеличения, уменьшения количества и удаления товара.
 *
 * Интерфейс:
 * - UpdateTotalPriceListener: Используется для уведомления об изменении общей стоимости корзины.
 *
 * Логика:
 * - При увеличении количества товара проверяется доступное количество в Firestore.
 * - При уменьшении количества товара или удалении его из корзины обновляется состояние корзины через CartManager.
 * - При нажатии на изображение товара осуществляется переход на экран с деталями товара.
 * - Glide используется для загрузки изображений товара с поддержкой плейсхолдера.
 */

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private List<CartItem> cartItems;
    private UpdateTotalPriceListener updateListener;
    private Context context;
    private FirebaseFirestore db;

    public CartAdapter(List<CartItem> cartItems, UpdateTotalPriceListener listener) {
        this.cartItems = new ArrayList<>(cartItems);
        this.updateListener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    public void updateCartItems(List<CartItem> newItems) {
        this.cartItems.clear();
        this.cartItems.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);

        holder.itemImage.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailsActivity.class);
            intent.putExtra("productId", item.getProductId());
            context.startActivity(intent);
        });
        holder.itemName.setText(item.getName());
        holder.itemTotalPrice.setText(item.getTotalPrice() + " руб.");
        holder.itemQuantity.setText(String.valueOf(item.getQuantity()));
        Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(holder.itemImage);

        holder.increaseButton.setOnClickListener(v -> {
            db.collection("products").document(item.getProductId())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            int availableQuantity = document.getLong("quantity").intValue();
                            int newQuantity = item.getQuantity() + 1;
                            if (newQuantity <= availableQuantity) {
                                CartManager.getInstance().updateQuantity(item.getProductId(), newQuantity);
                                updateListener.onTotalPriceUpdated();
                            } else {
                                Toast.makeText(context, "Нельзя добавить больше, чем есть в наличии", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "Товар не найден", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Ошибка проверки наличия", Toast.LENGTH_SHORT).show();
                    });
        });

        holder.decreaseButton.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() - 1;
            CartManager.getInstance().updateQuantity(item.getProductId(), newQuantity);
            updateListener.onTotalPriceUpdated();
        });

        holder.removeButton.setOnClickListener(v -> {
            CartManager.getInstance().updateQuantity(item.getProductId(), 0);
            updateListener.onTotalPriceUpdated();
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemName, itemTotalPrice, itemQuantity;
        ImageView decreaseButton, increaseButton;
        ImageButton removeButton;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.cart_item_image);
            itemName = itemView.findViewById(R.id.cart_item_name);
            itemTotalPrice = itemView.findViewById(R.id.cart_item_total_price);
            itemQuantity = itemView.findViewById(R.id.cart_item_quantity);
            decreaseButton = itemView.findViewById(R.id.decrease_quantity_button);
            increaseButton = itemView.findViewById(R.id.increase_quantity_button);
            removeButton = itemView.findViewById(R.id.remove_item_button);
        }
    }

    interface UpdateTotalPriceListener {
        void onTotalPriceUpdated();
    }
}