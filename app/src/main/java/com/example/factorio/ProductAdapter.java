package com.example.factorio;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> products;
    private Context context;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public ProductAdapter(Context context, List<Product> products) {
        this.context = context;
        this.products = products;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);

        holder.productName.setText(product.getName());
        holder.productCategory.setText("Категория: " + product.getCategoryName());
        holder.productDescription.setText(product.getDescription());
        holder.productPrice.setText(String.format("%d руб.", product.getPrice()));
        holder.productQuantity.setText("В наличии: " + product.getQuantity());
        Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(holder.productImage);

        holder.favoriteIcon.setImageResource(product.isFavorite() ? R.drawable.favorite_on : R.drawable.favorite);

        holder.favoriteIcon.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Toast.makeText(context, "Войдите, чтобы добавить в избранное", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean newFavoriteStatus = !product.isFavorite();
            product.setFavorite(newFavoriteStatus);
            holder.favoriteIcon.setImageResource(newFavoriteStatus ? R.drawable.favorite_on : R.drawable.favorite);
            String message = newFavoriteStatus ? "Добавлено в избранное" : "Удалено из избранного";
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

            String userId = user.getUid();
            if (newFavoriteStatus) {
                Map<String, Object> favoriteData = new HashMap<>();
                favoriteData.put("productId", product.getId());
                favoriteData.put("addedAt", System.currentTimeMillis());
                db.collection("users").document(userId).collection("favorites").document(product.getId())
                        .set(favoriteData);
            } else {
                db.collection("users").document(userId).collection("favorites").document(product.getId())
                        .delete();
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("productId", product.getId());
            resultIntent.putExtra("isFavorite", newFavoriteStatus);
            if (context instanceof Activity) {
                ((Activity) context).setResult(RESULT_OK, resultIntent);
            }
        });

        if (product.getQuantity() > 0) {
            holder.buyButton.setText("Купить");
            holder.buyButton.setEnabled(true);
            holder.buyButton.setBackgroundTintList(context.getResources().getColorStateList(R.color.circuit_green));
            holder.buyButton.setOnClickListener(v -> {
                CartItem cartItem = new CartItem(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        1,
                        product.getImageUrl()
                );
                CartManager.getInstance().addToCart(cartItem);
                Toast.makeText(context, "Добавлено в корзину: " + product.getName(), Toast.LENGTH_SHORT).show();
            });
        } else {
            holder.buyButton.setText("Нет в наличии");
            holder.buyButton.setTextColor(context.getResources().getColorStateList(R.color.white));
            holder.buyButton.setEnabled(false);
            holder.buyButton.setBackgroundTintList(context.getResources().getColorStateList(R.color.coal_gray));
        }

        holder.detailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailsActivity.class);
            intent.putExtra("productId", product.getId());
            intent.putExtra("categoryName", product.getCategoryName());
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(intent, 1);
            } else {
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage, favoriteIcon;
        TextView productName, productCategory, productDescription, productPrice, productQuantity;
        Button detailsButton, buyButton;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productCategory = itemView.findViewById(R.id.product_category);
            productDescription = itemView.findViewById(R.id.product_description);
            productQuantity = itemView.findViewById(R.id.product_quantity);
            productPrice = itemView.findViewById(R.id.product_price);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
            detailsButton = itemView.findViewById(R.id.details_button);
            buyButton = itemView.findViewById(R.id.buy_button);
        }
    }
}