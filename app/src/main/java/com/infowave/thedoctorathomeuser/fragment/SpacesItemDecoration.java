package com.infowave.thedoctorathomeuser.fragment;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private final int space;

    public SpacesItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.right = space;

        // Add left margin to first item only
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.left = space;
        }
    }
}
