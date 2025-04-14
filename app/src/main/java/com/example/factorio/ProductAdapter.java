package com.example.factorio;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProductAdapter - адаптер для отображения списка продуктов в RecyclerView.
 *
 * Основные функции:
 * - Отображение информации о продукте: название, категория, описание, цена, количество, статус избранного.
 * - Управление корзиной: добавление, удаление, изменение количества товаров.
 * - Обработка кликов на избранное, детали продукта и кнопки "купить".
 *
 * Поля:
 * - Context context: Контекст для работы с ресурсами и навигацией.
 * - List<Product> productList: Список продуктов для отображения.
 * - CartManager cartManager: Менеджер корзины для синхронизации состояния.
 * - FirebaseFirestore db: Firestore для взаимодействия с базой данных.
 * - FirebaseAuth auth: FirebaseAuth для проверки текущего пользователя.
 * - Map<String, Integer> cartQuantities: Карта для хранения количества товаров в корзине.
 *
 * Конструкторы:
 * - ProductAdapter(Context, List<Product>): Инициализация адаптера с контекстом и списком продуктов.
 *
 * Методы:
 * - loadCartItems(): Загружает текущие товары в корзине и обновляет UI.
 * - onCartChanged(List<CartItem>): Обновляет состояние корзины при изменении.
 * - onCreateViewHolder(ViewGroup, int): Создает ViewHolder для элемента списка.
 * - onBindViewHolder(ProductViewHolder, int): Привязывает данные продукта к ViewHolder.
 * - updateButtonState(ProductViewHolder, int, Product): Обновляет состояние кнопок и UI для продукта.
 * - getItemCount(): Возвращает количество товаров в списке.
 * - onDetachedFromRecyclerView(RecyclerView): Удаляет слушателя изменений корзины при уничтожении адаптера.
 *
 * Вложенный класс:
 * - ProductViewHolder:
 *   - Поля:
 *     - Различные элементы интерфейса, такие как ImageView, TextView, Button, LinearLayout.
 *   - Конструктор:
 *     - ProductViewHolder(View): Инициализирует элементы интерфейса.
 *
 * Логика:
 * - Пользователь может добавлять товары в корзину, изменять их количество или удалять.
 * - Если товара нет в наличии, кнопка "Купить" становится неактивной.
 * - Избранные товары синхронизируются с Firestore.
 * - Клики на детали продукта перенаправляют на экран с дополнительной информацией.
 * - Обновление UI корзины происходит в реальном времени.
 */

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> implements CartManager.OnCartChangedListener {
    private static final String TAG = "ProductAdapter";
    private Context context;
    private List<Product> productList;
    private CartManager cartManager;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Map<String, Integer> cartQuantities;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
        this.cartManager = CartManager.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.cartQuantities = new HashMap<>();
        cartManager.addOnCartChangedListener(this);
        loadCartItems();
    }

    private void loadCartItems() {
        cartManager.loadCartFromFirestore(items -> {
            cartQuantities.clear();
            for (CartItem item : items) {
                cartQuantities.put(item.getProductId(), item.getQuantity());
            }
            Log.d(TAG, "Корзина загружена в адаптере, элементов: " + items.size());
            notifyDataSetChanged();
        });
    }

    @Override
    public void onCartChanged(List<CartItem> cartItems) {
        cartQuantities.clear();
        for (CartItem item : cartItems) {
            cartQuantities.put(item.getProductId(), item.getQuantity());
        }
        Log.d(TAG, "Корзина обновлена в адаптере, элементов: " + cartItems.size());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.productName.setText(product.getName());
        holder.productCategory.setText(product.getCategoryName());
        holder.productDescription.setText(product.getDescription());
        holder.productPrice.setText(String.format("%d руб.", product.getPrice()));
        holder.favoriteIcon.setImageResource(product.isFavorite() ? R.drawable.favorite_on : R.drawable.favorite);
        holder.productQuantity.setText("В наличии: " + product.getQuantity());

        // Загрузка изображения
        Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(holder.productImage);

        // Обновление UI корзины
        Integer cartQuantity = cartQuantities.getOrDefault(product.getId(), 0);
        updateButtonState(holder, cartQuantity, product);

        // Обработчики кликов
        holder.detailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailsActivity.class);
            intent.putExtra("productId", product.getId());
            context.startActivity(intent);
        });

        holder.buyButton.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Toast.makeText(context, "Войдите, чтобы добавить в корзину", Toast.LENGTH_SHORT).show();
                return;
            }
            if (product.getQuantity() <= 0) {
                Toast.makeText(context, "Товара нет в наличии", Toast.LENGTH_SHORT).show();
                return;
            }
            db.collection("products").document(product.getId())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            Long quantity = document.getLong("quantity");
                            int availableQuantity = quantity != null ? quantity.intValue() : 0;
                            if (availableQuantity > 0) {
                                CartItem cartItem = new CartItem(
                                        product.getId(),
                                        product.getName(),
                                        product.getPrice(),
                                        1,
                                        product.getImageUrl()
                                );
                                cartManager.addToCart(cartItem);
                                Toast.makeText(context, product.getName() + " добавлен в корзину", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Товара нет в наличии", Toast.LENGTH_SHORT).show();
                                product.setQuantity(0);
                                notifyItemChanged(position);
                            }
                        } else {
                            Toast.makeText(context, "Товар не найден", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Ошибка проверки наличия", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Ошибка Firestore: " + e.getMessage());
                    });
        });

        holder.increaseButton.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Toast.makeText(context, "Войдите, чтобы изменить количество", Toast.LENGTH_SHORT).show();
                return;
            }
            int currentQuantity = cartQuantities.getOrDefault(product.getId(), 0);
            db.collection("products").document(product.getId())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            Long quantity = document.getLong("quantity");
                            int availableQuantity = quantity != null ? quantity.intValue() : 0;
                            if (currentQuantity < availableQuantity) {
                                cartManager.updateQuantity(product.getId(), currentQuantity + 1);
                            } else {
                                Toast.makeText(context, "Нельзя добавить больше, чем есть в наличии", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "Товар не найден", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Ошибка проверки наличия", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Ошибка Firestore: " + e.getMessage());
                    });
        });

        holder.decreaseButton.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Toast.makeText(context, "Войдите, чтобы изменить количество", Toast.LENGTH_SHORT).show();
                return;
            }
            int currentQuantity = cartQuantities.getOrDefault(product.getId(), 0);
            if (currentQuantity > 0) {
                cartManager.updateQuantity(product.getId(), currentQuantity - 1);
            }
        });

        holder.favoriteIcon.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Toast.makeText(context, "Войдите, чтобы добавить в избранное", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean newFavoriteState = !product.isFavorite();
            product.setFavorite(newFavoriteState);
            holder.favoriteIcon.setImageResource(newFavoriteState ? R.drawable.favorite_on : R.drawable.favorite);
            String userId = user.getUid();
            if (newFavoriteState) {
                Map<String, Object> favoriteData = new HashMap<>();
                favoriteData.put("productId", product.getId());
                favoriteData.put("addedAt", System.currentTimeMillis());
                db.collection("users").document(userId).collection("favorites").document(product.getId())
                        .set(favoriteData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Ошибка добавления в избранное", Toast.LENGTH_SHORT).show();
                            product.setFavorite(false);
                            holder.favoriteIcon.setImageResource(R.drawable.favorite);
                        });
            } else {
                db.collection("users").document(userId).collection("favorites").document(product.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Удалено из избранного", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Ошибка удаления из избранного", Toast.LENGTH_SHORT).show();
                            product.setFavorite(true);
                            holder.favoriteIcon.setImageResource(R.drawable.favorite_on);
                        });
            }
        });
    }

    private void updateButtonState(ProductViewHolder holder, int cartQuantity, Product product) {
        Log.d(TAG, "Обновление UI для товара: " + product.getName() +
                ", в корзине: " + cartQuantity + ", в наличии: " + product.getQuantity());

        if (product.getQuantity() <= 0) {
            holder.buyButton.setVisibility(View.VISIBLE);
            holder.quantityLayout.setVisibility(View.GONE);
            holder.buyButton.setText("Нет в наличии");
            holder.buyButton.setEnabled(false);
            holder.buyButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.coal_gray));
            holder.buyButton.setTextColor(Color.WHITE);
        } else if (cartQuantity > 0) {
            holder.buyButton.setVisibility(View.GONE);
            holder.quantityLayout.setVisibility(View.VISIBLE);
            holder.quantityText.setText(String.valueOf(cartQuantity));
            holder.decreaseButton.setEnabled(cartQuantity > 0);
            holder.increaseButton.setEnabled(cartQuantity < product.getQuantity());
        } else {
            holder.buyButton.setVisibility(View.VISIBLE);
            holder.quantityLayout.setVisibility(View.GONE);
            holder.buyButton.setText("Купить");
            holder.buyButton.setEnabled(true);
            holder.buyButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.circuit_green));
            holder.buyButton.setTextColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        cartManager.removeOnCartChangedListener(this);
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage, favoriteIcon, increaseButton, decreaseButton;
        TextView productName, productCategory, productDescription, productPrice, quantityText, productQuantity;
        Button detailsButton, buyButton;
        LinearLayout quantityLayout;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productCategory = itemView.findViewById(R.id.product_category);
            productDescription = itemView.findViewById(R.id.product_description);
            productPrice = itemView.findViewById(R.id.product_price);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
            detailsButton = itemView.findViewById(R.id.details_button);
            buyButton = itemView.findViewById(R.id.buy_button);
            increaseButton = itemView.findViewById(R.id.increase_quantity_button);
            decreaseButton = itemView.findViewById(R.id.decrease_quantity_button);
            quantityText = itemView.findViewById(R.id.quantity_text);
            quantityLayout = itemView.findViewById(R.id.quantity_layout);
            productQuantity = itemView.findViewById(R.id.product_quantity);
        }
    }
}