package com.gemx.gemx.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gemx.gemx.R;
import com.gemx.gemx.RecyclerItems.ChatHistoryItems;
import com.gemx.gemx.RecyclerItems.ChatItems;

import java.util.List;

public class ChatItemAdapter extends RecyclerView.Adapter<ChatItemAdapter.ItemViewHolder> {

    private List<String> senderList;
    private List<String> receiverList;


    public ChatItemAdapter(List<String> senderList,List<String> receiverList) {
        this.senderList = senderList;
        this.receiverList = receiverList;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        String sendItem = senderList.get(position);
        holder.sender.setText(sendItem);
        String receiveItem = receiverList.get(position);
        holder.receiver.setText(receiveItem);
    }

    @Override
    public int getItemCount() {
        return senderList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView sender,receiver;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.sender);
            receiver = itemView.findViewById(R.id.receiver);
        }
    }
}
