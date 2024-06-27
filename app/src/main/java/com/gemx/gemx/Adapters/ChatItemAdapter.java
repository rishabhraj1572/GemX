package com.gemx.gemx.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.gemx.gemx.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ChatItemAdapter extends RecyclerView.Adapter<ChatItemAdapter.ItemViewHolder> {

    private List<String> senderList;
    private List<String> receiverList;
    private List<String> imageUrlList;
    private Context context;


    public ChatItemAdapter(Context context,List<String> senderList,List<String> receiverList, List<String> imageUrlList) {
        this.context = context;
        this.senderList = senderList;
        this.receiverList = receiverList;
        this.imageUrlList = imageUrlList;
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

        //picasso for image
        String imageUrlItem = imageUrlList.get(position);
        if(imageUrlItem.equals("na")){
            //do nothing
            holder.chatImage.setVisibility(View.GONE);
        }else{
            holder.chatImage.setVisibility(View.VISIBLE);
            Glide.with(context).load(imageUrlItem)
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(18)))
                    .into(holder.chatImage);
//            Picasso.get()
//                    .load(imageUrlItem)
//                    .into(holder.chatImage);
        }

    }

    @Override
    public int getItemCount() {
        return senderList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView sender,receiver;
        ImageView chatImage;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.sender);
            receiver = itemView.findViewById(R.id.receiver);
            chatImage = itemView.findViewById(R.id.chatImage);
        }
    }
}
