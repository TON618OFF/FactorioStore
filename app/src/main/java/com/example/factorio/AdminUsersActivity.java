package com.example.factorio;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminUsersActivity extends AppCompatActivity {

    private static final String TAG = "AdminUsersActivity";
    private RecyclerView usersRecyclerView;
    private FirebaseFirestore db;
    private AdminUserAdapter userAdapter;
    private List<User> userList;
    private ListenerRegistration userListener; // Для управления слушателем

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        db = FirebaseFirestore.getInstance();
        usersRecyclerView = findViewById(R.id.users_recycler_view);

        userList = new ArrayList<>();
        userAdapter = new AdminUserAdapter(this, userList);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(userAdapter);

        loadUsers();
    }

    public void loadUsers() {
        // Удаляем предыдущий слушатель, если он существует
        if (userListener != null) {
            userListener.remove();
        }

        userListener = db.collection("users")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Ошибка загрузки пользователей: " + e.getMessage());
                        Toast.makeText(this, "Ошибка загрузки пользователей: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        List<User> updatedUserList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            User user = doc.toObject(User.class);
                            user.setId(doc.getId());
                            updatedUserList.add(user);
                            Log.d(TAG, "Пользователь загружен: " + user.getEmail() + ", isAdmin: " + user.isAdmin());
                        }
                        userAdapter.updateUserList(updatedUserList);
                        Log.d(TAG, "Список пользователей обновлён, размер: " + updatedUserList.size());
                    } else {
                        Log.w(TAG, "Snapshots равен null");
                        userAdapter.updateUserList(new ArrayList<>());
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) {
            userListener.remove(); // Очищаем слушатель при уничтожении активности
        }
    }
}