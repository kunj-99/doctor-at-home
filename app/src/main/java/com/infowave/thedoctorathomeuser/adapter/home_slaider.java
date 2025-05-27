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
        Glide.with(holder.imageView.getContext())
                .load(url)
                .placeholder(R.drawable.plasholder) // Make sure you have this image in your drawables!
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return imageUrlList.size();
    }
}
