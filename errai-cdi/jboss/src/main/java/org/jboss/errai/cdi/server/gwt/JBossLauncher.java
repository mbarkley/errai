package org.jboss.errai.cdi.server.gwt;

import java.io.File;
import java.io.IOException;
import java.net.BindException;

import org.jboss.errai.cdi.server.as.JBossServletContainerAdaptor;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.ServletContainerLauncher;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

public class JBossLauncher extends ServletContainerLauncher {
  
  // TODO remove hard-coding
  private final String JBOSS_HOME = "/home/yyz/mbarkley/Documents/errai-projects/errai/errai-cdi/jboss/src/main/resources/jboss-as-7.1.1.Final";
  // TODO make portable
  private final String JBOSS_START = JBOSS_HOME + "/bin/standalone.sh";

  @Override
  public ServletContainer start(TreeLogger logger, int port, File appRootDir) throws BindException, Exception {
    // Start JBoss AS
    Process jbossProcess;
    try {
      ProcessBuilder builder = new ProcessBuilder(JBOSS_START);
      // Add JBOSS_HOME to environment
      builder.environment().put("JBOSS_HOME", JBOSS_HOME);
      jbossProcess = builder.start();
    } catch (IOException e) {
      logger.log(TreeLogger.Type.ERROR, "Failed to start JBoss AS process", e);
      throw new UnableToCompleteException();
    }

    // TODO figure how to identify when AS is up
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e1) {
      // Don't care too much if this is interrupted...
    }
    
    return new JBossServletContainerAdaptor(port, appRootDir, logger, jbossProcess);
  }

}
