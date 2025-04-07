package com.example.factorio;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    private static final String TAG = "AdminUserAdapter";
    private Context context;
    private List<User> userList;
    private FirebaseFirestore db;

    public AdminUserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateUserList(List<User> newUserList) {
        this.userList.clear();
        this.userList.addAll(newUserList);
        notifyDataSetChanged();
        Log.d(TAG, "Список пользователей в адаптере обновлён, размер: " + userList.size());
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView emailText, nicknameText, isAdminText;
        private Button editRightsButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            emailText = itemView.findViewById(R.id.user_email_text);
            nicknameText = itemView.findViewById(R.id.user_nickname_text);
            isAdminText = itemView.findViewById(R.id.user_is_admin_text);
            editRightsButton = itemView.findViewById(R.id.edit_rights_button);
        }

        public void bind(User user) {
            emailText.setText(user.getEmail() != null ? user.getEmail() : "Нет email");
            nicknameText.setText(user.getNickname() != null ? user.getNickname() : "Без ника");
            isAdminText.setText(user.isAdmin() ? "Да" : "Нет");
            Log.d(TAG, "Привязка данных: " + user.getEmail() + ", isAdmin: " + user.isAdmin());
            editRightsButton.setOnClickListener(v -> showEditRightsDialog(user));
        }

        private void showEditRightsDialog(User user) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_user_rights, null);
            builder.setView(dialogView);

            Spinner isAdminSpinner = dialogView.findViewById(R.id.is_admin_spinner);
            MaterialButton saveButton = dialogView.findViewById(R.id.save_rights_button);

            ArrayAdapter<String> adminAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, new String[]{"Нет", "Да"});
            adminAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            isAdminSpinner.setAdapter(adminAdapter);
            isAdminSpinner.setSelection(user.isAdmin() ? 1 : 0);

            AlertDialog dialog = builder.create();

            saveButton.setOnClickListener(v -> {
                boolean isAdmin = isAdminSpinner.getSelectedItemPosition() == 1;
                Map<String, Object> userData = new HashMap<>();
                userData.put("isAdmin", isAdmin);

                db.collection("users").document(user.getId())
                        .update(userData)
                        .addOnSuccessListener(aVoid -> {
                            user.setAdmin(isAdmin);
                            Toast.makeText(context, "Права обновлены", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Права обновлены в Firestore для " + user.getEmail() + ": isAdmin = " + isAdmin);
                            // Принудительно обновляем список
                            if (context instanceof AdminUsersActivity) {
                                ((AdminUsersActivity) context).loadUsers();
                            }
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Ошибка обновления прав: " + e.getMessage());
                            Toast.makeText(context, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });

            dialog.show();
        }
    }
}