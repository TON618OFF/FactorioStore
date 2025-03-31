package com.example.factorio;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrdersHistoryActivity extends AppCompatActivity {

    private static final String TAG = "OrdersHistoryActivity";

    private RecyclerView ordersRecyclerView;
    private OrdersHistoryAdapter ordersAdapter;
    private List<Order> ordersList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders_history);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ordersRecyclerView = findViewById(R.id.orders_recycler_view);
        ordersList = new ArrayList<>();
        ordersAdapter = new OrdersHistoryAdapter(ordersList);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ordersRecyclerView.setAdapter(ordersAdapter);

        loadOrdersHistory();
    }

    private void loadOrdersHistory() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Войдите, чтобы просмотреть историю заказов", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = user.getUid();
        db.collection("users").document(userId).collection("orders_history")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Сортировка по убыванию времени
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Ошибка загрузки истории заказов: " + e.getMessage());
                        Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        ordersList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Order order = doc.toObject(Order.class);
                            ordersList.add(order);
                        }
                        ordersAdapter.notifyDataSetChanged();
                    }
                });
    }
}

// Модель данных для заказа

// Адаптер для RecyclerView
