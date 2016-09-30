package com.example.asusn56vz.assignment1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;

import com.dropbox.client2.DropboxAPI.*;
import com.dropbox.client2.exception.DropboxException;

import android.view.View.*;

import java.io.File;

/**
 * Created by Asus N56VZ on 24/03/2015.
 */
public class List2 extends ArrayAdapter {
    private final Context context;
    private final DropboxAPI<?> mApi;
    private final Entry[] e;

    public List2(Context context, DropboxAPI<?> api, Entry[] e) {
        super(context,R.layout.folder,e);
        this.context = context;
        this.e = e;
        mApi = api;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list2, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.firstLine);
        final ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        if (e[position]!=null) {
            if(e[position].fileName().length()>26){
                String name = e[position].fileName();
                textView.setText(name.substring(0,13) + "..." + name.substring(name.length()-6,name.length()));
            }
            else {
                textView.setText(e[position].fileName());
            }
        }
        else{
            textView.setText("Back");
        }

        // Change the icon for folders and files
        if(e[position]==null){
            imageView.setImageResource(R.drawable.directory_up);
        }
        else{
            imageView.setImageResource(R.drawable.directory_icon);
        }
        return rowView;
    }
}
