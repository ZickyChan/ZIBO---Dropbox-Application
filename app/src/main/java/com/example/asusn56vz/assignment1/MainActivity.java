package com.example.asusn56vz.assignment1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.os.Bundle;
import android.util.*;
import android.view.*;
import android.widget.*;
import android.view.View.*;

import com.dropbox.client2.*;
import com.dropbox.client2.android.*;
import com.dropbox.client2.DropboxAPI.*;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.*;

import java.util.*;
import java.io.*;


public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private boolean mLoggedIn;
    private Button btnLogin;
    final static private String APP_KEY ="bgqrs0983c7mizu"; //"lya4dlu8huyerzp";
    final static private String APP_SECRET ="ivw144d5x0wu1h7"; // "mdcbzsabghcbtkx";

    // You don't need to change these, leave them alone.
    private static final String ACCOUNT_PREFS_NAME = "prefs";
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";


    // In the class declaration section:
    DropboxAPI<AndroidAuthSession> mDBApi;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        // And later in some initialization function:
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = buildSession();
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        btnLogin = (Button) findViewById(R.id.login_btn);


        //Display Dialog to warn user enable 2-step verification
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Warning")
                .setMessage("You should enable two-step verification of your dropbox account to have better security!")
                .setPositiveButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

        btnLogin.setOnClickListener(new OnClickListener(){
            public void onClick(View v){
                if(mLoggedIn){
                    logOut();
                }
                else{
                    mDBApi.getSession().startOAuth2Authentication(MainActivity.this);
                }
            }
        });

        setLoggedIn(mDBApi.getSession().isLinked());



    }

    private void loadAuth(AndroidAuthSession session) {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;

        if (key.equals("oauth2:")) {
            // If the key is set to "oauth2:", then we can assume the token is for OAuth 2.
            session.setOAuth2AccessToken(secret);
        } else {
            // Still support using old OAuth 1 tokens.
            session.setAccessTokenPair(new AccessTokenPair(key, secret));
        }
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        loadAuth(session);
        return session;
    }
    private void storeAuth(AndroidAuthSession session) {
        // Store the OAuth 2 access token, if there is one.
        String oauth2AccessToken = session.getOAuth2AccessToken();
        if (oauth2AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, "oauth2:");
            edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.commit();
            return;
        }
        // Store the OAuth 1 access token, if there is one.  This is only necessary if
        // you're still using OAuth 1.
        AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
        if (oauth1AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
            edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
            edit.commit();
            return;
        }
    }
    private void setLoggedIn(boolean loggedIn) {
        mLoggedIn = loggedIn;
        if (loggedIn) {
            btnLogin.setText("Unlink from Dropbox");

            showDialog();
        } else {
            btnLogin.setText("Link with Dropbox");
        }
    }



    //Modify method to show some toasts
    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AndroidAuthSession session = mDBApi.getSession();

        // The next part must be inserted in the onResume() method of the
        // activity from which session.startAuthentication() was called, so
        // that Dropbox authentication completes properly.
        if (session.authenticationSuccessful()) {
            try {
                // Mandatory call to complete the auth
                session.finishAuthentication();

                // Store it locally in our app for later use
                storeAuth(session);
                setLoggedIn(true);
            } catch (IllegalStateException e) {
                showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
                Log.i(TAG, "Error authenticating", e);
            }
        }
    }

    //This method is built for clearing drop box key
    private void clearKeys() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }

    //Log out from drop box
    protected void logOut() {
        // Remove credentials from the session
        mDBApi.getSession().unlink();

        // Clear our stored keys
        clearKeys();
        // Change UI state to display logged out version
        setLoggedIn(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void showDialog() {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = new FolderListing(MainActivity.this,mDBApi,this);
        newFragment.show(ft, "dialog");
    }


}
