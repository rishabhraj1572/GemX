package com.gemx.gemx.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gemx.gemx.R;
import com.gemx.gemx.RecyclerItems.ChatHistoryItems;

import java.util.List;

public class ChatHistoryItemAdapter extends RecyclerView.Adapter<ChatHistoryItemAdapter.ItemViewHolder> {

    private List<ChatHistoryItems> itemList;

    public ChatHistoryItemAdapter(List<ChatHistoryItems> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ChatHistoryItems item = itemList.get(position);
        holder.textView.setText(item.getText());
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView5);
        }
    }
}
