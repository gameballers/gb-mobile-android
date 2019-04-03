package com.gameball.gameball.views.leaderBoard;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.model.response.PlayerInfo;
import com.gameball.gameball.utils.ImageDownloader;

import java.util.ArrayList;

public class LeaderBoardAdapter extends RecyclerView.Adapter<LeaderBoardAdapter.ItemRowHolder> {
    private Context mContext;
    private ArrayList<PlayerInfo> mData;

    public LeaderBoardAdapter(Context context, ArrayList<PlayerInfo> data) {
        this.mData = data;
        this.mContext = context;
    }

    @Override
    public ItemRowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View row = inflater.inflate(R.layout.leaderboard_item_layout, parent, false);
        ItemRowHolder rowHolder = new ItemRowHolder(row);
        rowHolder.setIsRecyclable(false);
        return rowHolder;
    }

    @Override
    public void onBindViewHolder(ItemRowHolder holder, int position) {
        PlayerInfo item = mData.get(position);

        holder.playerName.setText(item.getName());
        holder.playerCurrentLevelName.setText(item.getLevel().getName());
        holder.frubiesValue.setText(String.format("%d",item.getAccFrubies()));
        if(item.getLevel().getIcon() != null)
            ImageDownloader.downloadImage(mContext, holder.playerLevelLogo,
                    item.getLevel().getIcon().getFileName());
    }

    public void setmData(ArrayList<PlayerInfo> mData)
    {
        this.mData = mData;
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ItemRowHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView playerLevelLogo;
        public TextView playerName;
        public TextView playerCurrentLevelName;
        public TextView frubiesValue;


        public ItemRowHolder(View itemView) {
            super(itemView);
            playerLevelLogo = itemView.findViewById(R.id.player_level_logo);
            playerName = itemView.findViewById(R.id.player_name);
            playerCurrentLevelName = itemView.findViewById(R.id.player_current_level_name);
            frubiesValue = itemView.findViewById(R.id.frubies_for_next_level);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            final int pos = getLayoutPosition();
            int pos1 = getAdapterPosition();
            if (pos == pos1) {

            }
        }
    }
}
