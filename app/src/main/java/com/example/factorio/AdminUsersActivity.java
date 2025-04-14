package com.example.factorio;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
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
    private EditText searchUsersEditText; // Поле для поиска
    private FirebaseFirestore db;
    private AdminUserAdapter userAdapter;
    private List<User> userList;
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        db = FirebaseFirestore.getInstance();
        usersRecyclerView = findViewById(R.id.users_recycler_view);
        searchUsersEditText = findViewById(R.id.search_users_edit_text); // Инициализируем EditText

        userList = new ArrayList<>();
        userAdapter = new AdminUserAdapter(this, userList);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(userAdapter);

        // Добавляем слушатель текста для поиска
        searchUsersEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                userAdapter.filterByNickname(s.toString());
            }
        });

        loadUsers();
    }

    public void loadUsers() {
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
                        // Применяем текущий фильтр после загрузки
                        userAdapter.filterByNickname(searchUsersEditText.getText().toString());
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
            userListener.remove();
        }
    }
}