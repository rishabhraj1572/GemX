package com.gemx.gemx.RecyclerItems;

public class ChatItems {
    private String sender,receiver;

    public ChatItems(String sender,String receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }
}
