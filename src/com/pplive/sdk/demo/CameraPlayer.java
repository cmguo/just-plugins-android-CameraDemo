package com.pplive.sdk.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;
import android.webkit.WebViewClient;

public class CameraPlayer extends WebViewClient {

	private Activity owner;
	
	public CameraPlayer(Activity owner)
	{
		this.owner = owner;
	}
	
	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
	{
		capture(null);
	}
	
	@JavascriptInterface
	public void capture(final String url)
	{
		owner.runOnUiThread(new Runnable() {
			@Override
			public void run()
			{
		        try
		        {
		            Intent intent = new Intent(
		            		owner.getBaseContext(), CameraActivity.class);
		            Bundle bl = new Bundle();
					bl.putString("url", url);
					intent.putExtras(bl);
		            owner.startActivityForResult(intent, 0);
		        }
		        catch (Exception e)
		        {
		            Toast.makeText(owner, "Open Failed", Toast.LENGTH_SHORT).show();
		        }
			}
		});
	}

}
