package net.solarvistas.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.jcraft.jsch.Channel;

public class ScanFilesThread implements Runnable {
    private static final int PERIOD   = 5 * 1000;
    private static final int TIMEDIFF = 1 * 1000;
    public static boolean stopSignal = false;
    public enum Direction { ServerToClient, ClientToServer }
    
	public void run() {
	    Map<String,ModificationInfo> serverInfo = null, localInfo = null;
		final String remoteDir = "sdcard";
		final File localDir  = AndroidFileBrowser.rootDirectory;
		Channel remoteChangeStream = null;
		FileMonitorThread localMonitor = new FileMonitorThread();
		new Thread(localMonitor).start();
		
		while (!stopSignal) {
//				Log.d("Files Map", mFilesMap.toString());
			if (serverInfo == null) {
				serverInfo = remoteDirscan(remoteDir);
				localInfo  = localDirscan(localDir);
				try {
					remoteChangeStream = BackgroundService.ssh.Exec("pull-sync " + remoteDir + "\n");
				} catch (Exception e) {
					Log.e("teledroid", "Unable to open pull-sync connection to server");
					e.printStackTrace();
				}
			}
			else {
				serverInfo = remoteDirscan(remoteDir);
				localInfo  = localDirscan(localDir);
//				localInfo  = localMonitor.getLatestChanges();
//				serverInfo = getRemoteChanges(remoteChangeStream);
			}
			if (localInfo == null) return;
			autoSyn(serverInfo, localInfo, Direction.ServerToClient);
			autoSyn(localInfo, serverInfo, Direction.ClientToServer);

			
			try {
				Thread.sleep(PERIOD);
			} catch (InterruptedException e) {}
		}
	}
	
	
	private Map<String,ModificationInfo> getRemoteChanges(Channel remoteChangeStream) {
		Log.v("teledroid","Fetching changes");
		try {
			OutputStream out = remoteChangeStream.getOutputStream();
			out.write("\n".getBytes()); out.flush();
			return fetchJSON(remoteChangeStream);
		} catch (Exception e) {
			Log.e("teledroid", "Error getting changes from server");
			e.printStackTrace();
		}
		
		return null;
	}


	private Map<String, ModificationInfo> localDirscan(final File dir) {
		if (!dir.isDirectory()) {
			Log.e("teledroid", "getFilesModifiedTime was passed a file that isn't a directory: " + dir.getAbsolutePath());
			return null;
		}
		final Map<String, ModificationInfo> m = new LinkedHashMap<String, ModificationInfo>(); 
		Stack<File> dirStack = new Stack<File>();
		dirStack.add(dir);
		while(!dirStack.empty()){
			final File currentDir = dirStack.pop();
			for (File currentFile : currentDir.listFiles()) {
				if (currentFile.isDirectory())
					dirStack.push(currentFile);
				else
					m.put(currentFile.getAbsolutePath().substring(1), new ModificationInfo(currentFile.lastModified()));
			}
		}
		
		return m;
	}
	
	private Map<String,ModificationInfo> remoteDirscan(String dirname) {
		Log.v("teledroid","Running remote dirscan");
		Map<String,ModificationInfo> result = null;
		Channel chan = null;
		try {
			chan = BackgroundService.ssh.Exec("dir-print " + dirname + "\n");
			result = fetchJSON(chan);
		} catch (Exception e) {
			Log.e("teledroid", "Error getting dir-print info from server");
			e.printStackTrace();
		}
		finally {
			if (chan != null) chan.disconnect();
		}
		return result;
	}


	@SuppressWarnings("unchecked")
	private Map<String,ModificationInfo> fetchJSON(Channel channel) throws IOException, JSONException {
		BufferedReader input = new BufferedReader(new InputStreamReader(channel
								   .getInputStream()), 8 * 1024);
		String msg = "";
		
		//skip the echoed line of the command ran
		while (!msg.contains("{"))
			msg = input.readLine();

		JSONObject jsonMap = new JSONObject(msg);
		Map<String,ModificationInfo> result = new LinkedHashMap<String,ModificationInfo>(jsonMap.length());
		for (Iterator i = jsonMap.keys(); i.hasNext();) {
			String key = (String)i.next();
			Long value = jsonMap.optLong(key);
			if (value == null){
				//TODO: handle deleted files
				Log.e("teledroid", "unable to handle value: " + jsonMap.opt(key));
				continue;
			}
			
			result.put(key, new ModificationInfo(value));
		}
		return result;
	}
    
	public static final int BUMP_MSG = 0x101;

    private void autoSyn(Map<String,ModificationInfo> o1, Map<String,ModificationInfo> o2, Direction direction) {    	
    	for (String filename : o1.keySet()) {
			ModificationInfo value1 = o1.get(filename);
			if (!o2.containsKey(filename)){
				syn(filename, direction, value1.mtime);
				continue;
			}
			
			ModificationInfo value2 = o2.get(filename);
			compareAndSyn(filename, value1.mtime, value2.mtime, direction);	
		}
    }
    
    private void compareAndSyn(String filename, Long mtime1, Long mtime2, Direction direction) {
    	if (mtime1 - mtime2 > TIMEDIFF) {
	    	Log.d("Test", Long.toString(mtime1 - mtime2));
	    	syn (filename, direction, mtime1);
    	}
    }
    
    private void syn(String fileName, Direction direction, Long canonicalModifiedTime) {
    	String parentPath = "/home/teledroid/";
    	
    	switch (direction) {
    	case ServerToClient:
//    		Log.d("teledroid","transferring " + fileName + " from server");
    		if (canonicalModifiedTime != null) {
	    		BackgroundService.ssh.SCPFrom(parentPath+fileName, fileName);
	    		File f = new File(fileName);
	    		f.setLastModified(canonicalModifiedTime);
    		}
    		break;
    	case ClientToServer:
    		BackgroundService.ssh.SCPTo(fileName, parentPath+fileName);
    	    String pattern = "yyyyMMddHHmm.ss";
    	    SimpleDateFormat format = new SimpleDateFormat(pattern);
			String formatDate = format.format((new Date(canonicalModifiedTime)));

    		Channel execChannel = null;
    		try {
				execChannel = BackgroundService.ssh.Exec("touch -t "+formatDate+" "+parentPath+fileName+"\n");
			} catch (Exception e) {
				Log.e("teledroid", "unable to touch file " + parentPath+fileName);
				e.printStackTrace();
			}
	        if (execChannel != null) execChannel.disconnect();
    		break;
    	}
    }
   
}
