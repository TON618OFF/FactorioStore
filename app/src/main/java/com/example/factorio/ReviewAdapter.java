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