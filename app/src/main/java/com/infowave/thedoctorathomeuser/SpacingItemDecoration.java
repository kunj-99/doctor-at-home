package com.infowave.thedoctorathomeuser;

import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SpacingItemDecoration extends RecyclerView.ItemDecoration {
    private final int spacePx;

    public SpacingItemDecoration(int spaceDp) {
        this.spacePx = (int) (spaceDp * (Resources.getSystem().getDisplayMetrics().density));
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect,
                               @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        outRect.left = 0;
        outRect.right = 0;
        outRect.bottom = spacePx;
        if (pos == 0) outRect.top = spacePx;
    }
}
