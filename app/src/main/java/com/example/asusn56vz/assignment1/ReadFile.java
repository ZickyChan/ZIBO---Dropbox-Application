package com.example.asusn56vz.assignment1;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.*;
import com.dropbox.client2.exception.DropboxException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Created by Asus N56VZ on 04/04/2015.
 */
public class ReadFile extends DialogFragment {
    private Context context;
    private DropboxAPI<?> mApi;
    private Entry e;
    private DropboxFileInfo info;
    private String fTitle;
    private String tempPath;
    private String tempImg;
    private TextView title;
    private EditText edit;
    private ImageView img;
    private Button close;

    private File f;
    private InputStream instream;
    private InputStreamReader inputreader;
    private BufferedReader buffreader;
    private FileOutputStream fos;
    private FileOutputStream imgFos;

    private LruCache lru;

    public ReadFile(Context c, DropboxAPI<?> api, DropboxAPI.Entry e,LruCache lru) throws FileNotFoundException {
        context = c;
        mApi = api;
        this.e = e;
        fTitle = e.fileName();
        this.lru = lru;

        tempPath = context.getCacheDir().getAbsolutePath() + "/" + "demo.txt";
        tempImg = context.getCacheDir().getAbsolutePath() + "/" + "demo.jpg";
    }
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.readfile, container, false);
        title = (TextView) v.findViewById(R.id.title);
        edit = (EditText) v.findViewById(R.id.text);
        img = (ImageView) v.findViewById(R.id.img);
        close = (Button) v.findViewById(R.id.close);
        title.setText(fTitle);

        String extension = fTitle.substring(fTitle.length() - 4, fTitle.length());
        String extension2 = fTitle.substring(fTitle.length() - 5, fTitle.length());
        if (extension.equalsIgnoreCase(".jpg") ||  extension.equalsIgnoreCase(".png") || extension2.equalsIgnoreCase(".jpeg")) {
            //f = new File(tempImg);
            edit.setVisibility(View.GONE);
            Drawable temp = (Drawable) lru.get(e.path + "big");
            if(temp == null) {
                String cachePath = context.getCacheDir().getAbsolutePath() + "/" + "abc.jpg";
                try {
                    imgFos = new FileOutputStream(cachePath);
                    mApi.getThumbnail(e.path, imgFos, ThumbSize.BESTFIT_1024x768, ThumbFormat.JPEG, null);
                } catch (Exception e) {
                    Log.w("error","cannot");
                }
                temp = Drawable.createFromPath(cachePath);
                lru.put(e.path + "big",temp);
            }
            img.setImageDrawable(temp);
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReadFile.this.dismiss();
                }
            });
        }
        else{
            f = new File(tempPath);
            img.setVisibility(View.GONE);
            try {
                fos = new FileOutputStream(f);
                info = mApi.getFile(e.path,null,fos,null);
                instream = new FileInputStream(tempPath);
                inputreader = new InputStreamReader(instream);
                buffreader = new BufferedReader(inputreader);
                String content = ReadBigString(buffreader);
                edit.setText(content);
                close.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        new AlertDialog.Builder(context)
                                .setTitle("Confirm")
                                .setMessage("Do you want to save?")
                                .setPositiveButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        //
                                        ReadFile.this.dismiss();
                                    }
                                })
                                .setNegativeButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // continue with delete
                                        new save().execute();
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                });
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        return v;
    }

    //This method to read all the lines in file and store it in a string
    public String ReadBigString(BufferedReader buffIn) throws IOException {
        StringBuilder everything = new StringBuilder();
        String line;
        while( (line = buffIn.readLine()) != null) {
            everything.append(line+ "\n");
        }
        return everything.toString();
    }
    public class save extends AsyncTask<Void, Long, Boolean> {
        private boolean result;
        private ProgressDialog mDialog;

        public save(){
            mDialog = new ProgressDialog(context);
            ReadFile.this.dismiss();
            mDialog.show();
            mDialog.setContentView(R.layout.processdialog);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String tempFile = context.getCacheDir().getAbsolutePath() + "/" + "write.txt";
                File nFile = new File(tempFile);
                FileOutputStream nFos = new FileOutputStream(nFile);
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(nFos));
                String newContent = edit.getText().toString();
                String[] store = newContent.split("\n");
                for(int i=0;i<store.length;i++){
                    out.write(store[i] + "\n");
                }
                out.close();
                tempFile = context.getCacheDir().getAbsolutePath() + "/" + "write.txt";
                nFile = new File(tempFile);
                FileInputStream a = new FileInputStream(nFile);
//                InputStreamReader b = new InputStreamReader(a);
//                BufferedReader c = new BufferedReader(b);
//                String content = ReadBigString(c);
                DropboxAPI.Entry entry = (Entry) mApi.putFileOverwrite(e.path, a, nFile.length(), null);
                result = true;
                out.close();
            }
            catch(Exception e){
                result = false;
            }
            finally {
                try {
                    buffreader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return result;
        }
        protected void onProgressUpdate(Long... progress) {
            int percent = (int)(100.0*(double)progress[0]/100 + 0.5);
            mDialog.setProgress(percent);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mDialog.dismiss();
            if (result) {
                //Show toast when succeed
                showToast("Saved!");
                //f.setPath(e[position].parentPath());
            } else {
                // Couldn't download it, so show an error
                showToast("There is some problem!");
            }
        }

        private void showToast(String msg) {
            Toast error = Toast.makeText(context, msg, Toast.LENGTH_LONG);
            error.show();
        }
    }
}
