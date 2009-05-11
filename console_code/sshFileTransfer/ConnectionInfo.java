import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.jcraft.jsch.UserInfo;


public class ConnectionInfo {
  public String username;
  public String host;
  public UserInfo ui;
  public int port;
  public static final int sshPort = 22;
  
  //useful for debugging, but the UI for fetching a password works
  // over the console
  public ConnectionInfo(String username, String host) {
    init(username, host, new ConsolePrompt(), sshPort);
  }
  
  //Our android service will have to implement a UserInfo class for getting
  // the password
  public ConnectionInfo(String username, String host, UserInfo ui){
    init(username, host, ui, sshPort);
  }
  
  
  private void init(String username, String host, UserInfo ui, int port) {
    this.username = username;this.host = host;this.ui = ui;this.port = port;
  }
  
  
  private static class ConsolePrompt implements UserInfo {
    public String password;
    public String passphrase;
    public static final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    
    public String getPassphrase() {
      return passphrase;
    }

    public String getPassword() {
      return password;
    }

    public boolean promptPassphrase(String message) {
      passphrase = prompt(message);
      return passphrase != null;
    }

    public boolean promptPassword(String message) {
      password = prompt(message);
      return password != null;
    }

    private String prompt(String message) {
      showMessage(message);
      try {
        return in.readLine().trim();
      } catch (IOException e) {
        return null;
      }
    }

    public boolean promptYesNo(String message) {
      showMessage(message + " (Y/n):");     
      while (true){
        String response = null;
        try {
          response = in.readLine().trim().toLowerCase();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        if (response.equals("") || response.startsWith("y")) return true;
        if (response.startsWith("n")) return false;
      }
    }

    public void showMessage(String message) {
      System.out.println(message);
    }
  }
}
