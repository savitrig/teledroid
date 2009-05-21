package net.solarvistas.android;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import net.solarvistas.android.AndroidFileBrowser;
import net.solarvistas.android.ModificationInfo;
import android.util.Log;

public class FileMonitorThread implements Runnable {
    
	public boolean stopSignal;
    private int mNFD;
    
    public FileMonitorThread() {
    	stopSignal = false;
    }
    
    Map<String,ModificationInfo> mFileChanges;
    Map<Integer,String>	mFileList = new LinkedHashMap<Integer, String>();
    
    public void run() {
    	//final String remoteDir = "sdcard";
    	mFileChanges = new LinkedHashMap<String, ModificationInfo>();
    	try{
    		mNFD = Notify.initNotify();
    		Log.i("teledroid", "notify initiated, get nfd " + mNFD + ".");
    	}catch(Exception e){
    		Log.e("teledroid", "initNotify", e);
    	}
    	registerDir(AndroidFileBrowser.rootDirectory);
    	while(Notify.hasNext(mNFD)){
    		if (stopSignal) break;
    		interpEvent(Notify.nextEvent());
    	}
    		
    	Log.i("teledroid", "File monitor thread ended.");
    }

    public void registerDir(final File dir) {
		if (!dir.isDirectory()) {
			Log.e("teledroid", "registerDir was passed a file that isn't a directory: " +  
					dir.getAbsolutePath());
		}
		Log.i("teledroid", "Registering directory " + dir.getAbsolutePath() + ".");
		
		Stack<File> dirStack = new Stack<File>();
		dirStack.add(dir);
		while(!dirStack.empty()){
			final File currentDir = dirStack.pop();
			//TODO:	Further implementation may watch directory, current file only.
			registerFile(currentDir.getAbsolutePath());
			for (File currentFile : currentDir.listFiles()) {
				if (currentFile.isDirectory())
					dirStack.push(currentFile);
				else
					registerFile(currentFile.getAbsolutePath());
			}
		}
	}
    
    public void registerFile(String file) {
    	if( !mFileList.containsValue(file)){
    		Integer wd = Notify.registerFile(mNFD, file, Notify.IN_ALL_EVENTS);//NOTIFY_MONITOR);
    		if( wd > 0){
    			Log.d("teledroid", "Registering file " + file + " get wd:" + wd +".");
    			mFileList.put(wd, file);
    		}else
    			Log.e("teledroid", "Unable to register file " + file + ".");
    	}
    }
    
    public void unregisterFile(int wd){
    	String file = mFileList.get(Integer.valueOf(wd));
    	if( file != null ){
    		int res = Notify.unregisterFile(mNFD, wd);
    		if( res > 0){
    			mFileList.remove(Integer.valueOf(wd));
    			Log.d("teledroid", "Unregistering file " + file + " with wd:" + wd +".");
    		}else
    			Log.e("teledroid", "Unable to unregister file " + file + ". Error: " + res);
    	}
    }
    private void interpEvent(int event){
    	Integer fileNum = event;
    	Object filename = mFileList.get(fileNum);
    	if(filename != null){
    		event = Notify.eventMask();
    		
    		switch(event){
    		case Notify.IN_CREATE:
    		case Notify.IN_MOVED_TO:
    			mFileChanges.put(filename.toString() + "/" + Notify.newFile(), new ModificationInfo(
        				(new File(filename.toString())).lastModified()));
    			registerFile(filename.toString() + "/" + Notify.newFile());
    			Log.v("teledroid", "[" + mFileChanges.size()+ "]\tFile " + Notify.newFile() + " created in / moved to " + filename);
    			break;
    		case Notify.IN_DELETE:
    		case Notify.IN_MOVED_FROM:
    			mFileChanges.put(filename.toString() + "/" + Notify.newFile(), new ModificationInfo(
    					(new File(filename.toString())).lastModified(), ModificationInfo.Kind.DELETED));
    			Log.v("teledroid", "[" + mFileChanges.size()+ "]\tFile " + Notify.newFile() + " deleted/moved from " + filename);
    			break;
    		case Notify.IN_DELETE_SELF:
    		case Notify.IN_MOVE_SELF:
    			mFileChanges.put(filename.toString(), new ModificationInfo(
        				(new File(filename.toString())).lastModified(), ModificationInfo.Kind.DELETED));
    			mFileList.remove(fileNum);
    			Log.v("teledroid", "[" + mFileChanges.size()+ "]\tFile " + filename + " deleted/moved");
    			break;
    		case Notify.IN_CLOSE_WRITE:
    			File file = new File(filename.toString());
    			if(!file.isDirectory())
    				mFileChanges.put(filename.toString(), new ModificationInfo(
    						file.lastModified()));
    			Log.v("teledroid", "[" + mFileChanges.size()+ "]\tFile " + filename + " modified");
    			break;
    		default:
    			Log.v("teledroid.ignore", "Ignored event " + Notify.maskToEvent(event) +" for file " + filename);
    		}
    	}
    }
    
    /*
    public void appendChange(Object pathname, String filename){
    	File file = new File(pathname.toString() + "/" + filename);
    	if(file.isFile())
    		mFileChanges.put(file.getAbsolutePath(), new ModificationInfo(
				file.lastModified()));
    }
    
    public void appendChange(Object filename){
    	File file = new File(filename.toString());
    	if(file.isFile())
    		mFileChanges.put(filename.toString(), new ModificationInfo(
				file.lastModified()));
    }
    */
    public Map<String,ModificationInfo> getLatestChanges() {
    	Log.d("teledroid", "Local changes: "+mFileChanges.size());
    	Map<String,ModificationInfo> result = mFileChanges;
    	mFileChanges = new LinkedHashMap<String,ModificationInfo>();
    	return result;
    }
}

class Notify {
	public final static int IN_ACCESS = 0x00000001;	/* File was accessed.  */
    public final static int IN_MODIFY = 0x00000002;	/* File was modified.  */
    public final static int IN_ATTRIB = 0x00000004;	/* Metadata changed.  */
    public final static int IN_CLOSE_WRITE = 0x00000008;	/* Writtable file was closed.  */
    public final static int IN_CLOSE_NOWRITE = 0x00000010;	/* Unwrittable file closed.  */
    
    public final static int IN_OPEN = 0x00000020;	/* File was opened.  */
    public final static int IN_MOVED_FROM = 0x00000040;	/* File was moved from X.  */
    public final static int IN_MOVED_TO = 0x00000080;	/* File was moved to Y.  */

    public final static int IN_CREATE = 0x00000100;	/* Subfile was created.  */
    public final static int IN_DELETE = 0x00000200;	/* Subfile was deleted.  */
    public final static int IN_DELETE_SELF = 0x00000400;	/* Self was deleted.  */
    public final static int IN_MOVE_SELF = 0x00000800;	/* Self was moved.  */

    /* Events sent by the kernel.  */
    public final static int IN_UNMOUNT = 0x00002000;	/* Backing fs was unmounted.  */
    public final static int IN_Q_OVERFLOW = 0x00004000;	/* Event queued overflowed.  */
    public final static int IN_IGNORED = 0x00008000;	/* File was ignored.  */

    public final static int IN_CLOSE = (IN_CLOSE_WRITE | IN_CLOSE_NOWRITE);	/* Close.  */
    public final static int IN_MOVE = (IN_MOVED_FROM | IN_MOVED_TO);		/* Moves.  */

    /* Special flags.  */
    public final static int IN_ONLYDIR = 0x01000000;	/* Only watch the path if it is a
                                                           directory.  */
    public final static int IN_DONT_FOLLOW = 0x02000000;	/* Do not follow a sym link.  */
    public final static int IN_MASK_ADD = 0x20000000;	/* Add to the mask of an already
                                                           existing watch.  */
    public final static int IN_ISDIR = 0x40000000;	/* Event occurred against dir.  */
    public final static int IN_ONESHOT = 0x80000000;	/* Only send event once.  */

    /* All events which a program can wait on.  */
    public final static int IN_ALL_EVENTS =	 (IN_ACCESS | IN_MODIFY | IN_ATTRIB | IN_CLOSE_WRITE 
                                              | IN_CLOSE_NOWRITE | IN_OPEN | IN_MOVED_FROM 
                                              | IN_MOVED_TO | IN_CREATE | IN_DELETE 
                                              | IN_DELETE_SELF | IN_MOVE_SELF);
    
    public final static int NOTIFY_DELETE = (IN_DELETE_SELF | IN_MOVE_SELF);
    public final static int NOTIFY_MONITOR =  ( IN_MODIFY | IN_CLOSE_WRITE | IN_MOVED_FROM
    											| IN_MOVED_TO | IN_CREATE | IN_DELETE 
    											| IN_DELETE_SELF | IN_MOVE_SELF);
    
    static {
    	// The runtime will add "lib" on the front and ".o" on the end of
    	// the name supplied to loadLibrary.
    	//
    	try{
    		Log.d("teledroid", "Loading JNI Lib.");
    		System.loadLibrary("notify");
    		registerNativeMethod();
    	}catch (Throwable tr){
    		Log.e("teledroid", "Can't load JNI", tr);
    	}
    }
    
    public static String maskToEvent(int mask){
    	String event;
    	switch(mask){
    	case IN_ACCESS:
    		event = "accessed";
    		break;
    	case IN_MODIFY:
    		event = "modify";
    		break;
    	case IN_ATTRIB:
    		event = "meta data change";
    		break;
    	case IN_CLOSE_WRITE:
    		event = "write close";
    		break;
    	case IN_CLOSE_NOWRITE:
    		event = "nowrite close";
    		break;
    	case IN_OPEN:
    		event = "open";
    		break;
    	case IN_MOVED_FROM:
    		event = "move from";
    		break;
    	case IN_MOVED_TO:
    		event = "move to";
    		break;
    	case IN_CREATE:
    		event = "create";
    		break;
    	case IN_DELETE:
    		event = "delete";
    		break;
    	case IN_DELETE_SELF:
    		event = "delete self";
	    	break;
	    case IN_MOVE_SELF:
    		event = "move self";
    		break;
    	 default:
    		event = "unknown";
    	}
    	return event;
    }
    
    public static native void registerNativeMethod();
    public static native int initNotify();
    public static native int registerFile(int nfd, String file, int mask);
    public static native int unregisterFile(int nfd, int wd);
    public static native int nextEvent();
    public static native int eventMask();
    public static native String newFile();
    public static native boolean hasNext(int nfd);
}

