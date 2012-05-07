package com.shellware.FileSyncService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class FileSyncIntentReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent serviceIntent = new Intent();
		serviceIntent.setAction("com.shellware.FileSyncService.MyFileObserver");
		context.startService(serviceIntent);
	}
}