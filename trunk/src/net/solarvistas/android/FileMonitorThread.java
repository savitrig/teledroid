package net.solarvistas.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import android.util.Log;


public class FileMonitorThread implements Runnable {

    //Map<String, Long> mFilesMap;
    Process mPNotify;
	int mRunningStatus;
    String mTarget = "/sdcard/teledroid";
    Map<String,ModificationInfo> mFileChanges = new LinkedHashMap<String,ModificationInfo>();
	final String inotify = "/data/inotify ";//data/net.ssolarvistas.android/libs/";
    final ArrayList<String> mask = new ArrayList<String>();

    final int TD_FM_RUNNING = 1;
    final int TD_FM_STOPED = 0;
    final int TD_FM_TERMINATING = -1;
    final int TD_FM_RESTARTING = 2;

    public FileMonitorThread() {
        InitiateMonitoredMaskList();
    }

    public void run() {
    	try {
    		String status;
    		BufferedReader input = invokeNotify(mTarget);
            mRunningStatus = TD_FM_RUNNING;
    		while(true){
				if ((status = input.readLine()) != null)
                    if(status.startsWith("[i]"))
                        ProcessINotifyEvent(status);

                switch(mRunningStatus){
                    case TD_FM_RUNNING:
                        break;
                    case TD_FM_RESTARTING:
                        input = invokeNotify(mTarget);
                        mRunningStatus = TD_FM_RUNNING;
                        break;
                    case TD_FM_TERMINATING:
                        mPNotify.destroy();
                        mRunningStatus = TD_FM_STOPED;
                    default:
                        throw new Exception("inotify process stoped.");
                }
    		}
    	} catch (Exception e) {
			Log.d("Teledroid.Monitor", "IOException", e);
    	}
    }

    private void InitiateMonitoredMaskList(){
        mask.add("CREATE");
        mask.add("CLOSE_WRITE");
        mask.add("ATTRIB");
        mask.add("DETELE");
        mask.add("DELETE_SELF");
        mask.add("MODIFY");
        mask.add("MOVE_SELF");
        mask.add("MOVED_FROM");
        mask.add("MOVED_TO");
    }
    
    private void ProcessINotifyEvent(String input) {
        String fileName= input.substring(3).split(", ")[0];
        String event = input.substring(3).split(", ")[1];
        if(mask.contains(event)) {
            File file = new File(fileName);
            if (!file.isDirectory()){
            	ModificationInfo.Kind modificationKind = event.contains("DELETE") ? ModificationInfo.Kind.DELETED : ModificationInfo.Kind.MODIFIED;
            	mFileChanges.put(fileName, new ModificationInfo(file.lastModified(), modificationKind));
                Log.d("teledroid", "noticed " + file + " had event:" + event);
            }
        }
        Log.d("teledroid", input);
    }

    public void setMRunningStatus(int mRunningStatus) {
        this.mRunningStatus = mRunningStatus;
    }

    public Map<String,ModificationInfo> getLatestChanges() {
        Map<String,ModificationInfo> result = mFileChanges;
        mFileChanges = new LinkedHashMap<String,ModificationInfo>();
    	return result;
    }

    public int getMRunningStatus() {
        return mRunningStatus;
    }

    private BufferedReader invokeNotify(String target) throws Exception {
    	String command = inotify + target;
		mPNotify = Runtime.getRuntime().exec(command);
		Log.d("teledroid", ">> FileMonitor Initiated Process $" + command);
		return new BufferedReader(new InputStreamReader(mPNotify.getInputStream()));
    }

    public void Terminate(){
        setMRunningStatus(TD_FM_TERMINATING);
    }
}
