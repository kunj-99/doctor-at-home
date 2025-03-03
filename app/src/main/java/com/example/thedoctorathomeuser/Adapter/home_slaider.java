package com.example.thedoctorathomeuser.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.thedoctorathomeuser.R;

public class home_slaider extends RecyclerView.Adapter<home_slaider.ImageViewHolder> {

    private int[] imageList;

    // Constructor to receive the image list
    public home_slaider(int[] imageList) {
        this.imageList = imageList;
    }

    // ViewHolder class for images
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.item_image);
        }
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ad_home_slaider, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        holder.imageView.setImageResource(imageList[position]);
    }

    @Override
    public int getItemCount() {
        return imageList.length;
    }
}


