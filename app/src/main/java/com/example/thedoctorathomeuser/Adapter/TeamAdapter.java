// TeamAdapter.java
package com.example.thedoctorathomeuser.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

import com.example.thedoctorathomeuser.R;
import com.example.thedoctorathomeuser.TeamMember;

public class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.ViewHolder> {

    private final List<TeamMember> team;

    public TeamAdapter(List<TeamMember> team) {
        this.team = team;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TeamMember member = team.get(position);
        holder.name.setText(member.getName());
        holder.role.setText(member.getRole());
        Glide.with(holder.itemView.getContext())
                .load(member.getPhotoRes())
                .circleCrop()
                .into(holder.photo);
    }

    @Override
    public int getItemCount() {
        return team.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView name;
        TextView role;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.memberPhoto);
            name = itemView.findViewById(R.id.memberName);
            role = itemView.findViewById(R.id.memberRole);
        }
    }
}