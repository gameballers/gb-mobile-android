package com.example.testappwithoutfirebase;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ChallengeApiIDAdapter extends RecyclerView.Adapter<ChallengeApiIDAdapter.ItemRowHolder>
{
    private Context mContext;
    private ArrayList<String> mData;

    public ChallengeApiIDAdapter(Context context, ArrayList<String> data)
    {
        this.mData = data;
        this.mContext = context;
    }

    @Override
    public ItemRowHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View row = inflater.inflate(R.layout.challange_api_ids_item_layout, parent, false);
        return new ItemRowHolder(row);
    }

    @Override
    public void onBindViewHolder(ItemRowHolder holder, int position)
    {
        String item = mData.get(position);
        holder.challengeApiIdValue.setText(item);
    }

    @Override
    public int getItemCount()
    {
        return mData.size();
    }

    public void addChallengeApiId(String id)
    {
        mData.add(id);
        notifyDataSetChanged();
    }

    public ArrayList<String> getmData()
    {
        return mData;
    }

    public class ItemRowHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
		public TextView challengeApiIdValue;
		public Button removeBtn;


        public ItemRowHolder(View itemView)
        {
            super(itemView);
            challengeApiIdValue = itemView.findViewById(R.id.challenge_api_id_value);
            removeBtn = itemView.findViewById(R.id.remove_btn);

            removeBtn.setOnClickListener(this);
        }

        @Override
        public void onClick(View view)
        {
            final int pos = getLayoutPosition();
            int pos1 = getAdapterPosition();
            if (pos == pos1)
            {
                mData.remove(pos);
                notifyItemRemoved(pos);
            }
        }
    }
}
