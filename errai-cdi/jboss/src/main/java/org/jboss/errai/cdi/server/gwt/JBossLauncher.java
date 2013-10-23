package org.jboss.errai.cdi.server.gwt;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.util.Stack;

import org.jboss.errai.cdi.server.as.JBossServletContainerAdaptor;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.ServletContainerLauncher;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;

public class JBossLauncher extends ServletContainerLauncher {

  private final String JBOSS_HOME;
  // TODO make portable
  private final String JBOSS_START;
  
  public JBossLauncher() {
    JBOSS_HOME = Thread.currentThread().getContextClassLoader().getResource("jboss-as-7.1.1.Final").getPath();
    JBOSS_START = JBOSS_HOME + "/bin/standalone-debug.sh";
  }

  @Override
  public ServletContainer start(TreeLogger logger, int port, File appRootDir) throws BindException, Exception {
    final Stack<TreeLogger> branches = new Stack<TreeLogger>();
    branches.add(logger);

    branches.add(branches.peek().branch(Type.INFO, "Starting launcher..."));
    try {
      branches.add(branches.peek().branch(Type.INFO, String.format("Preparing JBoss AS instance (%s)", JBOSS_START)));
      ProcessBuilder builder = new ProcessBuilder("bash", JBOSS_START);

      branches.peek().log(Type.INFO, String.format("Adding JBOSS_HOME=%s to instance environment", JBOSS_HOME));
      builder.environment().put("JBOSS_HOME", JBOSS_HOME);

      branches.peek().log(Type.INFO, "Redirecting stdout and stderr to share with this process");
      builder.inheritIO();

      builder.start();
      branches.peek().log(Type.INFO, "Executing AS instance...");
    } catch (IOException e) {
      branches.pop().log(TreeLogger.Type.ERROR, "Failed to start JBoss AS process", e);
      throw new UnableToCompleteException();
    }

    // TODO figure out better way to identify when AS is up
    try {
      branches.peek().log(Type.INFO, "Waiting for AS instance...");
      Thread.sleep(5000);
    } catch (InterruptedException e1) {
      // Don't care too much if this is interrupted... but I guess we'll log it
      branches.peek().log(Type.WARN, "Launcher was interrupted while waiting for JBoss AS to start", e1);
    }
    branches.pop();

    branches.add(branches.peek().branch(Type.INFO, "Creating servlet container controller..."));
    try {
      JBossServletContainerAdaptor controller = new JBossServletContainerAdaptor(port, appRootDir, branches.peek());
      branches.pop().log(Type.INFO, "Controller created");
      return controller;
    } catch (UnableToCompleteException e) {
      branches.peek().log(Type.ERROR, "Could not start servlet container controller", e);
      throw new UnableToCompleteException();
    }
  }

}
