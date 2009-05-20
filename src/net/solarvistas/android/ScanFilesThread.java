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
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.jcraft.jsch.Channel;

public class ScanFilesThread implements Runnable {
    private static final int PERIOD   = 5 * 1000;
    private static final int TIMEDIFF = 1 * 1000;
    public static boolean stopSignal = false;
    public enum Direction { ServerToClient, ClientToServer }
    private BackgroundService bs;
    
    public ScanFilesThread(BackgroundService bs) {
    	this.bs = bs;
    }
    
	public void run() {
		Looper.prepare(); //for making toast notifications
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
			
			List<SyncAction> syncActions = getSynchronizationActions(serverInfo, localInfo);
			
			if (syncActions.size() != 0) {
				bs.beginSyncNotification(syncActions);
				for (SyncAction action : syncActions){
					if (stopSignal) return;
					sync(action);
				}
				bs.finishedSyncNotification(syncActions);
			}

			if (stopSignal) return;
			try {
				Thread.sleep(PERIOD);
			} catch (InterruptedException e) {}
		}
	}
	
	
	private List<SyncAction> getSynchronizationActions(Map<String, ModificationInfo> serverInfo, Map<String, ModificationInfo> localInfo) {
		Stack<SyncAction> results = new Stack<SyncAction>();
		autoSyn(serverInfo, localInfo, Direction.ServerToClient, results);
		autoSyn(localInfo, serverInfo, Direction.ClientToServer, results);
		return results;
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

    private void autoSyn(Map<String,ModificationInfo> o1, Map<String,ModificationInfo> o2, Direction direction, Stack<SyncAction> actions) {    	
    	for (String filename : o1.keySet()) {
			ModificationInfo mod1 = o1.get(filename);
			if (!o2.containsKey(filename)){
				actions.push(new SyncAction(filename, direction, mod1));
				continue;
			}
			
			ModificationInfo mod2 = o2.get(filename);
			if (mod1.mtime - mod2.mtime > TIMEDIFF)
				actions.push(new SyncAction(filename, direction, mod1));
		}
    }
    
    private void sync(SyncAction action) {
    	String parentPath = "/home/teledroid/";
    	
    	switch (action.direction) {
    	case ServerToClient:
//    		Log.d("teledroid","transferring " + fileName + " from server");
    		BackgroundService.ssh.SCPFrom(parentPath+action.filename, action.filename);
	    	File f = new File(action.filename);
	    	f.setLastModified(action.modificationInfo.mtime);
    		break;
    	case ClientToServer:
    		BackgroundService.ssh.SCPTo(action.filename, parentPath+action.filename);
    	    String pattern = "yyyyMMddHHmm.ss";
    	    SimpleDateFormat format = new SimpleDateFormat(pattern);
			String formatDate = format.format((new Date(action.modificationInfo.mtime)));

    		Channel execChannel = null;
    		try {
				execChannel = BackgroundService.ssh.Exec("touch -t "+formatDate+" "+parentPath+action.filename+"\n");
			} catch (Exception e) {
				Log.e("teledroid", "unable to touch file " + parentPath+action.filename);
				e.printStackTrace();
			}
	        if (execChannel != null) execChannel.disconnect();
    		break;
    	}
    }
   
}
