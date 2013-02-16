package de.robv.android.xposed.mods.smileys;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Movie;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import de.robv.android.xposed.mods.smileys.views.MovieSpan;

public class PreviewActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_preview);

		if (Common.XPOSED_ACTIVE)
			findViewById(R.id.xposed_not_active).setVisibility(View.GONE);

		Button btnChoose = (Button) findViewById(R.id.btn_choose);
		btnChoose.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(PreviewActivity.this, ChooserActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET); 
				startActivityForResult(intent, Common.REQUEST_CHOOSE_PACK);
			}
		});
		
        Button btnRestart = (Button) findViewById(R.id.btn_restart);
        btnRestart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendBroadcast(new Intent(Common.KILL_INTENT));

				try {
					// sendBroadcast is asynchronous, so wait a bit to make sure the old process is gone
					Thread.sleep(1000);
				} catch (InterruptedException ignored) {}

				Intent launchIntent = getPackageManager().getLaunchIntentForPackage(Common.MMS_PACKAGE);
				if (launchIntent != null) {
					startActivity(launchIntent);
				} else {
					String toastText = getString(R.string.could_not_launch_mms, Common.MMS_PACKAGE);
					Toast.makeText(PreviewActivity.this, toastText, Toast.LENGTH_SHORT).show();
				}
			}
		});
        
        //testMovieSpan();
        
        if (savedInstanceState == null) {
        	loadPreviewFragment();
        }
	}

	private void loadPreviewFragment() {
		@SuppressLint("WorldReadableFiles")
		final SharedPreferences pref = getSharedPreferences(Common.PREF_FILE, Context.MODE_WORLD_READABLE);
    	String filename = pref.getString(Common.PREF_SELECTED_PACK, null);
    	File file = null;
    	if (filename != null) {
    		file = new File(filename);
    		if (!file.canRead()) {
    			file = null;
    			pref.edit().remove(Common.PREF_SELECTED_PACK).commit();
    		}
    	}
    	
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment newFragment = PreviewFragment.newInstance(file);
        ft.replace(R.id.preview_fragment, newFragment);
        ft.commit();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case Common.REQUEST_CHOOSE_PACK:
				if (resultCode == Activity.RESULT_OK)
					loadPreviewFragment();
				break;
		}
	}
	
	
	
	@SuppressWarnings("unused")
    private void testMovieSpan() {
    	Movie movie = null;
    	try {
    		InputStream is = new BufferedInputStream(new FileInputStream(new File(getFilesDir(), "test.gif")), 16 * 1024);
    		is.mark(16 * 1024);
    		movie = Movie.decodeStream(is);
    	} catch (FileNotFoundException e) {
    		e.printStackTrace();
    	}
    	final TextView tv = (TextView) findViewById(R.id.preview_explanation);
    	tv.setTextSize(40);
    	SpannableStringBuilder builder = new SpannableStringBuilder(":-)\nTest\nTest :-)\n:-)\n:-)");
    	int starts[] = { 0, 14, 18, 22};
    	for (int start : starts) {
    		builder.setSpan(new MovieSpan(movie, MovieSpan.ALIGN_BOTTOM), start, start + 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    	}
    	tv.setText(builder);
        
        final Handler handler = new Handler();
        handler.post(new Runnable() {
        	@Override
        	public void run() {
        		tv.postInvalidate();
        		long now = SystemClock.uptimeMillis();
        		long rate = 50; 
        		handler.postAtTime(this, now + rate - (now % rate));
        	}
        });
    }
}
