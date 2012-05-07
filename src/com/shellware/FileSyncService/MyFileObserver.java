package com.shellware.FileSyncService;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class MyFileObserver extends Service {
	
	private Context ctx;
	
	private Observer cameraObserver;
	private Observer downloadsObserver;
	
	private ZombieThread myThread;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	
		ctx = this;
		
		cameraObserver = new Observer(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera");
		downloadsObserver = new Observer(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download");
		
		myThread = new ZombieThread();
		
		Log.d("FileSyncService", "Service Created");
	}

	@Override
	public void onDestroy() {
		cameraObserver.stopWatching();
		downloadsObserver.stopWatching();
		
		if (myThread.isRunning()) {
			myThread.setRunning(false);
			try {
				myThread.join();
			} catch (InterruptedException e) {
				// do nothing
			}
		}
		
		super.onDestroy();

		Log.d("FileSyncService", "Service Destroyed");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		if (!myThread.isRunning()) {
			myThread.start();
		}
		
		cameraObserver.startWatching();
		downloadsObserver.startWatching();

		Log.d("FileSyncService", "Service Started");
	}
	
    private static String getEventString(int event) {
        switch (event) {
            case  android.os.FileObserver.ACCESS:
                return "ACCESS";
            case android.os.FileObserver.MODIFY:
                return "MODIFY";
            case android.os.FileObserver.ATTRIB:
                return "ATTRIB";
            case android.os.FileObserver.CLOSE_WRITE:
                return "CLOSE_WRITE";
            case android.os.FileObserver.CLOSE_NOWRITE:
                return "CLOSE_NOWRITE";
            case android.os.FileObserver.OPEN:
                return "OPEN";
            case android.os.FileObserver.MOVED_FROM:
                return "MOVED_FROM";
            case android.os.FileObserver.MOVED_TO:
                return "MOVED_TO";
            case android.os.FileObserver.CREATE:
                return "CREATE";
            case android.os.FileObserver.DELETE:
                return "DELETE";
            case android.os.FileObserver.DELETE_SELF:
                return "DELETE_SELF";
            case android.os.FileObserver.MOVE_SELF:
                return "MOVE_SELF";
            default:
                return "UNKNOWN - " + event;
        }
    }
    
    private class Observer extends FileObserver {
    	private String path;

        public Observer(String path) {
            //super(path, FileObserver.CLOSE_WRITE);
        	super(path, FileObserver.ALL_EVENTS);
            this.path = path;
        }

        public void onEvent(int event, String file) {
        	
        	if (file != null && path != null && (event == FileObserver.CLOSE_WRITE || event == FileObserver.MOVED_TO)) {
            	if (!file.equals("temp_video")) {
                	Log.d("FileSyncService", "event: " + getEventString((Integer) event) + " file: [" + file + "]");  

                	Thread doTransfer = new TransferFileThread(path, file);
	            	doTransfer.start();
            	}
        	}
        }
        
        private class TransferFileThread extends Thread {        	
        	private String path;
        	private String file;

        	public TransferFileThread(String path, String file) {
				super();
				
				this.path = path;
				this.file = file;
			}

			@Override
			public void run() {
				super.run();
				
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

				while (true) {
					try {
						final String serverName = prefs.getString("servername", "ftp.shellware.com");
						final String userName = prefs.getString("username", "");
						final String password = prefs.getString("password", "");

						FTPClient ftp = new FTPClient();
		
		            	ftp.setConnectTimeout(15000);
		            	ftp.setDefaultTimeout(15000);
		            	ftp.setBufferSize(2048);
		        		
		        		InputStream is = new FileInputStream(path + "/" + file);
		            	ftp.connect(serverName);
		            	
		                // After connection attempt, you should check the reply code to verify
		                // success.
		                final int reply = ftp.getReplyCode();
		
		                if(!FTPReply.isPositiveCompletion(reply)) {
		                  ftp.disconnect();
		                  throw new Exception("connect failed - code " + reply);
		                }
		                
		                if (!ftp.login(userName, password)) {
		                    ftp.disconnect();
		                    throw new Exception("login failed");
		                }
		
		                ftp.setFileType(FTP.BINARY_FILE_TYPE);
		    			ftp.enterLocalPassiveMode();
		    			
		    			if (!ftp.storeFile(file, is)) {
		    				throw new Exception("transfer failed");
		    			}
		
		    			// close our inputstream and connection
		    			is.close();
		    			ftp.logout();
		    			ftp.disconnect();
		    			
		        		Log.d("FileSyncService", file + " transferred");
		        		return;
		
		        	} catch (Exception e) {
		        		Log.e("FileSyncService", e.getCause() + " " + e.getMessage());
		        		e.printStackTrace();
		    		}

		        	try {
						Thread.sleep(60000);
					} catch (InterruptedException e1) {
						// do nothing
					}
				}
			}
        }
    }
    
    private class ZombieThread extends Thread {        	
    	private boolean running = false;

		@Override
		public void run() {
			super.run();
			
			running = true;
			
			while (running) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// do nothing
				}
			}
		}

		/**
		 * @return the running
		 */
		public boolean isRunning() {
			return running;
		}

		/**
		 * @param running the running to set
		 */
		public void setRunning(boolean running) {
			this.running = running;
		}
		
		
	}
}
