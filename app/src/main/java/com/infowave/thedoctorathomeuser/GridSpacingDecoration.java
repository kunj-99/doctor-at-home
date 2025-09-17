package com.infowave.thedoctorathomeuser;

import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GridSpacingDecoration extends RecyclerView.ItemDecoration {

    private final int space; // in px

    public GridSpacingDecoration(int spaceDp) {
        this.space = (int) (spaceDp * Resources.getSystem().getDisplayMetrics().density);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        int spanCount = 2;

        int column = position % spanCount;
        outRect.left = space - column * space / spanCount;
        outRect.right = (column + 1) * space / spanCount;
        outRect.top = space;
        outRect.bottom = space;
    }
}
