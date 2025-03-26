package com.example.factorio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private List<CartItem> cartItems;
    private UpdateTotalPriceListener updateListener;

    public CartAdapter(List<CartItem> cartItems, UpdateTotalPriceListener listener) {
        this.cartItems = cartItems;
        this.updateListener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);

        holder.itemName.setText(item.getName());
        holder.itemTotalPrice.setText(item.getTotalPrice() + " руб.");
        holder.itemQuantity.setText(String.valueOf(item.getQuantity()));
        Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(holder.itemImage);

        holder.decreaseButton.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() - 1;
            CartManager.getInstance().updateQuantity(item.getProductId(), newQuantity);
            updateListener.onTotalPriceUpdated();
        });

        holder.increaseButton.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() + 1;
            CartManager.getInstance().updateQuantity(item.getProductId(), newQuantity);
            updateListener.onTotalPriceUpdated();
        });

        holder.removeButton.setOnClickListener(v -> {
            CartManager.getInstance().updateQuantity(item.getProductId(), 0); // Удаление товара
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
        Button decreaseButton, increaseButton;
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