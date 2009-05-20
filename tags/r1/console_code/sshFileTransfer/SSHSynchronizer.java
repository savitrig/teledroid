import com.jcraft.jsch.JSchException;


public class SSHSynchronizer {
  private ConnectionInfo info;
  private Connection con;
  
  public SSHSynchronizer(ConnectionInfo info) throws JSchException {
    this.info = info;
    this.con = new Connection(this.info);
    con.connect();
  }
  
  public void syncUp(Iterable<FileMatcher> localFiles) throws JSchException {
    
  }
  
  public void syncDown(Iterable<FileMatcher> remoteFiles) throws JSchException{
    
  }
  
  public void sync(Iterable<FileMatcher> files) throws JSchException{
    
  }
}
