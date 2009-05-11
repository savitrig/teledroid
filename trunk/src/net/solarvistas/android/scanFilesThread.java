package net.solarvistas.android;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import android.util.Log;

public class scanFilesThread implements Runnable {
	Map<String, Long> mFilesMap;
	File rootDir = new File("/sdcard/");
    public scanFilesThread(BackgroundService bs) {
    	this.mFilesMap = bs.mFilesMap; 
    }
    
	public void run() {
		getFilesModifiedTime(rootDir); 
		Set s = mFilesMap.entrySet();
		
		for (Iterator i = s.iterator(); i.hasNext();) {
			Entry<String, Long> e = (Entry<String, Long>)i.next();
			Log.d(e.getKey(), new Long(e.getValue()).toString());
		}
	}
	
	private void getFilesModifiedTime(File dir) {
		if (dir.isDirectory()) {
			for (File currentFile : dir.listFiles()) {
				if (currentFile.isFile())
					mFilesMap.put(currentFile.getAbsolutePath(), currentFile.lastModified());
				else
					getFilesModifiedTime(currentFile.getAbsoluteFile());
			}
		}
		else
			System.err.println(dir.getAbsolutePath()+" is not a directory.");
	}
}
