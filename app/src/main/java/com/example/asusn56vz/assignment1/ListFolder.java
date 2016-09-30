package com.example.asusn56vz.assignment1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;

import com.dropbox.client2.DropboxAPI.*;
import com.dropbox.client2.exception.DropboxException;

import android.view.View.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by Asus N56VZ on 24/03/2015.
 */
public class ListFolder extends ArrayAdapter {
    private final Context context;
    private final DropboxAPI<?> mApi;
    private Entry[] e;
    private String mPath;
    private FolderListing f;
    private final int USER_OPTION = 100;
    private final static String IMAGE_FILE_NAME = "dbroulette.png";
    private LruCache lru;

    public ListFolder(Context context, DropboxAPI<?> api, Entry[] e, String path, FolderListing folder, LruCache lru) {
        super(context,R.layout.folder,e);
        this.context = context;
        this.e = e;
        mApi = api;
        mPath = path;
        f = folder;
        this.lru = lru;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.folder, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.firstLine);
        final ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        final ImageView plus = (ImageView) rowView.findViewById(R.id.plus);


        if (e[position]!=null) {
            if(e[position].fileName().length()>26){
                String name = e[position].fileName();
                textView.setText(name.substring(0,13) + "..." + name.substring(name.length()-6,name.length()));
            }
            else {
                textView.setText(e[position].fileName());
            }
        }

        if(e[position].isDir){
            imageView.setImageResource(R.drawable.directory_icon);
        }
        else{
            imageView.setImageResource(R.drawable.file_icon);
        }
        plus.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (v.equals(plus)) {
                    onCreateDialog(USER_OPTION,e[position],position);
                }
            }
        });
        if (e[position].fileName().length()>4) {
            String extension = e[position].fileName().substring(e[position].fileName().length() - 4, e[position].fileName().length());
            String extension2 = e[position].fileName().substring(e[position].fileName().length() - 5, e[position].fileName().length());
            if (extension.equalsIgnoreCase(".jpg") || extension2.equalsIgnoreCase(".jpeg") || extension.equalsIgnoreCase(".png")) {
                imageView.setImageResource(R.drawable.image);
                new loadThumbnail(imageView, e[position]).execute();
            }
        }
        return rowView;
    }
    public class loadThumbnail extends AsyncTask<Void, Long, Boolean>{
        private Drawable mDrawable;
        private ImageView mView;
        private FileOutputStream mFos;
        private Entry ent;

        public loadThumbnail(ImageView img, Entry ent){
            mView = img;
            this.ent = ent;
        }

        protected Boolean doInBackground(Void... params) {
            Drawable temp = (Drawable) lru.get(ent.path);
            if(temp == null) {
                String cachePath = context.getCacheDir().getAbsolutePath() + "/" + IMAGE_FILE_NAME;
                try {
                    mFos = new FileOutputStream(cachePath);
                    mApi.getThumbnail(ent.path, mFos, ThumbSize.ICON_64x64, ThumbFormat.JPEG, null);
                } catch (Exception e) {
                    return false;
                }
                temp = Drawable.createFromPath(cachePath);
                lru.put(ent.path,temp);
                mDrawable = temp;
            }
            else{
                mDrawable = (Drawable) lru.get(ent.path);
            }
            return true;
        }
        protected void onPostExecute(Boolean result) {
            if (result) {
                // Set the image now that we have it
                mView.setImageDrawable((Drawable) mDrawable);
            } else {
                // Couldn't download it, so show an error
                Log.e("eroor","couldn't");
                mView.setImageResource(R.drawable.file_icon);
            }
        }
    }


    protected Dialog onCreateDialog(int id, Entry en, int p) {
        final Entry ent = en;
        Dialog dialog = null;
        final int pos = p;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String[] action = {"Delete", "Download", "Move", "Rename"};;
        if (en.isDir ) {
            String[] temp = {"Delete","Move","Rename"};
            action = temp;
        }
        ArrayAdapter<String> ad = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1,action);
        switch(id){
            case USER_OPTION:
                builder.setTitle("What do you want to do?");
                final String[] finalAction = action;
                builder.setAdapter(ad, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(finalAction[which].equalsIgnoreCase("delete")){
                            try {
                                if (ent.isDir)
                                    mApi.delete(ent.path + "/");
                                else
                                    mApi.delete(ent.path);
                                f.setNewView();

                            }
                            catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                        else if(finalAction[which].equalsIgnoreCase("download")){
                            DownloadFile d = new DownloadFile(context,mApi,ent.fileName(),mPath);
                            d.execute();
                        }
                        else if(finalAction[which].equalsIgnoreCase("rename")){
                            f.showRename(f,pos,e);
                        }
                        else{
                            f.showMove(ent.path,ent.fileName());
                        }
                    }
                });
                break;
        }
        dialog = builder.show();
        return dialog;
    }
}
