package app.cap.beshop.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import app.cap.beshop.R;
import app.cap.beshop.dataStructures.Chatting;

public class ChatAdapter extends ArrayAdapter<Chatting> {

    public ChatAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_chat, null);

            viewHolder = new ViewHolder();
            viewHolder.mTxtUserName = (TextView) convertView.findViewById(R.id.txt_userName);
            viewHolder.mTxtMessage = (TextView) convertView.findViewById(R.id.txt_message);
            viewHolder.mTxtTime = (TextView) convertView.findViewById(R.id.txt_time);

            convertView.setTag(viewHolder);
        } else  {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        Chatting chatData = getItem(position);
        viewHolder.mTxtUserName.setText(chatData.userName);
        viewHolder.mTxtMessage.setText(chatData.message);
        viewHolder.mTxtTime.setText(chatData.time);

        return convertView;
    }

    private class ViewHolder {
        private TextView mTxtUserName;
        private TextView mTxtMessage;
        private TextView mTxtTime;
    }
}
