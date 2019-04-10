package com.gameball.gameball.views.profile;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gameball.gameball.R;
import com.gameball.gameball.model.response.Quest;

import java.util.ArrayList;

public class QuestsAdapter extends RecyclerView.Adapter<QuestsAdapter.ItemRowHolder>
{
    private Context mContext;
    private ArrayList<Quest> mData;

    public QuestsAdapter(Context context, ArrayList<Quest> data)
    {
        this.mData = data;
        this.mContext = context;
    }

    @Override
    public ItemRowHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View row = inflater.inflate(R.layout.quest_item_layout, parent, false);
        return new ItemRowHolder(row);
    }

    @Override
    public void onBindViewHolder(ItemRowHolder holder, int position)
    {
        Quest item = mData.get(position);

        holder.questName.setText(item.getQuestName());
    }

    @Override
    public int getItemCount()
    {
        return mData.size();
    }

    public void setmData(ArrayList<Quest> mData)
    {
        this.mData = mData;
    }

    public class ItemRowHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
		public ImageView questLogo;
	    public TextView questName;


        public ItemRowHolder(View itemView)
        {
            super(itemView);
            questLogo = itemView.findViewById(R.id.quest_logo);
            questName = itemView.findViewById(R.id.quest_name);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view)
        {
            final int pos = getLayoutPosition();
            int pos1 = getAdapterPosition();
            if (pos == pos1)
            {
                
            }
        }
    }
}
