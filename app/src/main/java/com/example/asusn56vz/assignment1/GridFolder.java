package com.example.asusn56vz.assignment1;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;

import com.dropbox.client2.DropboxAPI.*;

import android.view.View.*;

import java.io.FileOutputStream;

/**
 * Created by Asus N56VZ on 24/03/2015.
 */
public class GridFolder extends ArrayAdapter {
    private final Context context;
    private final DropboxAPI<?> mApi;
    private final Entry[] e;
    private String mPath;
    private FolderListing f;
    private LruCache lru;
    private final static String IMAGE_FILE_NAME = "dbroulette.png";

    public GridFolder(Context context, DropboxAPI<?> api, Entry[] e, String path, FolderListing f, LruCache lru) {
        super(context,R.layout.folder,e);
        this.context = context;
        this.e = e;
        mApi = api;
        mPath = path;
        this.f = f;
        this.lru = lru;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.grid_folder, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.title);
        final ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        final ImageView download = (ImageView) rowView.findViewById(R.id.download);
        final ImageView delete = (ImageView) rowView.findViewById(R.id.delete);
        final ImageView move = (ImageView) rowView.findViewById(R.id.move);
        final ImageView rename = (ImageView) rowView.findViewById(R.id.rename);

        final LinearLayout downloadLinear = (LinearLayout) rowView.findViewById(R.id.linearDownload);

        if (e[position]!=null) {
            if(e[position].fileName().length()>26){
                String name = e[position].fileName();
                textView.setText(name.substring(0,13) + "..." + name.substring(name.length()-6,name.length()));
            }
            else {
                textView.setText(e[position].fileName());
            }
            if (e[position].isDir) {
                download.setVisibility(View.GONE);
                downloadLinear.setVisibility(View.GONE);
            }
        }
        // Change the icon for folders and files
        if(e[position]==null){
            imageView.setImageResource(R.drawable.directory_up);
        }
        else if(e[position].isDir){
            imageView.setImageResource(R.drawable.directory_icon);
        }
        else{
            imageView.setImageResource(R.drawable.file_icon);
        }
        //If the entry is an image file then load the thumbnail
        if (e[position].fileName().length()>4) {
            String extension = e[position].fileName().substring(e[position].fileName().length() - 4, e[position].fileName().length());
            if (extension.equalsIgnoreCase(".jpg") || extension.equalsIgnoreCase("jpeg") || extension.equalsIgnoreCase(".png")) {
                imageView.setImageResource(R.drawable.image);
                new loadThumbnail(imageView, e[position]).execute();
            }
        }
        download.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (v.equals(download)) {
                    DownloadFile d = new DownloadFile(context,mApi,e[position].fileName(),mPath);
                    d.execute();
                }
            }
        });
        delete.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v){
                try {
                    if (e[position].isDir)
                        mApi.delete(e[position].path + "/");
                    else
                        mApi.delete(e[position].path);
                    f.setNewView();

                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
        move.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                f.showMove(e[position].path, e[position].fileName());
                f.setView();
            }
        });
        rename.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                f.showRename(f,position,e);
            }
        });
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
                Log.e("eroor", "couldn't");
                mView.setImageResource(R.drawable.file_icon);
            }
        }
    }
}
