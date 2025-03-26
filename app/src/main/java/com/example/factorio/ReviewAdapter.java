package com.example.factorio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private List<Review> reviews;

    // Конструктор
    public ReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
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
        holder.nickname.setText(review.nickname);
        holder.reviewText.setText(review.text);
        holder.reviewRating.setText(String.valueOf(review.rating));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    // ViewHolder
    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView nickname, reviewText, reviewRating;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            nickname = itemView.findViewById(R.id.review_user_nickname);
            reviewText = itemView.findViewById(R.id.review_text);
            reviewRating = itemView.findViewById(R.id.review_rating);
        }
    }
}