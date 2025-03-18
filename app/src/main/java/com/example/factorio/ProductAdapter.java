package com.example.factorio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

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

        // Логика для "Избранного"
        holder.favoriteIcon.setSelected(product.isFavorite); // Начальное состояние из модели
        holder.favoriteIcon.setOnClickListener(v -> {
            product.isFavorite = !product.isFavorite; // Переключаем состояние в модели
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

    // Модель данных для товара
    public static class Product {
        String name;
        String description;
        int price;
        String imageUrl;
        boolean isFavorite; // Добавлено поле для состояния "Избранное"

        public Product(String name, String description, int price, String imageUrl) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.imageUrl = imageUrl;
            this.isFavorite = false; // По умолчанию не в избранном
        }
    }

    // ViewHolder для карточки продукта
    public static class ProductViewHolder extends RecyclerView.ViewHolder {
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