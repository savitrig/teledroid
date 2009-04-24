import com.jcraft.jsch.*;


class Connection {
  public static void main (String[] args) throws JSchException {
    if (args.length != 2) {
      System.out.println("usage: Connection <host> <username>");
      System.exit(1);
    }
    Connection con = new Connection(new ConnectionInfo(args[0],args[1]));
    con.connect();
    con.sshShell();
  }
  
  private ConnectionInfo info;
  private Session session;
  public Connection(ConnectionInfo info){
    this.info = info;
  }
  
  public void connect() throws JSchException {
    JSch jsch=new JSch();
    session = jsch.getSession(info.username, info.host);
    session.setUserInfo(info.ui);
    
//    session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");
//    session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
//    session.setConfig("compression_level", "9");
    session.setConfig("StrictHostKeyChecking", "no");

    session.connect();
  }
  
  public void sshShell() throws JSchException {
    Channel channel=session.openChannel("shell");

    channel.setInputStream(System.in);
    channel.setOutputStream(System.out);

    channel.connect();
  }
    
  
}