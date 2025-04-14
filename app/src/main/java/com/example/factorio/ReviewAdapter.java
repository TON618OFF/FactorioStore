package com.example.factorio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.function.Consumer;

/**
 * ReviewAdapter - адаптер для отображения списка отзывов в RecyclerView.
 *
 * Основные функции:
 * - Отображение отзывов, включая никнейм автора, текст и рейтинг.
 * - Управление отзывами пользователя (редактирование и удаление).
 *
 * Поля:
 * - List<Review> reviews: Список отзывов для отображения.
 * - Consumer<Review> onEditClick: Обработчик клика для редактирования отзыва.
 * - Runnable onDeleteClick: Обработчик клика для удаления отзыва.
 * - String currentUserId: ID текущего пользователя для проверки прав на редактирование и удаление.
 *
 * Конструкторы:
 * - ReviewAdapter(List<Review>, Consumer<Review>, Runnable): Инициализация адаптера с обработчиками событий.
 *
 * Методы:
 * - onCreateViewHolder(ViewGroup, int): Создаёт ViewHolder для элемента списка.
 * - onBindViewHolder(ReviewViewHolder, int): Привязывает данные отзыва к ViewHolder.
 * - getItemCount(): Возвращает количество отзывов в списке.
 *
 * Вложенный класс:
 * - ReviewViewHolder:
 *   - Поля:
 *     - TextView nickname: Поле для отображения никнейма автора.
 *     - TextView reviewText: Поле для отображения текста отзыва.
 *     - TextView reviewRating: Поле для отображения рейтинга отзыва.
 *     - ImageView editIcon: Иконка для редактирования отзыва.
 *     - ImageView deleteIcon: Иконка для удаления отзыва.
 *   - Конструктор:
 *     - ReviewViewHolder(View): Инициализирует элементы интерфейса для отзыва.
 *
 * Логика:
 * - Отзывы отображаются в виде списка, включая никнейм автора, текст и рейтинг.
 * - Иконки редактирования и удаления отображаются только для отзывов текущего пользователя.
 * - При клике на иконку редактирования вызывается onEditClick с текущим отзывом.
 * - При клике на иконку удаления вызывается onDeleteClick.
 */

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private List<Review> reviews;
    private Consumer<Review> onEditClick;
    private Runnable onDeleteClick;
    private String currentUserId;

    public ReviewAdapter(List<Review> reviews, Consumer<Review> onEditClick, Runnable onDeleteClick) {
        this.reviews = reviews;
        this.onEditClick = onEditClick;
        this.onDeleteClick = onDeleteClick;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.nickname.setText(review.getNickname());
        holder.reviewText.setText(review.getText());
        holder.reviewRating.setText(String.valueOf(review.getRating()));

        // Показываем иконки редактирования и удаления только для отзыва текущего пользователя
        boolean isCurrentUserReview = currentUserId != null && currentUserId.equals(review.getUserId());
        holder.editIcon.setVisibility(isCurrentUserReview ? View.VISIBLE : View.GONE);
        holder.deleteIcon.setVisibility(isCurrentUserReview ? View.VISIBLE : View.GONE);

        holder.editIcon.setOnClickListener(v -> onEditClick.accept(review));
        holder.deleteIcon.setOnClickListener(v -> onDeleteClick.run());
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView nickname, reviewText, reviewRating;
        ImageView editIcon, deleteIcon;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            nickname = itemView.findViewById(R.id.review_user_nickname);
            reviewText = itemView.findViewById(R.id.review_text);
            reviewRating = itemView.findViewById(R.id.review_rating);
            editIcon = itemView.findViewById(R.id.edit_review_icon);
            deleteIcon = itemView.findViewById(R.id.delete_review_icon);
        }
    }
}