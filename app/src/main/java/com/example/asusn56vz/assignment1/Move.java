package com.example.asusn56vz.assignment1;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.view.View.OnClickListener;
import android.widget.Toast;

/**
* Created by Asus N56VZ on 27/03/2015.
*/
public class Move extends DialogFragment {
    private final Context context;
    private final DropboxAPI<?> mApi;
    private ListView fileList;
    private String chosenFile;
    private DropboxAPI.Entry[] en = null;
    private Boolean firstLvl = true;
    private String path = "/";
    private String oldPath;
    private String fileName;
    private List2 l2;
    private FolderListing f;
    ArrayList<String> str = new ArrayList<String>();

    public Move(Context context, DropboxAPI<?> api, String oldPath, String fName,FolderListing f){
        this.context = context;
        mApi = api;
        this.oldPath = oldPath;
        fileName = fName;
        this.f = f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.move, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        fileList = (ListView) v.findViewById(R.id.listView);
        Button ok = (Button) v.findViewById(R.id.ok);
        Button cancel = (Button) v.findViewById(R.id.cancel);
        final EditText showPath = (EditText) v.findViewById(R.id.link_path);
        setView();
        fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (en[position] != null) {
                    chosenFile = en[position].fileName();
                    if (en[position].isDir) {
                        firstLvl = false;

                        // Adds chosen directory to list
                        str.add(chosenFile);
                        en = null;
                        path = path + chosenFile + "/";
                        showPath.setText(path);
                        setView();
                        Log.w("e", path);
                    }
                }
                // Checks if 'up' was clicked
                else {
                    // present directory removed from list
                    String s = str.remove(str.size() - 1);

                    // path modified to exclude present directory
                    path = path.substring(0, path.lastIndexOf(s));
                    showPath.setText(path);
                    en = null;

                    // if there are no more directories in the list, then
                    // its the first level
                    if (str.isEmpty()) {
                        firstLvl = true;
                    }
                    setView();
                }
            }
        });
        ok.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String newPath = showPath.getText().toString();
                newPath = newPath + "/" + fileName;
                new MoveDo(oldPath,newPath).execute();
            }

        });
        cancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Move.this.dismiss();
            }
        });
        return v;
    }
    protected void loadFolder(String p){
        try {
            int i = 0;
            DropboxAPI.Entry entries = mApi.metadata(p, 1000, null, true, null);
            for (DropboxAPI.Entry e: entries.contents){
                if(e.isDir)
                i++;
            }
            en = new DropboxAPI.Entry[i];
            i = 0;
            for (DropboxAPI.Entry e: entries.contents){
                if(e.isDir) {
                    en[i] = e;
                    i++;
                }
            }

            if (!firstLvl) {
                DropboxAPI.Entry[] temp = new DropboxAPI.Entry[en.length + 1];
                for (int j = 0; j < en.length; j++) {
                    temp[j + 1] = en[j];
                }
                en = temp;
            }
        } catch (DropboxException e) {
            e.printStackTrace();
        }
    }
    public void setView(){
        loadFolder(path);
        l2 = new List2(context,mApi,en);
        fileList.setAdapter(l2);
    }
    public class MoveDo extends AsyncTask<Void, Long, Boolean> {
        private boolean result;
        private String currentPath;
        private String newPath;
        private final ProgressDialog mDialog;

        public MoveDo(String now, String newN){
            currentPath = now;
            newPath = newN;
            mDialog = new ProgressDialog(context);
            Move.this.dismiss();
            mDialog.show();
            mDialog.setContentView(R.layout.processdialog);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                mApi.move(currentPath,newPath);
                result = true;
            } catch (DropboxException e1) {
                e1.printStackTrace();
                result = false;
            }

            return result;
        }
        @Override
        protected void onProgressUpdate(Long... progress) {
            int percent = (int)(100.0*(double)progress[0]/100 + 0.5);
            mDialog.setProgress(percent);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mDialog.dismiss();
            if (result) {
                //Show toast when succeed
                showToast("Moved!");
                f.setNewView();
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