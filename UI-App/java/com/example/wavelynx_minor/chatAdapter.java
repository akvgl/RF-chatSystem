package com.example.wavelynx_minor;

import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;

public class chatAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final ArrayList<String> messages;

    public chatAdapter(Context context, ArrayList<String> messages) {
        super(context, R.layout.chat_bubble, messages);
        this.context = context;
        this.messages = messages;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.chat_bubble, parent, false);
            holder = new ViewHolder();
            holder.msgText = convertView.findViewById(R.id.message_text);
            holder.bubbleLayout = convertView.findViewById(R.id.bubble_layout);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String msg = messages.get(position);
        holder.msgText.setText(msg);

        // Style messages
        if (msg.startsWith("Me:")) {
            holder.msgText.setBackgroundResource(R.drawable.chat_bubble_right);
            holder.bubbleLayout.setGravity(Gravity.END);
            holder.msgText.setTextColor(Color.WHITE);
        } else if (msg.startsWith("Friend:")) {
            holder.msgText.setBackgroundResource(R.drawable.chat_bubble_left);
            holder.bubbleLayout.setGravity(Gravity.START);
            holder.msgText.setTextColor(Color.BLACK);
        } else {
            holder.msgText.setBackgroundResource(R.drawable.chat_bubble_left);
            holder.bubbleLayout.setGravity(Gravity.START);
            holder.msgText.setTextColor(Color.BLACK);
        }

        return convertView;
    }

    static class ViewHolder {
        TextView msgText;
        LinearLayout bubbleLayout;
    }
}