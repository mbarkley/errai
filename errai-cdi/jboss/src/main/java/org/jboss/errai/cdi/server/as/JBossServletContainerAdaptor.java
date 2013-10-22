package org.jboss.errai.cdi.server.as;

import java.io.File;
import java.io.IOException;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * Acts as a an adaptor between gwt's ServletContainer interface and a JBoss AS 7 instance.
 * 
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class JBossServletContainerAdaptor extends ServletContainer {

  // TODO remove hard-coding
  private final String JBOSS_HOME =
          "/home/yyz/mbarkley/Documents/errai-projects/errai/errai-cdi/jboss/src/main/resources/jboss-as-7.1.1.Final";
  // TODO make portable
  private final String JBOSS_START = JBOSS_HOME + "/bin/standalone.sh";
  private final int MANAGEMENT_PORT = 9999;
  private final String JBOSS_BIND_NAME = "localhost";

  private Process jbossProcess;
  private CommandContext ctx;

  private int port;
  private TreeLogger logger;
  private File appRootDir;

  public JBossServletContainerAdaptor(int port, File appRootDir, TreeLogger logger) throws UnableToCompleteException {
    this.port = port;
    this.appRootDir = appRootDir;
    this.logger = logger;

    // Start JBoss AS
    try {
      jbossProcess = Runtime.getRuntime().exec(JBOSS_START);
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

    // Start CLI Context
    try {
      ctx = CommandContextFactory.getInstance().newCommandContext();
    } catch (CliInitializationException e) {
      logger.log(TreeLogger.Type.ERROR, "Could not initialize JBoss AS CLI", e);
      throw new UnableToCompleteException();
    }

    // Connect to AS instance
    try {
      ctx.handle("connect");
    } catch (CommandLineException e) {
      logger.log(Type.ERROR, "Could not connect to AS", e);
    }

    try {
      ctx.handle("deploy " + appRootDir.getAbsolutePath());
    } catch (CommandLineException e) {
      logger.log(Type.ERROR, "Could not deploy " + appRootDir.getAbsolutePath(), e);
      throw new UnableToCompleteException();
    }

  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public void refresh() throws UnableToCompleteException {
    // Deploying again should override any previous deployment of the directory
    try {
      ctx.handle("deploy " + appRootDir.getAbsolutePath());
    } catch (CommandLineException e) {
      logger.log(Type.ERROR, "Failed to redeploy app at " + appRootDir.getAbsolutePath(), e);
      throw new UnableToCompleteException();
    }
  }

  @Override
  public void stop() throws UnableToCompleteException {
    TreeLogger branch = null;
    try {
      ctx.handle(":shutdown");
    } catch (CommandLineException e) {
      branch = logger.branch(Type.ERROR, "Could not shutdown AS", e);
    } finally {
      ctx.terminateSession();
      if (branch != null) {
        // If an error occurred shutting down AS, attempt to kill process
        jbossProcess.destroy();
        throw new UnableToCompleteException();
      }
    }
  }
}
