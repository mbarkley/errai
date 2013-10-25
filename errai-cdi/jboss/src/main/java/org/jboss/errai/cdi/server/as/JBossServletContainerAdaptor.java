package org.jboss.errai.cdi.server.as;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;
import org.jboss.errai.cdi.server.gwt.util.CopyUtil;
import org.jboss.errai.cdi.server.gwt.util.CopyUtil.Filter;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.HelpInfo;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * Acts as a an adaptor between gwt's ServletContainer interface and a JBoss AS 7 instance.
 * 
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class JBossServletContainerAdaptor extends ServletContainer {

  private final CommandContext ctx;

  private final int port;
  private final Stack<TreeLogger> branches = new Stack<TreeLogger>();
  private final File appRootDir;
  @SuppressWarnings("unused")
  private final Process jbossProcess;

  /**
   * Initialize the command context for a remote JBoss AS instance.
   * 
   * @param port
   *          The port to which the JBoss instance binds. (not yet implemented!)
   * @param appRootDir
   *          The exploded war directory to be deployed.
   * @param logger
   *          For logging events from this container.
   * @throws UnableToCompleteException
   *           Thrown if this container cannot properly connect or deploy.
   */
  public JBossServletContainerAdaptor(int port, File appRootDir, TreeLogger logger, Process jbossProcess) throws UnableToCompleteException {
    this.port = port;
    this.appRootDir = appRootDir;
    branches.add(logger);
    this.jbossProcess = jbossProcess;

    branch(Type.INFO, "Starting container initialization...");

    CommandContext ctx = null;
    try {
      try {
        branch(Type.INFO, "Creating new command context...");

        ctx = CommandContextFactory.getInstance().newCommandContext();
        this.ctx = ctx;

        log(Type.INFO, "Command context created");
        unbranch();
      } catch (CliInitializationException e) {
        branch(TreeLogger.Type.ERROR, "Could not initialize JBoss AS command context", e);
        throw new UnableToCompleteException();
      }

      try {
        branch(Type.INFO, "Connecting to JBoss AS...");

        ctx.handle("connect localhost:9999");

        log(Type.INFO, "Connected to JBoss AS");
        unbranch();
      } catch (CommandLineException e) {
        branch(Type.ERROR, "Could not connect to AS", e);
        throw new UnableToCompleteException();
      }

      try {
        branch(Type.INFO, String.format("Setting AS to listen for http requests on port %d...", port));

        ctx.handle(String.format(
                "/socket-binding-group=standard-sockets/socket-binding=http:write-attribute(name=port,value=%d)", port));
        ctx.handle(":reload");
        // Give the server time to reload
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          log(Type.WARN, "Interrupted while waiting for JBoss AS to reload", e);
        }

        log(Type.INFO, "Port change successful");
        unbranch();
      } catch (CommandLineException e1) {
        branch(Type.ERROR, String.format("Could not change the http port to %d", port), e1);
        throw new UnableToCompleteException();
      }

      try {
        /*
         * Need to add deployment resource to specify exploded archive
         * 
         * path : the absolute path the deployment file/directory archive : true iff the an archived
         * file, false iff an exploded archive enabled : true iff war should be automatically
         * scanned and deployed
         */
        branch(Type.INFO, String.format("Adding deployment %s at %s...", getAppName(), appRootDir.getAbsolutePath()));

        ctx.handle(String.format("/deployment=%s:add(content=[{\"path\"=>\"%s\",\"archive\"=>false}], enabled=false)",
                getAppName(), appRootDir.getAbsolutePath()));

        log(Type.INFO, "Deployment resource added");
        unbranch();
      } catch (CommandLineException e) {
        branch(Type.ERROR, String.format("Could not add deployment %s", getAppName()), e);
        throw new UnableToCompleteException();
      }

      try {
        branch(Type.INFO, String.format("Deploying %s...", getAppName()));

        ctx.handle(String.format("/deployment=%s:deploy", getAppName()));

        log(Type.INFO, String.format("%s deployed", getAppName()));
        unbranch();
      } catch (CommandLineException e) {
        branch(Type.ERROR, String.format("Could not deploy %s", getAppName()), e);
        throw new UnableToCompleteException();
      }

    } catch (UnableToCompleteException e) {
      branch(Type.INFO, "Attempting to stop container...");
      stopHelper();

      throw e;
    }

  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public void refresh() throws UnableToCompleteException {
    try {
      branch(Type.INFO, String.format("Redeploying %s...", getAppName()));

      ctx.handle(String.format("/deployment=%s:redeploy", getAppName()));

      log(Type.INFO, String.format("%s redeployed", getAppName()));
      unbranch();
    } catch (CommandLineException e) {
      log(Type.ERROR, String.format("Failed to redeploy %s", getAppName()), e);
      throw new UnableToCompleteException();
    }
  }

  @Override
  public void stop() throws UnableToCompleteException {
    try {
      branch(Type.INFO, String.format("Removing %s from deployments...", getAppName()));

      ctx.handle(String.format("/deployment=%s:remove", getAppName()));

      log(Type.INFO, String.format("%s removed", getAppName()));
      unbranch();
    } catch (CommandLineException e) {
      log(Type.ERROR, "Could not shutdown AS", e);
      throw new UnableToCompleteException();
    } finally {
      stopHelper();
    }
  }

  private void stopHelper() {
    branch(Type.INFO, "Attempting to stop JBoss AS instance...");
    /*
     * There is a problem with Process#destroy where it will not reliably kill the JBoss instance.
     * So instead we must try and send a shutdown signal. If that is not possible or does not work,
     * we will log it's failure, advising the user to manually kill this process.
     */
    try {
      if (ctx.getControllerHost() == null) {
        ctx.handle("connect localhost:9999");
      }
      ctx.handle(":shutdown");

      log(Type.INFO, "JBoss AS instance stopped");
      unbranch();
    } catch (CommandLineException e) {
      log(Type.ERROR, "Could not shutdown JBoss AS instance. "
              + "Restarting this container while a JBoss AS instance is still running will cause errors.");
    }

    branch(Type.INFO, "Terminating command context...");
    ctx.terminateSession();
    log(Type.INFO, "Command context terminated");
    unbranch();
  }

  /**
   * @return The runtime-name for the given deployment.
   */
  private String getAppName() {
    // Deployment names must end with .war
    return appRootDir.getName().endsWith(".war") ? appRootDir.getName() : appRootDir.getName() + ".war";
  }

  /**
   * Create a log entry on the current active branch.
   * 
   * @see TreeLogger#log(Type, String, Throwable, HelpInfo)
   */
  private void log(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    branches.peek().log(type, msg, caught, helpInfo);
  }

  private void log(Type type, String msg, Throwable caught) {
    branches.peek().log(type, msg, caught);
  }

  private void log(Type type, String msg) {
    branches.peek().log(type, msg);
  }

  /**
   * Create a new active sub-branch and make this the active branch.
   * 
   * @see TreeLogger#branch(Type, String, Throwable, HelpInfo)
   */
  private void branch(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    branches.add(branches.peek().branch(type, msg, caught, helpInfo));
  }

  private void branch(Type type, String msg, Throwable caught) {
    branch(type, msg, caught, null);
  }

  private void branch(Type type, String msg) {
    branch(type, msg, null, null);
  }

  /**
   * Return to the last active branch (i.e. the parent of current active branch).
   */
  private void unbranch() {
    branches.pop();
  }
}
