package com.gemx.gemx.Adapters;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.gemx.gemx.R;
import com.gemx.gemx.ViewShareChat;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.List;

public class ChatItemAdapter extends RecyclerView.Adapter<ChatItemAdapter.ItemViewHolder> {

    private List<String> senderList;
    private List<String> receiverList;
    private List<String> imageUrlList;
    private Context context;
    private int lastItemPosition=-1;
    private ChatItemAdapter.OnItemClickListener mListener;


    public ChatItemAdapter(Context context,List<String> senderList,List<String> receiverList, List<String> imageUrlList) {
        this.context = context;
        this.senderList = senderList;
        this.receiverList = receiverList;
        this.imageUrlList = imageUrlList;
    }

    public interface OnItemClickListener {
        void onRefresh(int position);
    }

    public void setOnItemClickListener(ChatItemAdapter.OnItemClickListener listener) {
        this.mListener = listener;
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
        String receiveItemi = receiverList.get(position);
        try{
            String receiveItem = receiveItemi.substring(0, receiveItemi.length() - 1);
            holder.receiver.setText(receiveItem);
            holder.copyBtn.setOnClickListener(v->setClipboard(context,receiveItem));
        }catch (Exception ignore){
            holder.receiver.setText(receiveItemi);
            holder.copyBtn.setOnClickListener(v->setClipboard(context,receiveItemi));
        }

        String imageUrlItem = imageUrlList.get(position);
        if(imageUrlItem.equals("na")){
            //do nothing
            holder.chatImage.setVisibility(View.GONE);
        }else{
            holder.chatImage.setVisibility(View.VISIBLE);
            Glide.with(context).load(imageUrlItem)
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(18)))
                    .into(holder.chatImage);
            holder.chatImage.setOnClickListener(v->showPicDialog(imageUrlItem));
        }

        if (context instanceof ViewShareChat) {
            holder.refreshBtn.setVisibility(View.GONE);
        }else{
            if(lastItemPosition !=-1){
                if (position == lastItemPosition) {
                    holder.refreshBtn.setVisibility(View.VISIBLE);
                    //do refresh
                }else {
                    holder.refreshBtn.setVisibility(View.GONE);
                }
            }
        }

        holder.refreshBtn.setOnClickListener(view -> {
            if (mListener != null) {
                if (position != RecyclerView.NO_POSITION) {
                    mListener.onRefresh(position);
                }
            }
        });
    }

    public void updateLastItemPosition(int position) {
        lastItemPosition = position;
        notifyDataSetChanged();
    }
    private void showPicDialog(String imageUrl) {

        Dialog dialog = new Dialog(context,R.style.FullScreenDialog);
        dialog.setContentView(R.layout.display_image_dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        PhotoView displayImg = dialog.findViewById(R.id.displayImg);
        ImageView cancel = dialog.findViewById(R.id.cancelBtn);

        cancel.setOnClickListener(v->dialog.dismiss());

        Glide.with(context)
                .load(imageUrl)
                .into(displayImg);

        dialog.show();
    }

    private void setClipboard(Context context, String text) {
        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setText(text);
    }

    @Override
    public int getItemCount() {
        return senderList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView sender,receiver;
        ImageView chatImage,copyBtn,refreshBtn;
        ConstraintLayout displayImageLayout;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.sender);
            receiver = itemView.findViewById(R.id.receiver);
            chatImage = itemView.findViewById(R.id.chatImage);
            displayImageLayout = itemView.findViewById(R.id.other);
            copyBtn = itemView.findViewById(R.id.copy);
            refreshBtn = itemView.findViewById(R.id.refresh);
        }
    }
}
