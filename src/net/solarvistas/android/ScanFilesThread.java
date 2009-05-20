package net.solarvistas.android;

import java.io.File;
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
	Map<String, Object> mFilesMap;
	File rootDir = new File("/sdcard/");
    public static Thread synThread = null; //bcast thread
    private JSONObject mServerJson;
    private JSONObject mLocalJson;
    private static final int PERIOD = 5000;
    private static final int TIMEDIFF = 1000;
    public static boolean stopSignal = false;
    
    /*Flag 0: Server to Local; Flag 1: Local to Server; */
    public enum Flag { ServerToClient, ClientToServer }
    
    //Connection mShell = null;

    public ScanFilesThread(BackgroundService bs) {
    	this.mFilesMap = bs.mFilesMap; 
    }
    
	public void run() {
		while (!stopSignal) {
			getFilesModifiedTime(rootDir, mFilesMap);
			mLocalJson = new JSONObject(mFilesMap);
			Log.d("Files Map", mFilesMap.toString());
	
			/*
			 * Set s = mFilesMap.entrySet();
			 * for (Iterator i = s.iterator(); i.hasNext();) {
				Entry<String, Long> e = (Entry<String, Long>)i.next();
				Log.d(e.getKey(), new Long(e.getValue()).toString());
			}*/
			
			synThread = new Thread(new SynThread(this));
			synThread.start();
			
			try {
				Thread.sleep(PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void getFilesModifiedTime(File dir, Map<String, Object> m) {
		if (dir.isDirectory()) {
			for (File currentFile : dir.listFiles()) {
				if (currentFile.isFile())
					m.put(currentFile.getAbsolutePath().substring(1), currentFile.lastModified());
				else {
					Map<String, Object> dirMap = new LinkedHashMap<String, Object>();
					getFilesModifiedTime(currentFile.getAbsoluteFile(), dirMap);
					m.put(currentFile.getAbsolutePath().substring(1), dirMap);
				}	
			}
		}
		else
			System.err.println(dir.getAbsolutePath().substring(1)+" is not a directory.");
	}
	
    public static final int BUMP_MSG = 0x101;

    public Handler getServerInfoHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case BUMP_MSG:
                	try {
                		mServerJson = new JSONObject((String)msg.obj);
					} catch (JSONException e) {
						e.printStackTrace();
					}
					for (Iterator i = mServerJson.keys(); i.hasNext();) {
						Log.d("Key", (String)i.next());
					}
					autoSyn(mServerJson, mLocalJson, Flag.ServerToClient);
					autoSyn(mLocalJson, mServerJson, Flag.ClientToServer);
                	//Log.d("Server", (String)msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };
    
    private void autoSyn(JSONObject o1, JSONObject o2, Flag flag) {    	
		for (Iterator i = o1.keys(); i.hasNext();) {
				
			String key = (String)i.next();
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
				// else for one side Long, one side JSONObject;
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
    	    Date date;
			String formatDate = format.format((new Date(value)));
	    	Log.d("TestTime", formatDate);

    		try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		Connection mShell = BackgroundService.ssh;
    		//mShell.channelSetup();
            Channel c = mShell.Exec("touch -t "+formatDate+" "+parentPath+fileName+"\n");
            c.disconnect();
    		break;
    	default:break;
    	}
    }
    
    /*private void getFileFromServer(String localFileName) {
        FileOutputStream fos; 
        DataOutputStream dos;
        
    	try {
    		File f = new File(localFileName);
    		fos = new FileOutputStream(f);
    		dos=new DataOutputStream(fos);

    		//while (true) {
          	  try {
				  String msg;// = mShell.fromServer.readLine();
				  do {
					  msg = mShell.fromServer.readLine();
					  Log.d("MSG!!", msg);
					  dos.write(msg.getBytes());
				  } while (mShell.fromServer.ready());
				  //ServerSocketTest.text.setText((CharSequence) mMsg);  
              } catch(IOException ioException){
      			ioException.printStackTrace();
              } finally {
              }
            //}
        } catch (Exception e) {
			e.printStackTrace();
        }
    }*/
}
