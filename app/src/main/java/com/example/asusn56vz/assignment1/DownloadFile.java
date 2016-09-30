package com.example.asusn56vz.assignment1;

/**
 * Created by Asus N56VZ on 25/03/2015.
 */
/*
 * Copyright (c) 2010-11 Dropbox, Inc.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.DropboxAPI.ThumbFormat;
import com.dropbox.client2.DropboxAPI.ThumbSize;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

/**
 * Here we show getting metadata for a directory and downloading a file in a
 * background thread, trying to show typical exception handling and flow of
 * control for an app that downloads a file from Dropbox.
 */

public class DownloadFile extends AsyncTask<Void, Long, Boolean> {


    private Context mContext;
    private final ProgressDialog mDialog;
    private DropboxAPI<?> mApi;
    private String mPath;
    //private ImageView mView;
    private Drawable mDrawable;

    private FileOutputStream mFos;

    private boolean mCanceled;
    private Long mFileLen;
    private String mErrorMsg;
    private String fileName;
    private String dPath;

    public DownloadFile(Context context, DropboxAPI<?> api, String f, String path) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context.getApplicationContext();
        fileName = f;
        mApi = api;
        dPath = path + f;

        mDialog = new ProgressDialog(context);
        mDialog.setMessage("Downloading File");
        mDialog.setButton(ProgressDialog.BUTTON_POSITIVE, "Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mCanceled = true;
                mErrorMsg = "Canceled";

                // This will cancel the getThumbnail operation by closing
                // its stream
                if (mFos != null) {
                    try {
                        mFos.close();
                    } catch (IOException e) {
                    }
                }
            }
        });

        mDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        String f_path = "/sdcard/" + fileName;
        File file= new File(f_path);
        boolean result=false;
        if (mCanceled) {
            return false;
        }
        try {
            mFos = new FileOutputStream(file);
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            if (mCanceled) {
                return false;
            }
            Log.w("Path",dPath);
            Log.w("Path",f_path);
            DropboxAPI.DropboxFileInfo info = mApi.getFile(dPath, null,mFos, null);
            Entry temp = mApi.metadata(dPath, 1000, null, true, null);
            mFileLen = temp.bytes;
            result=true;
        } catch (DropboxException e) {
            Log.e("DbExampleLog", "Something went wrong while downloading.");
            file.delete();
            result=false;
        }


        return result;
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        int percent = (int)(100.0*(double)progress[0]/mFileLen + 0.5);
        mDialog.setProgress(percent);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mDialog.dismiss();
        if (result) {
            // Set the image now that we have it
            //mView.setImageDrawable(mDrawable);
            Log.w("download","succees");
            showToast("Downloaded!");
        } else {
            // Couldn't download it, so show an error
            showToast(mErrorMsg);
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
        error.show();
    }


}
