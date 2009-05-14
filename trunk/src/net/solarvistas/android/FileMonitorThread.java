package net.solarvistas.android;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;


public class FileMonitorThread implements Runnable {

    //Map<String, Long> mFilesMap;
	String target = "/data/tmp";///sdcard";
	BackgroundService bs;
	Process notify, testp;
	final String app_path = "/data/data/net.ssolarvistas.android/libs/";
	
    public FileMonitorThread(BackgroundService bs) {
    	this.bs = bs;
    }

    public void run() {
    	try {
    		String status;
    		BufferedReader input = invokeNotify(target);
    		BufferedReader error = new BufferedReader(new InputStreamReader(notify.getErrorStream()));
    		BufferedReader test = new BufferedReader(new InputStreamReader(testp.getInputStream()));
    		Log.d("teledroid", "+=============================================+");
    		while(true){
				if ((status = input.readLine()) != null)
					Log.d("teledroid", status);
				if ((status = error.readLine()) != null)
					Log.d("teledroid", "[Error]" + status);
				if ((status = test.readLine()) != null)
					Log.d("teledroid", "[Test]" + status);
    		}
    	} catch (Exception e) {
			Log.d("Teledroid.Monitor", "IOException", e);
    	}
    }

    private BufferedReader invokeNotify(String target) throws Exception {
    	String command = app_path + "inotify -c 0 " + target;
		notify = Runtime.getRuntime().exec(command);
		Log.d("teledroid", "FileMonitor Initiated Process $" + command);
		
		command = app_path + "inotify -c " + target;
		testp = Runtime.getRuntime().exec(command);
		return new BufferedReader(new InputStreamReader(notify.getInputStream()));
    }
}
