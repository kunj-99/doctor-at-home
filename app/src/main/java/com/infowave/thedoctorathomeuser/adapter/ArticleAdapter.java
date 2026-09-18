package com.infowave.thedoctorathomeuser.adapter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.infowave.thedoctorathomeuser.ArticleItem;
import com.infowave.thedoctorathomeuser.R;

import java.util.List;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder> {

    private Context context;
    private List<ArticleItem> articleList;

    public ArticleAdapter(Context context, List<ArticleItem> articleList) {
        this.context = context;
        this.articleList = articleList;
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        ArticleItem item = articleList.get(position);
        holder.title.setText(item.getTitle());
        holder.subtitle.setText(item.getSubtitle());

        com.infowave.thedoctorathomeuser.network.SlowImageLoader.load(
                holder.image, item.getCover(), R.drawable.plasholder, R.drawable.plaseholder_error);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri pdfUri = Uri.parse(item.getPdf());
                Intent intent = new Intent(Intent.ACTION_VIEW, pdfUri);
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    // Toast.makeText(context, "No application available to view PDF", Toast.LENGTH_SHORT).show();
                    Toast.makeText(context, "No app found to open this file. Please install a PDF viewer to continue.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull ArticleViewHolder holder) {
        com.infowave.thedoctorathomeuser.network.SlowImageLoader.clear(holder.image);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return articleList.size();
    }

    public static class ArticleViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, subtitle;

        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.articleImage);
            title = itemView.findViewById(R.id.articleTitle);
            subtitle = itemView.findViewById(R.id.articleSubtitle);
        }
    }
}

// Last Updated: 2026-09-18 14:00 IST
