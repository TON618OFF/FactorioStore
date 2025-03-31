package com.example.factorio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

class OrdersHistoryAdapter extends RecyclerView.Adapter<OrdersHistoryAdapter.OrderViewHolder> {

    private List<Order> ordersList;

    public OrdersHistoryAdapter(List<Order> ordersList) {
        this.ordersList = ordersList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_history, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = ordersList.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return ordersList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        private TextView orderIdText, dateText, itemsText, subtotalText, commissionText, totalText, paymentMethodText;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderIdText = itemView.findViewById(R.id.order_id_text);
            dateText = itemView.findViewById(R.id.date_text);
            itemsText = itemView.findViewById(R.id.items_text);
            subtotalText = itemView.findViewById(R.id.subtotal_text);
            commissionText = itemView.findViewById(R.id.commission_text);
            totalText = itemView.findViewById(R.id.total_text);
            paymentMethodText = itemView.findViewById(R.id.payment_method_text);
        }

        public void bind(Order order) {
            orderIdText.setText("Заказ #" + order.getOrderId());

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String date = order.getTimestamp() != null ? sdf.format(order.getTimestamp().toDate()) : "Неизвестно";
            dateText.setText("Дата: " + date);

            StringBuilder itemsBuilder = new StringBuilder();
            for (CartItem item : order.getItems()) {
                itemsBuilder.append(item.getName())
                        .append(" (").append(item.getQuantity()).append(" шт, ")
                        .append(item.getTotalPrice()).append(" руб.)\n");
            }
            itemsText.setText(itemsBuilder.toString().trim());

            subtotalText.setText("Промежуточная сумма: " + order.getSubtotal() + " руб.");
            commissionText.setText(order.getCommission() > 0 ?
                    "Комиссия: " + order.getCommission() + " руб." : "Комиссия: 0 руб.");
            totalText.setText("Итого: " + order.getTotalWithCommission() + " руб.");
            paymentMethodText.setText("Способ оплаты: " + (order.getPaymentMethod().equals("card") ? "Карта" : "Наличные"));
        }
    }
}
