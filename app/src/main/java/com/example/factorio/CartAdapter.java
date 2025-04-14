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