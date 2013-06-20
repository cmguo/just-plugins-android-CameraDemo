package com.pplive.demo;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;
import android.net.Uri;
import android.webkit.JavascriptInterface;

public class IntentPlayer {

	private Activity owner;
	
	public IntentPlayer(Activity owner)
	{
		this.owner = owner;
	}
	
	@JavascriptInterface
	public void play(final String url)
	{
		owner.runOnUiThread(new Runnable() {
			@Override
			public void run()
			{
		        try
		        {
		            Intent intent = new Intent(Intent.ACTION_VIEW);
		            intent.setDataAndType(Uri.parse(url), "video/*");
		            // intent.setData(Uri.parse(value));
		
		            owner.startActivity(Intent.createChooser(intent, "Select Player"));
		        }
		        catch (Exception e)
		        {
		            Toast.makeText(owner, "Open Failed", Toast.LENGTH_SHORT).show();
		        }
			}
		});
	}
}
