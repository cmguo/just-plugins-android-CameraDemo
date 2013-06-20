package com.pplive.sdk.demo;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.Menu;
import android.webkit.WebView;

@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends Activity {

    private WebView mWebView;

    private CameraPlayer player;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		player = new CameraPlayer(this);
		
		mWebView = (WebView) findViewById(R.id.webview);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.addJavascriptInterface(player, "android_capture");
		mWebView.setWebViewClient(player);
		mWebView.loadUrl("http://192.168.14.205/test-page/capture.htm");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
