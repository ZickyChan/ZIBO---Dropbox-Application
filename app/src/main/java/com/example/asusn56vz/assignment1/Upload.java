package com.example.asusn56vz.assignment1;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;

/**
 * Created by Asus N56VZ on 02/04/2015.
 */
public class Upload extends DialogFragment {
    private Context context;
    private DropboxAPI<?> mApi;
    private File path = new File(Environment.getExternalStorageDirectory() + "");
    private ListView uploadList;
    private Item[] fileList;
    private boolean firstLvl = true;
    private String chosenFile;
    private String boxPath;
    private Button cancel;
    private FolderListing f;
    ListAdapter adapter;
    // Stores names of traversed directories
    ArrayList<String> str = new ArrayList<String>();

    public Upload(Context context, DropboxAPI<?> api, String boxPath, FolderListing f) {
        this.context = context;
        mApi = api;
        this.boxPath = boxPath;
        this.f = f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.upload, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        uploadList = (ListView) v.findViewById(R.id.listView);
        cancel = (Button) v.findViewById(R.id.cancel);

        loadFileList();

        uploadList.setAdapter(adapter);
        uploadList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                chosenFile = fileList[position].file;
                File sel = new File(path + "/" + chosenFile);
                if (sel.isDirectory()) {
                    firstLvl = false;

                    // Adds chosen directory to list
                    str.add(chosenFile);
                    fileList = null;
                    path = new File(sel + "");

                    setView();

                }

                // Checks if 'back' was clicked
                else if (chosenFile.equalsIgnoreCase("back") && !sel.exists()) {

                    // present directory removed from list
                    String s = str.remove(str.size() - 1);

                    // path modified to exclude present directory
                    path = new File(path.toString().substring(0,
                            path.toString().lastIndexOf(s)));
                    fileList = null;
                    // if there are no more directories in the list, then
                    // its the first level
                    if (str.isEmpty()) {
                        firstLvl = true;
                    }
                    setView();
                }
            }
        }

    );
    cancel.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Upload.this.dismiss();
        }
    });

            return v;
        }

    private void loadFileList() {
        try {
            path.mkdirs();
        } catch (SecurityException e) {
            Log.e("Path", "unable to write on the sd card ");
        }

        // Checks whether path exists
        if (path.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    // Filters based on whether the file is hidden or not
                    return (sel.isFile() || sel.isDirectory())
                            && !sel.isHidden();

                }
            };

            String[] fList = path.list(filter);
            fileList = new Item[fList.length];
            for (int i = 0; i < fList.length; i++) {
                fileList[i] = new Item(fList[i], R.drawable.file_icon);

                // Convert into file path
                File sel = new File(path, fList[i]);
                // Set drawables
                if (sel.isDirectory()) {
                    fileList[i].icon = R.drawable.directory_icon;
                    Log.d("DIRECTORY", fileList[i].file);
                } else {
                    Log.d("FILE", fileList[i].file);
                }
            }

            if (!firstLvl) {
                Item temp[] = new Item[fileList.length + 1];
                for (int i = 0; i < fileList.length; i++) {
                    temp[i + 1] = fileList[i];
                }
                temp[0] = new Item("Back", R.drawable.directory_up);
                fileList = temp;
            }
        } else {
            Log.e("error", "path does not exist");
        }

        adapter = new ArrayAdapter<Item>(context,R.layout.list2,fileList) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                //
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View rowView = inflater.inflate(R.layout.list3, parent, false);
                TextView text = (TextView) rowView.findViewById(R.id.firstLine);
                ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
                ImageView upload = (ImageView) rowView.findViewById(R.id.up);

                if (fileList[position]!=null) {
                    if (fileList[position].file.length() > 25){
                        String name = fileList[position].file.substring(0,12) + "..."
                                + fileList[position].file.substring(fileList[position].file.length() - 8, fileList[position].file.length());
                        text.setText(name);
                    }
                    else{
                    text.setText(fileList[position].file);
                    }
                }
                else{
                    text.setText("Back");
                }
                imageView.setImageResource(fileList[position].icon);
                if(fileList[position].icon == R.drawable.directory_up || fileList[position].icon == R.drawable.directory_icon){
                    upload.setVisibility(View.GONE);
                }
                else{
                    upload.setVisibility(View.VISIBLE);
                }
                upload.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        File sel = new File(path, fileList[position].file);
                        String upfile = boxPath + fileList[position].file;
                        new UploadDo(sel,upfile).execute();
                        /*if (!sel.isDirectory()){
                            Log.w("file","true");

                            try {
                                FileInputStream fis = new FileInputStream(sel);
                                String upfile = boxPath + fileList[position].file;
                                DropboxAPI.Entry response = mApi.putFile(upfile, fis, sel.length(), null, null);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }*/
                    }
                });

                return rowView;
            }
        };

    }

    private class Item {
        public String file;
        public int icon;

        public Item(String file, Integer icon) {
            this.file = file;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return file;
        }
    }
    public void setView(){
        loadFileList();
        uploadList.setAdapter(adapter);
    }
    public class UploadDo extends AsyncTask<Void, Long, Boolean> {
        private boolean result;
        private String upPath;
        private File sel;
        private FileInputStream fis;
        private final ProgressDialog mDialog;

        public UploadDo(File sel,String p){
            upPath = p;
            this.sel = sel;
            mDialog = new ProgressDialog(context);
            Upload.this.dismiss();
            mDialog.show();
            mDialog.setContentView(R.layout.processdialog);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
                try {
                    fis = new FileInputStream(this.sel);
                    DropboxAPI.Entry response = mApi.putFile(upPath, fis, sel.length(), null, null);
                    result = true;
                } catch (Exception e1) {
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
                showToast("Uploaded!");
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