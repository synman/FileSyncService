package com.shellware.FileSyncService;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class FileSyncActivity extends Activity {

	private Context ctx;
	
	private Handler serviceStatusHandler;
	private ToggleButton serviceStatus;
	
	private EditText serverName;
	private EditText userName;
	private EditText password;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ctx = this;
		this.setTitle(getString(R.string.app_title));
		
		setContentView(R.layout.main);
		
		serverName = (EditText) findViewById(R.id.serverName);
		userName = (EditText) findViewById(R.id.userName);
		password = (EditText) findViewById(R.id.password);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		
		serverName.setText(prefs.getString("servername", "ftp.shellware.com"));
		userName.setText(prefs.getString("username", ""));
		password.setText(prefs.getString("password", ""));
		
		serviceStatus = (ToggleButton) findViewById(R.id.serviceToggle);
		serviceStatus.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				Intent serviceIntent = new Intent();
				serviceIntent.setAction("com.shellware.FileSyncService.MyFileObserver");

				if (isChecked) {				
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
					Editor edit = prefs.edit();
					
					edit.putString("servername", serverName.getText().toString());
					edit.commit();
					
					edit.putString("username", userName.getText().toString());
					edit.commit();
					
					edit.putString("password", password.getText().toString());
					edit.commit();
					
					getApplicationContext().startService(serviceIntent);
				} else {
					getApplicationContext().stopService(serviceIntent);
				}
			}
		});
		
		serviceStatusHandler = new Handler();
	}

	@Override
	protected void onResume() {
		super.onResume();	
		serviceStatusHandler.postDelayed(CheckServiceStatus, 1000);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		serviceStatusHandler.removeCallbacks(CheckServiceStatus);
	}

	private Runnable CheckServiceStatus = new Runnable() {
		public void run() {     		

			serviceStatus.setChecked(isMyServiceRunning());
			serviceStatusHandler.postDelayed(CheckServiceStatus, 1000);		   
		}
	};
   
	private boolean isMyServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if ("com.shellware.FileSyncService.MyFileObserver".equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
}
