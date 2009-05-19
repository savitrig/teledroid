package net.solarvistas.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.jcraft.jsch.Channel;

public class ScanFilesThread implements Runnable {
    private static final int PERIOD   = 5 * 1000;
    private static final int TIMEDIFF = 1 * 1000;
    public static boolean stopSignal = false;
    private JSONObject mServerJson;
    private JSONObject mLocalJson;
    /*Flag 0: Server to Local; Flag 1: Local to Server; */
    public enum Flag { ServerToClient, ClientToServer }
    
    //Connection mShell = null;

	public void run() {
		while (!stopSignal) {
			mLocalJson = new JSONObject(getFilesModifiedTime(AndroidFileBrowser.rootDirectory));
//				Log.d("Files Map", mFilesMap.toString());
			mServerJson = remoteDirscan("sdcard");
			autoSyn(mServerJson, mLocalJson, Flag.ServerToClient);
			autoSyn(mLocalJson, mServerJson, Flag.ClientToServer);
			
			try {
				Thread.sleep(PERIOD);
			} catch (InterruptedException e) {}
		}
	}
	
	
	private Map<String, Object> getFilesModifiedTime(File dir) {
		if (!dir.isDirectory()) {
			Log.e("teledroid", "getFilesModifiedTime was passed a file that isn't a directory: " + dir.getAbsolutePath());
			return null;
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>(); 
		
		for (File currentFile : dir.listFiles()) {
			if (currentFile.isFile())
				m.put(currentFile.getAbsolutePath().substring(1), currentFile.lastModified());
			else {
				Map<String, Object> dirMap = getFilesModifiedTime(currentFile.getAbsoluteFile());
				m.put(currentFile.getAbsolutePath().substring(1), dirMap);
			}	
		}
		return m;
	}
	
	private JSONObject remoteDirscan(String dirname) {
		try {
			Channel channel = BackgroundService.ssh.Exec("dir-print " + dirname + "\n");
			BufferedReader input = new BufferedReader(new InputStreamReader(channel
					.getInputStream()));
			String msg = null;
	    	while (true) {
	    		msg = input.readLine();
			
	    		//skip the echoed line of the command ran
				if (msg.contains("{")){
					channel.disconnect();
					break;
				}
	    	}
	    	return new JSONObject(msg);
		} catch (Exception e) {
			Log.e("teledroid.SynThread.run", "Error getting dir-print info from server");
			e.printStackTrace();
		}
		return null;
	}
    
	public static final int BUMP_MSG = 0x101;

    private void autoSyn(JSONObject o1, JSONObject o2, Flag flag) {    	
    	for (Iterator<String> i = o1.keys(); i.hasNext();) {
			String key = i.next();
			Object value1 = null;
			try {
				value1 = o1.get(key);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			try {
				Object value2 = o2.get(key);
				if (value1 instanceof Long && value2 instanceof Long)
					compareAndSyn(key, (Long)value1, (Long)value2, flag, (Long)value1);	
				else if (value1 instanceof JSONObject && value2 instanceof JSONObject)
					autoSyn((JSONObject)value1, (JSONObject)value2, flag);
				//TODO: else for one side Long, one side JSONObject;
			}
			catch (JSONException e) {
				if (value1 instanceof Long)
					syn(key, flag, (Long)value1);
				else
					syn(key, flag, null);
			}
			//Log.d("Key", (String)i.next());
		}
    }
    
    private void compareAndSyn(String key, Long v1, Long v2, Flag flag, Long value) {
    	if (v1 - v2 > TIMEDIFF) {
	    	Log.d("Test", Long.toString(v1 - v2));
    		syn (key, flag, value);
    	
    	}
    }
    
    private void syn(String fileName, Flag flag, Long value) {
    	String parentPath = "/home/teledroid/";
    	
    	switch (flag) {
    	case ServerToClient:
    		Log.d("teledroid","transferring " + fileName + " from server");
    		if (value != null) {
	    		BackgroundService.ssh.SCPFrom(parentPath+fileName, fileName);
	    		File f = new File(fileName);
	    		f.setLastModified(value);
    		}
    		break;
    	case ClientToServer:
    		BackgroundService.ssh.SCPTo(fileName, parentPath+fileName);
    	    String pattern = "yyyyMMddHHmm.ss";
    	    SimpleDateFormat format = new SimpleDateFormat(pattern);
			String formatDate = format.format((new Date(value)));
	    	Log.d("TestTime", formatDate);

    		try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		Connection con = BackgroundService.ssh;
    		Channel execChannel = null;
    		try {
				execChannel = con.Exec("touch -t "+formatDate+" "+parentPath+fileName+"\n");
			} catch (Exception e) {
				Log.e("teledroid.ScanFilesThread.syn", "unable to touch file " + parentPath+fileName);
				e.printStackTrace();
			}
	        if (execChannel != null) execChannel.disconnect();
    		break;
    	}
    }
   
}
