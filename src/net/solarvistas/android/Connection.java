/**
 * net.solarvistas.android :: Filename
 * <p/>
 * Created by Xi Zhang (zhangxi)
 * using IntelliJ IDEA 8.1.
 * at 3:13:26 PM, Apr 16, 2009
 */
package net.solarvistas.android;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class Connection {
    String user, pass, host;
    int port;
    Session session;
    String status = "ok";
	Channel channel;
    BufferedReader fromServer;
    OutputStream toServer; 
    
    public Connection(String user, String pass, String host, int port) {
        this.user = user;
        this.pass = pass;
        this.host = host;
        this.port = port;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, port);
            session.setPassword(pass);

            //TODO: host key check
            jsch.setKnownHosts("/data/data/net.solarvistas.android/files/.ssh/known_hosts");
            session.setConfig("StrictHostKeyChecking", "no");

            session.connect();   // making a connection with timeout.

        } catch(Exception e){
        	status = e.toString();
        }
    }

    @Override
    protected void finalize() throws Throwable {
    	super.finalize();
    	//session.disconnect();
    }

    public void channelSetup() {
        try {
			channel = session.openChannel("shell");
			fromServer = new BufferedReader(new InputStreamReader(channel.getInputStream()));  
	        toServer = channel.getOutputStream();
            channel.connect(3*1000);
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }	
    	
    public void Exec(String command) {
    	/*if(!status.equals("ok"))
    		return status;*/        
        try{            
        	toServer.write(command.getBytes());
        	toServer.flush();
        } catch (Exception e) {
			e.printStackTrace();
        }
    }
}