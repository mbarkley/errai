package org.jboss.errai.cdi.server.gwt.util;

import java.io.File;
import java.util.Arrays;

import org.jboss.errai.cdi.server.gwt.JBossLauncher;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.ServletContainerLauncher;

public class CLI {

  /**
   * Start a JBoss AS Embedded instance.
   * 
   * @param Arguments
   *          should be of form: {port_number} {war_directory}
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      throw new IllegalArgumentException("Expected args = {<port_number>, <war_directory>}. Found args = "
              + Arrays.toString(args));
    }

    int port = covertPortNum(args[0]);
    File appRootDir = getFileFromName(args[1]);

    ServletContainerLauncher launcher = new JBossLauncher();
    ServletContainer container = launcher.start(null, port, appRootDir);
  }

  private static File getFileFromName(String path) {
    return new File(path);
  }

  private static int covertPortNum(String portString) {
    int port;
    try {
      port = Integer.valueOf(portString);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    
    return port;
  }

}
