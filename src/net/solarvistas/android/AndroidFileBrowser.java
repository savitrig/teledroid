package net.solarvistas.android;

/*------------------------------
 * 
 * AndroidFileBrowser Codes are from anddev.com.
 * 
-------------------------------*/

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.anddev.filebrowser.IconifiedText;
import com.anddev.filebrowser.IconifiedTextListAdapter;
import com.jcraft.jsch.JSchException;

public class AndroidFileBrowser extends ListActivity {

    private enum DISPLAYMODE {
        ABSOLUTE, RELATIVE;
    }

    private final DISPLAYMODE displayMode = DISPLAYMODE.RELATIVE;
    private List<IconifiedText> directoryEntries = new ArrayList<IconifiedText>();
    public final static File rootDirectory = new File("/sdcard/");
    private File currentDirectory = rootDirectory;
    
    private Menu mMenu;
    private static final int START_SERVER_ID = Menu.FIRST;
    private static final int STOP_SERVER_ID = Menu.FIRST+1;
    private static final int MODE_SCAN_ID = Menu.FIRST+2;
    private static final int MODE_MONITOR_ID = Menu.FIRST+3;
    private static final int MODE_LAZY_ID = Menu.FIRST+4;
    //private static final int EXECUTE_ID = Menu.FIRST+5;

    private String type;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        browseToRoot();
        
        //BackgroundService.ssh = new Connection("cloud", "teledroid", "to.zxi.cc", 22);
        BackgroundService.ssh = new Connection("teledroid", "Lmssf6R6", "teledroid.rictic.com", 22);
        try {
			BackgroundService.ssh.connect();
			Log.v("teledroid.AndroidFileBrowser", "connected successfully");
		} catch (JSchException e) {
			Log.e("teledroid.AndroidFileBrowser", "unable to connect");
			e.printStackTrace();
		}
    }

    @Override
    public void onStop() {
        /*stopService(new Intent(AndroidFileBrowser.this,
                    BackgroundService.class));*/
        super.onStop();
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	boolean s = super.onCreateOptionsMenu(menu);
    	mMenu = menu;
    	menu.add(0, START_SERVER_ID, 0, "Start Service");
        menu.add(0, STOP_SERVER_ID, 0, "Stop Service");
        
    	//MenuItem item = menu.add("Execute command");
    	//item.setIcon(R.drawable.test);
    	
    	menu.add(1, MODE_SCAN_ID, 0, "Scan Mode").setIcon(R.drawable.blank);
        menu.add(1, MODE_MONITOR_ID, 0, "Monitor Mode").setIcon(R.drawable.blank);;
        menu.add(1, MODE_LAZY_ID, 0, "Lazy Mode").setIcon(R.drawable.blank);
        for(int id = 0 ; id < 3; id++){
        	if( BackgroundService.mSyncMode == id + 1) {
        		menu.getItem(MODE_SCAN_ID + id - 1).setIcon(R.drawable.sync);
        		break;
        	}
        }
        
    	return s;
	}
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	switch (item.getItemId()) {
        case START_SERVER_ID:
        	Log.i("teledroid","starting synchronization service");
        	startService(new Intent(AndroidFileBrowser.this, BackgroundService.class));
            break;
	    case STOP_SERVER_ID:
	    	Log.i("teledroid","stopping synchronization service");
	    	stopService(new Intent(AndroidFileBrowser.this, BackgroundService.class));
	        break;
	    case MODE_SCAN_ID:
	    	Log.i("teledroid","switching to Scan Mode");
	    	BackgroundService.mSyncMode = BackgroundService.SYNC_MODE_SCAN;
	    	for(int id = 0 ; id < 3; id++)
	        	mMenu.getItem(MODE_SCAN_ID + id - 1).setIcon(R.drawable.blank);
	        item.setIcon(R.drawable.sync);
	    	break;
	    case MODE_MONITOR_ID:
	    	Log.i("teledroid","switching to Monitor Mode");
	    	BackgroundService.mSyncMode = BackgroundService.SYNC_MODE_MONITOR;
	    	for(int id = 0 ; id < 3; id++)
	    		mMenu.getItem(MODE_SCAN_ID + id - 1).setIcon(R.drawable.blank);
	    	item.setIcon(R.drawable.sync);
	    	break;
	    case MODE_LAZY_ID:
	    	Log.i("teledroid","switching to Lazy Mode");
	    	BackgroundService.mSyncMode = BackgroundService.SYNC_MODE_LAZY;
	    	for(int id = 0 ; id < 3; id++)
	        	mMenu.getItem(MODE_SCAN_ID + id - 1).setIcon(R.drawable.blank);
	        item.setIcon(R.drawable.sync);
	    	break;
        }
    	return true;
    }
	/**
     * This function browses to the
     * root-directory of the file-system.
     */
    private void browseToRoot() {
        browseTo(rootDirectory);
    }

    /**
     * This function browses up one level
     * according to the field: currentDirectory
     */
    private void upOneLevel() {
        if (this.currentDirectory.getParent() != null)
            this.browseTo(this.currentDirectory.getParentFile());
    }

    private void browseTo(final File aDirectory) {
        // On relative we display the full path in the title.
        if (this.displayMode == DISPLAYMODE.RELATIVE)
            this.setTitle(aDirectory.getAbsolutePath() + " :: " +
                    getString(R.string.app_name));
        if (aDirectory.isDirectory()) {
            this.currentDirectory = aDirectory;
            fill(aDirectory.listFiles());
        } else {
            OnClickListener okButtonListener = new OnClickListener() {
                // @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // Lets start an intent to View the file, that was clicked...
                    AndroidFileBrowser.this.openFile(aDirectory);
                }
            };
            OnClickListener cancelButtonListener = new OnClickListener() {
                // @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // Do nothing ^^
                }
            };
            new AlertDialog.Builder(this)
                    .setTitle("Question")
                    .setMessage("Do you want to open that file?\n" + aDirectory.getName())
                    .setPositiveButton("OK", okButtonListener)
                    .setNegativeButton("Cancel", cancelButtonListener)
                    .show();
        }
    }

    private void openFile(File aFile) {
        String fileName = aFile.getName();
        if (checkEndsWithInStringArray(fileName, getResources().
                getStringArray(R.array.fileEndingImage))) {
            type = "image/*";
        } else if (checkEndsWithInStringArray(fileName, getResources().
                getStringArray(R.array.fileEndingWebText))) {
            //type = getResources().getDrawable(R.drawable.webtext);
        } else if (checkEndsWithInStringArray(fileName, getResources().
                getStringArray(R.array.fileEndingPackage))) {
            //type = getResources().getDrawable(R.drawable.packed);
        } else if (checkEndsWithInStringArray(fileName, getResources().
                getStringArray(R.array.fileEndingAudio))) {
            type = "audio/*";
        } else {
            type = "text/*";
        }
        
    	Intent intent = new Intent();  
    	intent.setAction(android.content.Intent.ACTION_VIEW);  
    	intent.setDataAndType(Uri.fromFile(aFile), type);  
    	startActivity(intent); 
        
        /*Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.fromFile(aFile));
        startActivity(myIntent);*/
    }

    private void fill(File[] files) {
        this.directoryEntries.clear();

        // Add the "." == "current directory"
        this.directoryEntries.add(new IconifiedText(
                getString(R.string.current_dir),
                getResources().getDrawable(R.drawable.folder)));
        // and the ".." == 'Up one level'
        if (this.currentDirectory.getParent() != null)
            this.directoryEntries.add(new IconifiedText(
                    getString(R.string.up_one_level),
                    getResources().getDrawable(R.drawable.uponelevel)));

        Drawable currentIcon = null;
        for (File currentFile : files) {
            if (currentFile.isDirectory()) {
                currentIcon = getResources().getDrawable(R.drawable.folder);
            } else {
                String fileName = currentFile.getName();
                /* Determine the Icon to be used,
              * depending on the FileEndings defined in:
              * res/values/fileendings.xml. */
                if (checkEndsWithInStringArray(fileName, getResources().
                        getStringArray(R.array.fileEndingImage))) {
                    currentIcon = getResources().getDrawable(R.drawable.image);
                } else if (checkEndsWithInStringArray(fileName, getResources().
                        getStringArray(R.array.fileEndingWebText))) {
                    currentIcon = getResources().getDrawable(R.drawable.webtext);
                } else if (checkEndsWithInStringArray(fileName, getResources().
                        getStringArray(R.array.fileEndingPackage))) {
                    currentIcon = getResources().getDrawable(R.drawable.packed);
                } else if (checkEndsWithInStringArray(fileName, getResources().
                        getStringArray(R.array.fileEndingAudio))) {
                    currentIcon = getResources().getDrawable(R.drawable.audio);
                } else {
                    currentIcon = getResources().getDrawable(R.drawable.text);
                }
            }
            switch (this.displayMode) {
                case ABSOLUTE:
                    /* On absolute Mode, we show the full path */
                    this.directoryEntries.add(new IconifiedText(currentFile
                            .getPath(), currentIcon));
                    break;
                case RELATIVE:
                    /* On relative Mode, we have to cut the
               * current-path at the beginning */
                    int currentPathStringLenght = this.currentDirectory.
                            getAbsolutePath().length();
                    this.directoryEntries.add(new IconifiedText(
                            currentFile.getAbsolutePath().
                                    substring(currentPathStringLenght),
                            currentIcon));

                    break;
            }
        }
        Collections.sort(this.directoryEntries);

        IconifiedTextListAdapter itla = new IconifiedTextListAdapter(this);
        itla.setListItems(this.directoryEntries);
        this.setListAdapter(itla);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        String selectedFileString = this.directoryEntries.get(position)
                .getText();
        if (selectedFileString.equals(getString(R.string.current_dir))) {
            // Refresh
            this.browseTo(this.currentDirectory);
        } else if (selectedFileString.equals(getString(R.string.up_one_level))) {
            this.upOneLevel();
        } else {
            File clickedFile = null;
            switch (this.displayMode) {
                case RELATIVE:
                    clickedFile = new File(this.currentDirectory
                            .getAbsolutePath()
                            + this.directoryEntries.get(position)
                            .getText());
                    break;
                case ABSOLUTE:
                    clickedFile = new File(this.directoryEntries.get(
                            position).getText());
                    break;
            }
            if (clickedFile != null)
                this.browseTo(clickedFile);
        }
    }

    /**
     * Checks whether checkItsEnd ends with
     * one of the Strings from fileEndings
     */
    private boolean checkEndsWithInStringArray(String checkItsEnd,
                                               String[] fileEndings) {
        for (String aEnd : fileEndings) {
            if (checkItsEnd.endsWith(aEnd))
                return true;
        }
        return false;
    }
}