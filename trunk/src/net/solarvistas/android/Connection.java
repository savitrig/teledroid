/**
 * net.solarvistas.android :: Filename
 * <p/>
 * Created by Xi Zhang (zhangxi)
 * using IntelliJ IDEA 8.1.
 * at 3:13:26 PM, Apr 16, 2009
 */
package net.solarvistas.android;

import com.jcraft.jsch.*;

import java.io.InputStream;

public class Connection {
    String user, pass, host;
    int port;
    Session session;
    String status = "ok";

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
    	session.disconnect();
    }

    public String Exec(String command) {
    	if(!status.equals("ok"))
    		return status;
    	Channel channel;
        String result = "";

        try{
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream in = channel.getInputStream();
            channel.connect();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    result += new String(tmp, 0, i);
                }
                if (channel.isClosed()) {
                    if(channel.getExitStatus() != 0)
                    	return "exit-status: " + channel.getExitStatus();
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                    result += ee;
                }
            }
            channel.disconnect();
        } catch (Exception e) {
            result += e;
        }
        return result;
    }
}