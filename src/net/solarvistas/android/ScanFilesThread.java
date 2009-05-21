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

import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;

public class ScanFilesThread implements Runnable {
    private static final int PERIOD   = 5 * 1000;
    private static final int TIMEDIFF = 1 * 1000;
    public static boolean stopSignal = false;
    public enum Direction { ServerToClient, ClientToServer }
    private BackgroundService bs;
    private FileMonitorThread localMonitor;
    public ScanFilesThread(BackgroundService bs) {
    	this.bs = bs;
    }
    private Map<String,ModificationInfo> serverInfo = null, localInfo = null;

	final String remoteDir = "sdcard";
	final File localDir  = AndroidFileBrowser.rootDirectory;
	Channel remoteChangeStream = null;
	OutputStream out;
	BufferedReader in;

	public void run() {
		go();
		onFinished();
		Log.i("teledroid", "ScanFilesThread ended");
	}
	
	public void go() {

		
		//do one initial scan to get synchronized
		dirScan(localDir, remoteDir);
		
		try {
			connectMonitorOutputStream();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		while (!stopSignal) {
			if(BackgroundService.mSyncMode == BackgroundService.SYNC_MODE_MONITOR)
				onlyChanges(localDir, remoteDir);
			else {
				switchMode();
				dirScan(localDir, remoteDir);
			}	

			/*if (remoteChangeStream != null && remoteChangeStream.isConnected())
				remoteChangeStream.disconnect();*/
			
			List<SyncAction> syncActions = getSynchronizationActions(serverInfo, localInfo);
			
			int successfulCount = 0;
			if (syncActions.size() != 0) {
				bs.beginSyncNotification(syncActions);
				for (SyncAction action : syncActions){
					if (stopSignal){
						bs.syncInterruptedNotification(successfulCount, syncActions.size()-successfulCount);
						return;
					}
					sync(action);
					successfulCount++;
				}
				bs.finishedSyncNotification(syncActions);
			}

			if (stopSignal) return;
			try {
				Thread.sleep(PERIOD);
			} catch (InterruptedException e) {}
		}
	}
	
	private void onFinished() {
		switchMode();
		if (remoteChangeStream.isConnected())
			remoteChangeStream.disconnect();
	}
	
	private void switchMode() {
		if (localMonitor != null) {
			localMonitor.stopSignal = true;
			localMonitor = null;
		}	
	}
	
	private void dirScan(File localDir, String remoteDir) {
		serverInfo = remoteDirscan(remoteDir);
		localInfo  = localDirscan(localDir);
	}
	
	private void onlyChanges (File localDir, String remoteDir) {
//		try {
//			remoteChangeStream = BackgroundService.ssh.Exec("pull-sync " + remoteDir + "\n");
//			//out = remoteChangeStream.getOutputStream();
//		} catch (Exception e) {
//			Log.e("teledroid", "unable to open remoteChangeStream",e);
//			return;
//		}
		if (localMonitor == null) {
			localMonitor = new FileMonitorThread();
			new Thread(localMonitor, "Local monitor thread").start();
		}
		localInfo  = localMonitor.getLatestChanges();
		serverInfo = getRemoteChanges(remoteChangeStream);
		Log.i("teledroid", "inotify found " + localInfo.size() + " local changes");
		Log.i("teledroid", "server found " + serverInfo.size() + " remote changes");
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
			out = remoteChangeStream.getOutputStream();
			out.write("\n".getBytes()); out.flush();
			return fetchJSON(in, 1000);
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
			BufferedReader input2 = new BufferedReader(new InputStreamReader(chan.getInputStream()), 8 * 1024);
			result = fetchJSON(input2, 8 * 1000);
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
	private Map<String,ModificationInfo> fetchJSON(BufferedReader input, long timeout) throws IOException, JSONException {
		//BufferedReader input = new BufferedReader(new InputStreamReader(channel.getInputStream()), 8 * 1024);
		String msg = "";
		
		//skip the echoed line of the command ran
		while (!msg.contains("{")) {
			msg = input.readLine();
			Log.v("teledroid", "read from server: " + msg);
		}
	

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
//				if (direction == Direction.ServerToClient)
//					filename = filename.substring(24);
				actions.push(new SyncAction(filename, direction, mod1));
				continue;
			}
			
			ModificationInfo mod2 = o2.get(filename);
			if (mod1.mtime - mod2.mtime > TIMEDIFF)
//				if (direction == Direction.ServerToClient)
//					filename = filename.substring(24);
				actions.push(new SyncAction(filename, direction, mod1));
		}
    }
    
    private void sync(SyncAction action) {
    	String parentPath = null;
    	final int validPathIndex = 23;
		String localFileName = null;

    	switch (BackgroundService.mSyncMode) {
    	case BackgroundService.SYNC_MODE_SCAN:
    		parentPath = "/home/teledroid/";
    		localFileName = action.filename;
    		break;
    	case BackgroundService.SYNC_MODE_MONITOR:
    		parentPath = "/home/.urban/teledroid/";
    		if (action.direction == Direction.ServerToClient)
    			localFileName = action.filename.substring(validPathIndex);
    	case BackgroundService.SYNC_MODE_LAZY: break;

    	}
    	    	
    	switch (action.direction) {
    	case ServerToClient:
    		Log.d("teledroid","transferring " + action.filename + " from server");
	    	if (localMonitor != null)
	    		localMonitor.unregisterFile(localFileName);
    		BackgroundService.ssh.SCPFrom(action.filename, localFileName);
	    	File f = new File(localFileName);
	    	f.setLastModified(action.modificationInfo.mtime);
	    	if (localMonitor != null)
	    		localMonitor.registerFile(localFileName);
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
    
    private void connectMonitorOutputStream() throws Exception {
    	remoteChangeStream = BackgroundService.ssh.session.openChannel("shell");
    	remoteChangeStream.connect();
		out = remoteChangeStream.getOutputStream();
		in = new BufferedReader(new InputStreamReader(remoteChangeStream.getInputStream()), 8 * 1024);

		String command = "pull-sync " + remoteDir + "\n";
		out.write(command.getBytes());
		out.flush();
		Log.d("teledroid.Connection.Exec", "sent command `" + command + "` to server");
    }
}
