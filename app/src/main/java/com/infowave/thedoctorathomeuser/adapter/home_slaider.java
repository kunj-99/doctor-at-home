package com.infowave.thedoctorathomeuser.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.infowave.thedoctorathomeuser.R;
import java.util.List;

public class home_slaider extends RecyclerView.Adapter<home_slaider.ImageViewHolder> {

    private List<String> imageUrlList;
    private Context context;

    // Constructor for URLs
    public home_slaider(Context context, List<String> imageUrlList) {
        this.context = context;
        this.imageUrlList = imageUrlList;
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.bannerImage);
        }
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ad_home_slaider, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        String url = imageUrlList.get(position);
        com.infowave.thedoctorathomeuser.network.SlowImageLoader.loadCenterCrop(
                holder.imageView, url, R.drawable.plasholder, R.drawable.plaseholder_error);
    }

    @Override
    public void onViewRecycled(ImageViewHolder holder) {
        com.infowave.thedoctorathomeuser.network.SlowImageLoader.clear(holder.imageView);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return imageUrlList.size();
    }
}

// Last Updated: 2026-09-18 14:00 IST
