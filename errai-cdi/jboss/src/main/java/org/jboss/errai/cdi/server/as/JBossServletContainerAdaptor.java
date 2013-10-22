package org.jboss.errai.cdi.server.as;

import java.io.File;

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

  private final Process jbossProcess;
  private final CommandContext ctx;

  private final int port;
  private final TreeLogger logger;
  private final File appRootDir;

  public JBossServletContainerAdaptor(int port, File appRootDir, TreeLogger logger, Process jbossProcess) throws UnableToCompleteException {
    this.port = port;
    this.appRootDir = appRootDir;
    this.logger = logger;
    this.jbossProcess = jbossProcess;

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

    // Deploy web app
    try {
      // Need to add deployment resource to specify exploded archive
      ctx.handle(String.format(
              "/deployment='%s':add(runtime-name='%s',content=[{'path'=>'%s','archive'=>false}], enabled=false)",
              appRootDir.getName(), appRootDir.getAbsolutePath()));
      // Deploy the resource
      ctx.handle(String.format("/deployment='%s':deploy", appRootDir.getName()));
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
