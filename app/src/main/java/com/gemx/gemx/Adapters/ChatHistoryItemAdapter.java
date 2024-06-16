package com.gemx.gemx.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.gemx.gemx.R;

import java.util.List;

public class ChatHistoryItemAdapter extends RecyclerView.Adapter<ChatHistoryItemAdapter.ItemViewHolder> {

    private List<String> itemList,itemId;
    private OnItemClickListener mListener;

    public ChatHistoryItemAdapter(List<String> itemList,List<String> itemId) {
        this.itemList = itemList;
        this.itemId = itemId;
    }

    public interface OnItemClickListener{
        void onItemClick(int position);
    }

    public void  setOnItemClickListener(OnItemClickListener listener){
        this.mListener= listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        String item = itemList.get(position);
        holder.textView.setText(item);

        holder.historyItem.setOnClickListener(view->{
            if(mListener!= null){
                if (position!=RecyclerView.NO_POSITION){
                    mListener.onItemClick(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        CardView historyItem;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            historyItem = itemView.findViewById(R.id.history_item);
            textView = itemView.findViewById(R.id.textView5);
        }
    }
}
