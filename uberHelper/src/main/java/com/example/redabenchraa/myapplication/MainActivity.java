package com.example.redabenchraa.myapplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.uber.sdk.core.auth.AccessToken;
import com.uber.sdk.rides.client.SessionConfiguration;
import com.uber.sdk.rides.client.model.SandboxProductRequestParameters;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    Context app;
    uberHelper uber;

    Button login,request,cancel,getinfo,accept;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        app = this;
        uber = new uberHelper(app);

        login = (Button) findViewById(R.id.login);
        request = (Button) findViewById(R.id.request_button);
        cancel = (Button) findViewById(R.id.request_cancel);
        getinfo = (Button) findViewById(R.id.request_info_button);
        accept = (Button) findViewById(R.id.ACCEPT_REQUEST);

        login.setOnClickListener(this);
        request.setOnClickListener(this);
        cancel.setOnClickListener(this);
        getinfo.setOnClickListener(this);
        accept.setOnClickListener(this);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; thisf(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear) {
            uber.accessTokenManager.removeAccessToken();
            Toast.makeText(this, "AccessToken cleared", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_copy) {
            AccessToken accessToken = uber.accessTokenManager.getAccessToken();
            String message = accessToken == null ? "No AccessToken stored" : "AccessToken copied to clipboard";
            if (accessToken != null) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("UberSampleAccessToken", accessToken.getToken());
                clipboard.setPrimaryClip(clip);
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uber.loginManager.onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.login :
                uber.login();
                break;
            case R.id.request_cancel :
                uber.cancelRide();
                break;
            case R.id.request_button :
                uber.requestRide("Uber HQ","1455 Market Street, San Francisco", 37.775304f, (float) -122.417522);
                break;
            case R.id.request_info_button :
                uber.getRequestStatus();
                break;
            case R.id.ACCEPT_REQUEST :
                uber.setRideSandBox(uberHelper.rideStatus.NO_DRIVERS_AVAILABLE);
                break;
        }
    }
}
