package com.example.asusn56vz.assignment1;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;


/**
 * Created by Asus N56VZ on 01/04/2015.
 */
public class Rename extends DialogFragment {
    private DropboxAPI.Entry[] e;
    private int position;
    private FolderListing f;
    private Context context;
    private DropboxAPI<?> mApi;

    public Rename(Context c, DropboxAPI<?> api, DropboxAPI.Entry[] e, int position, FolderListing f) {
        context = c;
        mApi = api;
        this.e = e;
        this.position = position;
        this.f = f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.rename_popup, container, false);
        final EditText name = (EditText) v.findViewById(R.id.newName);
        Button submit = (Button) v.findViewById(R.id.rename);
        Button cancel = (Button) v.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rename.this.dismiss();
            }
        });
        submit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String newName = name.getText().toString();
                if (newName.contains("/") || newName.contains("?") || newName.contains("\\") || newName.contains("*")) {
                    showToast("File or folder name should not contain special characters!");
                }
                else {
                    new RenameDo(newName).execute();
                }
            }
        });
        return v;
    }
    public class RenameDo extends AsyncTask<Void, Long, Boolean> {
        private boolean result;
        private String newName;
        private final ProgressDialog mDialog;

        public RenameDo(String name){
            newName = name;
            mDialog = new ProgressDialog(context);
            Rename.this.dismiss();
            mDialog.show();
            mDialog.setContentView(R.layout.processdialog);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            String currentPath = e[position].path;
            String parentPath = e[position].parentPath();
            String change = newName;
            parentPath = parentPath + "" + change;

            DropboxAPI.Entry RenamedFile    = null;  //move to new place "/"
            try {
                RenamedFile = mApi.move(currentPath, "/"+ change);
                DropboxAPI.Entry MoveRenameFile = mApi.move(RenamedFile.path,parentPath); //move to previous location
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
                showToast("Renamed!");
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
    private void showToast(String msg) {
        Toast error = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        error.show();
    }
}
