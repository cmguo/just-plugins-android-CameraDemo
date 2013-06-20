package com.pplive.demo;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;

public class CameraActivity extends Activity {

	private SurfaceView surface;
	
	private PpboxSink capture;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		// Show the Up button in the action bar.
		setupActionBar();
		
		Intent intent = getIntent();
		Bundle bl = intent.getExtras();
		String url = bl.getString("url");

		surface = (SurfaceView)findViewById(R.id.surfaceView);
		surface.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		PpboxSink.init(getApplicationContext());
		capture = new PpboxSink();
		capture.open(url);
	}


	@Override
	protected void onDestroy() {
		capture.close();
		
		super.onDestroy();
	}
	
	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.camera, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onBuntonPreviewClick(View v)
	{
		capture.preview(surface.getHolder());
	}

	public void onBuntonPlayClick(View v)
	{
		capture.start();
	}

	public void onBuntonStopClick(View v)
	{
		capture.stop();
	}

	public void onBuntonBackClick(View v)
	{
		Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
	}
}
