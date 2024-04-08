package com.example.macmessenger.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.macmessenger.R;
import com.example.macmessenger.model.ChatMessageModel;
import com.example.macmessenger.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.squareup.picasso.Picasso;

public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder> {

    Context context;

    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options,Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatModelViewHolder holder, int position, @NonNull ChatMessageModel model) {
        Log.i("haushd","asjd");
        if (model.getType().equals("text")) {
            if (model.getSenderId().equals(FirebaseUtil.currentUserId())) {
                holder.leftChatLayout.setVisibility(View.GONE);
                holder.rightChatLayout.setVisibility(View.VISIBLE);
                holder.rightChatTextview.setText(model.getMessage());
            } else {
                holder.rightChatLayout.setVisibility(View.GONE);
                holder.leftChatLayout.setVisibility(View.VISIBLE);
                holder.leftChatTextview.setText(model.getMessage());
            }
        }
       else if (model.getType().equals("image")){
            if (model.getSenderId().equals(FirebaseUtil.currentUserId())){
                holder.leftChatLayout.setVisibility(View.GONE);
                holder.rightChatLayout.setVisibility(View.VISIBLE);
                holder.rightChatImageView.setVisibility(View.VISIBLE);

                Picasso.get().load(model.getMessage()).into(holder.rightChatImageView);
            } else {
                holder.rightChatLayout.setVisibility(View.GONE);
                holder.leftChatLayout.setVisibility(View.VISIBLE);
                holder.leftChatImageView.setVisibility(View.VISIBLE);

                Picasso.get().load(model.getMessage()).into(holder.leftChatImageView);
            }
        }

    }

    @NonNull
    @Override
    public ChatModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row,parent,false);
        return new ChatModelViewHolder(view);
    }

    class ChatModelViewHolder extends RecyclerView.ViewHolder{

        LinearLayout leftChatLayout,rightChatLayout;
        TextView leftChatTextview,rightChatTextview;
        ImageView leftChatImageView, rightChatImageView;


        public ChatModelViewHolder(@NonNull View itemView) {
            super(itemView);

            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextview = itemView.findViewById(R.id.left_chat_textview);
            rightChatTextview = itemView.findViewById(R.id.right_chat_textview);
            leftChatImageView = itemView.findViewById(R.id.left_chat_image_view);
            rightChatImageView = itemView.findViewById(R.id.right_chat_image_view);
        }
    }
}
