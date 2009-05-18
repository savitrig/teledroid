/**
 * net.solarvistas.android :: Filename
 * <p/>
 * Created by Xi Zhang (zhangxi)
 * using IntelliJ IDEA 8.1.
 * at 3:13:26 PM, Apr 16, 2009
 */
package net.solarvistas.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

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
	}

	public void connect() throws JSchException {
		JSch jsch = new JSch();
		session = jsch.getSession(user, host, port);
		session.setPassword(pass);

		// TODO: host key check
		// jsch.setKnownHosts("/data/data/net.solarvistas.android/files/.ssh/known_hosts");
		session.setConfig("StrictHostKeyChecking", "no");

		session.connect(); // making a connection with timeout.

	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		// session.disconnect();
	}

	public Channel newShell() throws JSchException, IOException {
		Channel channel = session.openChannel("shell");
		channel.connect(3 * 1000);
		return channel;
	}

	
	public Channel Exec(String command) throws JSchException, IOException {
		Channel channel = newShell();
		channel.getOutputStream().write(command.getBytes());
		channel.getOutputStream().flush();
		return channel;
	}


	static int checkAck(InputStream in) throws IOException {
		int b=in.read();
	    // b may be 0 for success,
	    //          1 for error,
	    //          2 for fatal error,
	    //          -1
	    if(b==0) return b;
	    if(b==-1) return b;

	    if(b==1 || b==2){
	      StringBuffer sb=new StringBuffer();
	      int c;
	      do {
		c=in.read();
		sb.append((char)c);
	      }
	      while(c!='\n');
	      if(b==1){ // error
		System.out.print(sb.toString());
	      }
	      if(b==2){ // fatal error
		System.out.print(sb.toString());
	      }
	    }
	    return b;
	}

	public boolean SCPFrom(String rfile, String lfile) {
		FileOutputStream fos = null;
		try {
			String prefix = null;
			if (new File(lfile).isDirectory()) {
				prefix = lfile + File.separator;
			}

			// exec 'scp -f rfile' remotely
			String command = "scp -f " + rfile;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			byte[] buf = new byte[1024];

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			while (true) {
				int c = checkAck(in);
				if (c != 'C') {
					break;
				}

				// read '0644 '
				in.read(buf, 0, 5);

				long filesize = 0L;
				while (true) {
					if (in.read(buf, 0, 1) < 0) {
						break;// error
					}
					if (buf[0] == ' ')
						break;
					filesize = filesize * 10L + (long) (buf[0] - '0');
				}

				String file = null;
				for (int i = 0;; i++) {
					in.read(buf, i, 1);
					if (buf[i] == (byte) 0x0a) {
						file = new String(buf, 0, i);
						break;
					}
				}

				// System.out.println("filesize="+filesize+", file="+file);

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();

				// read a content of lfile
				fos = new FileOutputStream(prefix == null ? lfile : prefix
						+ file);
				int foo;
				while (true) {
					if (buf.length < filesize)
						foo = buf.length;
					else
						foo = (int) filesize;
					foo = in.read(buf, 0, foo);
					if (foo < 0) {
						// error
						break;
					}
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0L)
						break;
				}
				fos.close();
				fos = null;

				if (checkAck(in) != 0) {
					return false;
				}

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();

			}
		} catch (Exception e) {
			System.out.println(e);
			try {
				if (fos != null)
					fos.close();
			} catch (Exception ee) {
			}
		}
		Log.d("teledroid", "SCPFrom " + rfile + " : " + lfile);
		return true;
	}

	public boolean SCPTo(String lfile, String rfile) {
		FileInputStream fis;
		try {
			String command = "scp -p -t " + rfile;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();
			channel.connect();

			if (checkAck(in) != 0) {
				return false;
			}

			// send "C0644 filesize filename",
			// where filename should not include '/'
			long filesize = (new File(lfile)).length();
			command = "C0644 " + filesize + " ";
			if (lfile.lastIndexOf('/') > 0) {
				command += lfile.substring(lfile.lastIndexOf('/') + 1);
			} else {
				command += lfile;
			}
			command += "\n";
			out.write(command.getBytes());
			out.flush();
			if (checkAck(in) != 0) {
				return false;
			}

			// send content of lfile
			fis = new FileInputStream(lfile);
			byte[] buf = new byte[1024];
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len); // out.flush();
			}
			fis.close();
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			if (checkAck(in) != 0) {
				return false;
			}
			out.close();
		} catch (JSchException e) {
			Log.d("teledroid", "JSchException", e);
		} catch (IOException e) {
			Log.d("teledroid", "JSchException", e);
		}
		Log.d("teledroid", "SCPTo " + lfile + " : " + rfile);
		return true;
	}
}