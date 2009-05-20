import java.util.regex.Pattern;



public class FileMatcher {
  
  /* The FileMatcher contains the info needed to match a set of files and set up a
   * mapping between their local and remote versions.  Examples:
   * 
   * This matcher maps all java source from /usr/src locally to /opt/local/src including subdirectories  
   * FileMatcher("src",Pattern.compile(".*\\.java"), "/usr/", "/opt/local/", true);
   * */
  public FileMatcher(String relativeDirName, Pattern filePattern, String localRoot, String remoteRoot, boolean recurse) {
  }
}
