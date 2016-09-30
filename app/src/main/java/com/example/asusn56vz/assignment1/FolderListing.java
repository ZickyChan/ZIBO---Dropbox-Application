package com.example.asusn56vz.assignment1;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;


import java.io.IOException;
import java.util.ArrayList;
import android.view.View.OnClickListener;
import android.widget.Toast;

/**
 * Created by Asus N56VZ on 27/03/2015.
 */
public class FolderListing extends DialogFragment {
    public static final String PREFS_NAME = "settingSaving";
    private final Context context;
    private final DropboxAPI<?> mApi;
    private ListFolder lf;
    private GridFolder gf;
    private ListView fileList;
    private GridView fileGrid;
    private String chosenFile;
    private DropboxAPI.Entry[] en = null;
    private Boolean firstLvl = true;
    private String path = "/";
    ArrayList<String> str = new ArrayList<String>();
    private ImageView list;
    private ImageView grid;
    private ImageView back;
    private ImageView upload;

    private TextView logout;
    private TextView title;
    private MainActivity m;

    private TextView isNull; //It will appear whenever the folder is empty
    private String mode = "list";

    private LruCache lru; //Cache the image
    private LruCache cacheFolder; //Cache the ListView or GridView

    public FolderListing(Context context, DropboxAPI<?> api, MainActivity m){
        this.context = context;
        mApi = api;
        this.m = m;

        //Set size of each cache is 5 MB
        lru = new LruCache(5* 1024 * 1024);
        cacheFolder = new LruCache(5*1024*1024);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.folder_view, container, false);

        //Declare element which content in folder_view layout
        fileList = (ListView) v.findViewById(R.id.listView);
        fileGrid = (GridView) v.findViewById(R.id.gridView);
        logout = (TextView) v.findViewById(R.id.logout);
        list = (ImageView) v.findViewById(R.id.list);
        grid = (ImageView) v.findViewById(R.id.grid);
        back = (ImageView) v.findViewById(R.id.back);
        isNull = (TextView) v.findViewById(R.id.nothing);
        upload = (ImageView) v.findViewById(R.id.upload);
        title = (TextView) v.findViewById(R.id.title);

        //Display isNull and fileGrid view
        isNull.setVisibility(View.GONE);
        fileGrid.setVisibility(View.GONE);

        //Set function when click list image
        list.setOnClickListener(new OnClickListener(){
            public void onClick(View v){
                mode = "list";
                if(en.length==0){
                    isNull.setVisibility(View.VISIBLE);
                    fileGrid.setVisibility(View.GONE);
                    fileList.setVisibility(View.GONE);
                }
                else {
                    isNull.setVisibility(View.GONE);
                    fileGrid.setVisibility(View.GONE);
                    fileList.setVisibility(View.VISIBLE);
                }
            }

        });

        //Set function when click grid image
        grid.setOnClickListener(new OnClickListener(){
            public void onClick(View v){
                mode = "grid";
                if(en.length==0){
                    isNull.setVisibility(View.VISIBLE);
                    fileGrid.setVisibility(View.GONE);
                    fileList.setVisibility(View.GONE);
                }
                else {
                    isNull.setVisibility(View.GONE);
                    fileList.setVisibility(View.GONE);
                    fileGrid.setVisibility(View.VISIBLE);
                }
            }

        });

        //Set function when click upload image
        upload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v){
                showUpload();
            }
        });

        setView(); //Call setView method to set up the interface

        //Set function to log out
        logout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                m.logOut();
                FolderListing.this.dismiss();
            }
        });

        //Set function when click each element in ListView fileList so that it can open a file or folder
        fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (en[position]!=null) {
                    chosenFile = en[position].fileName();
                    if (en[position].isDir) {
                        firstLvl = false;

                                // Adds chosen directory to list
                                str.add(chosenFile);
                                if(chosenFile.length()>12){
                                    String show = chosenFile.substring(0,9) + "...";
                                    title.setText(show);
                                }
                                else{
                                    title.setText(chosenFile);
                                }
                                en = null;
                                path = path + chosenFile + "/";
                                setView();
                    }
                            // File picked
                    else {
                        if (en[position].fileName().length()>4) {
                            String extension = en[position].fileName().substring(en[position].fileName().length() - 4, en[position].fileName().length());
                            String extension2 = en[position].fileName().substring(en[position].fileName().length() - 5, en[position].fileName().length());
                            if (extension.equalsIgnoreCase(".jpg") || extension2.equalsIgnoreCase(".jpeg") || extension.equalsIgnoreCase(".png") || extension.equalsIgnoreCase(".txt")) {
                                try {
                                    showRead(en[position]);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            else{
                                showToast("This app only support showing image file or text file!");
                            }
                        }
                        else{
                            showToast("This file doesn't have extension!!!");
                        }
                    }
                }
            }
        });

        //Set function when clicking back image to go back the parent folder
        back.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String s = str.remove(str.size() - 1);

                // path modified to exclude present directory
                path = path.substring(0,path.lastIndexOf(s));
                en = null;

                // if there are no more directories in the list, then
                // its the first level
                if (str.isEmpty()) {
                    firstLvl = true;
                    title.setText("Home");
                }
                setView();
            }
        });

        //Same function with fileList clicking function but for GridView fileGrid
        fileGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (en[position]!=null) {
                    chosenFile = en[position].fileName();
                    if (en[position].isDir) {
                        firstLvl = false;

                        // Adds chosen directory to list
                        str.add(chosenFile);
                        if(chosenFile.length()>12){
                            String show = chosenFile.substring(0,9) + "...";
                            title.setText(show);
                        }
                        else{
                            title.setText(chosenFile);
                        }
                        en = null;
                        path = path + chosenFile + "/";
                        Log.w("e", path);
                        setView();
                    }
                    // File picked
                    else {

                        //Check if file have extension
                        if (en[position].fileName().length()>4) {
                            String extension = en[position].fileName().substring(en[position].fileName().length() - 4, en[position].fileName().length());
                            String extension2 = en[position].fileName().substring(en[position].fileName().length() - 5, en[position].fileName().length());
                            //Check if file is image or text file
                            if (extension.equalsIgnoreCase(".jpg") || extension2.equalsIgnoreCase(".jpeg") || extension.equalsIgnoreCase(".png") || extension.equalsIgnoreCase(".txt")) {
                                try {
                                    showRead(en[position]);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            else{
                                showToast("This app only support showing image file or text file!");
                            }
                        }
                        else{
                            showToast("This file doesn't have extension!!!");
                        }
                    }
                }
            }
        });

        return v;
    }

    //Load the entry and store in an array if that entry contents other entries
    protected void loadFolder(String p){
        try {
            int i = 0;
            DropboxAPI.Entry entries = mApi.metadata(p, 1000, null, true, null);
            en = new DropboxAPI.Entry[entries.contents.size()];
            for (DropboxAPI.Entry e: entries.contents){
                en[i] = e;
                i++;
            }
            if (!firstLvl) {
                back.setVisibility(View.VISIBLE);

            }
            else{
                back.setVisibility(View.GONE);
            }
        } catch (DropboxException e) {
            e.printStackTrace();
        }
    }

    //Set the view if user modify anything
    public void setView(){
        loadFolder(path);

        ListFolder tempList = (ListFolder) cacheFolder.get(path + "list");
        GridFolder tempGrid = (GridFolder) cacheFolder.get(path+"grid");
        if(tempList == null ){
            lf = new ListFolder(context,mApi,en,path,FolderListing.this,lru);
            cacheFolder.put(path+"list",lf);
        }
        else{
            lf = tempList;
        }
        if(tempList == null ){
            gf = new GridFolder(context,mApi,en,path,FolderListing.this,lru);
            cacheFolder.put(path+"grid",gf);
        }
        else{
            gf = tempGrid;
        }
        fileList.setAdapter(lf);
        fileGrid.setAdapter(gf);

        //If the entry is null then display the ListView and GridView, show isNull
        if(en.length == 0){
            isNull.setVisibility(View.VISIBLE);
            fileList.setVisibility(View.GONE);
            fileGrid.setVisibility(View.GONE);
        }
        else if(mode.equalsIgnoreCase("list")){
            isNull.setVisibility(View.GONE);
            fileList.setVisibility(View.VISIBLE);
            fileGrid.setVisibility(View.GONE);
        }
        else if(mode.equalsIgnoreCase("grid")){
            isNull.setVisibility(View.GONE);
            fileList.setVisibility(View.GONE);
            fileGrid.setVisibility(View.VISIBLE);
        }
    }

    //This function is like setView but when user modify anything whhich is already stored in a cache, this function will run
    public void setNewView(){
        loadFolder(path);
        lf = new ListFolder(context,mApi,en,path,FolderListing.this,lru);
        cacheFolder.put(path+"list",lf);
        gf = new GridFolder(context,mApi,en,path,FolderListing.this,lru);
        cacheFolder.put(path+"grid",gf);
        fileList.setAdapter(lf);
        fileGrid.setAdapter(gf);
        if(en.length == 0){
            isNull.setVisibility(View.VISIBLE);
            fileList.setVisibility(View.GONE);
            fileGrid.setVisibility(View.GONE);
        }
        else if(mode.equalsIgnoreCase("list")){
            isNull.setVisibility(View.GONE);
            fileList.setVisibility(View.VISIBLE);
            fileGrid.setVisibility(View.GONE);
        }
        else if(mode.equalsIgnoreCase("grid")){
            isNull.setVisibility(View.GONE);
            fileList.setVisibility(View.GONE);
            fileGrid.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Remove the title of dialog
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    //this function to show Rename DialogFragment
    void showRename(FolderListing f, int position, DropboxAPI.Entry[] e) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = new Rename(context,mApi,e,position,f);
        newFragment.show(ft, "dialog");
    }

    //this function to show Move DialogFragment
    void showMove(String o, String f) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = new Move(context,mApi,o,f,FolderListing.this);
        newFragment.show(ft, "dialog");
    }

    //this function to show Upload DialogFragment
    void showUpload() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = new Upload(context,mApi,path,FolderListing.this);
        newFragment.show(ft, "dialog");
    }

    //This function to show ReadFile DialogFragment
    void showRead(DropboxAPI.Entry ent) throws IOException {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = new ReadFile(context,mApi,ent,lru);
        newFragment.show(ft, "dialog");
    }

    //This function to show the toast
    private void showToast(String msg) {
        Toast error = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        error.show();
    }
}