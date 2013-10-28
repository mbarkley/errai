package org.jboss.errai.cdi.server.gwt;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.util.Stack;

import org.jboss.errai.cdi.server.as.JBossServletContainerAdaptor;
import org.jboss.errai.cdi.server.gwt.util.CopyUtil;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.ServletContainerLauncher;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;

public class JBossLauncher extends ServletContainerLauncher {

  // Property names
  private final String JBOSS_HOME_PROPERTY = "errai.jboss.home";
  private final String JBOSS_DEBUG_PORT_PROPERTY = "errai.jboss.debug.port";
  private final String TEMPLATE_CONFIG_FILE_PROPERTY = "errai.jboss.config.file";
  private final String CLASS_HIDING_JAVA_AGENT_PROPERTY = "errai.jboss.javaagent.path";

  private final String TMP_CONFIG_FILE = "standalone-errai-dev.xml";

  private final Stack<TreeLogger> branches = new Stack<TreeLogger>();

  @Override
  public ServletContainer start(TreeLogger logger, int port, File appRootDir) throws BindException, Exception {
    branches.push(logger);

    branches.push(branches.peek().branch(Type.INFO, "Starting launcher..."));

    // Get properties
    final String JBOSS_HOME = System.getProperty(JBOSS_HOME_PROPERTY);
    final String DEBUG_PORT = System.getProperty(JBOSS_DEBUG_PORT_PROPERTY, "8001");
    final String TEMPLATE_CONFIG_FILE = System.getProperty(TEMPLATE_CONFIG_FILE_PROPERTY, "standalone-full.xml");
    final String CLASS_HIDING_JAVA_AGENT = System.getProperty(CLASS_HIDING_JAVA_AGENT_PROPERTY);

    if (JBOSS_HOME == null) {
      branches.peek()
              .log(Type.ERROR,
                      String.format(
                              "No value for %s was found: The root directory of your JBoss installation must be given to the JVM",
                              JBOSS_HOME_PROPERTY));
      throw new UnableToCompleteException();
    }
    if (CLASS_HIDING_JAVA_AGENT == null) {
      branches.peek()
              .log(Type.ERROR,
                      String.format(
                              "The local path the artifact errai.org.jboss:class-local-class-hider:jar must be given as the property %s",
                              CLASS_HIDING_JAVA_AGENT_PROPERTY));
      throw new UnableToCompleteException();
    }

    try {
      copyConfigFile(TEMPLATE_CONFIG_FILE, TMP_CONFIG_FILE, JBOSS_HOME);
    } catch (IOException e) {
      branches.peek()
              .log(Type.ERROR,
                      String.format("Unable to create temporary config file %s from %s", TMP_CONFIG_FILE,
                              TEMPLATE_CONFIG_FILE), e);
    }

    final String JBOSS_START = JBOSS_HOME + "/bin/" + getStartScriptName();

    Process process;
    try {
      branches.push(branches.peek().branch(Type.INFO, String.format("Preparing JBoss AS instance (%s)", JBOSS_START)));
      ProcessBuilder builder = new ProcessBuilder(JBOSS_START, "-c", TMP_CONFIG_FILE);

      branches.peek().log(Type.INFO, String.format("Adding JBOSS_HOME=%s to instance environment", JBOSS_HOME));
      // Necessary for JBoss AS instance to startup
      builder.environment().put("JBOSS_HOME", JBOSS_HOME);

      // Allows JVM to be debugged
      builder.environment().put(
              "JAVA_OPTS",
              String.format("-Xrunjdwp:transport=dt_socket,address=%s,server=y,suspend=n -javaagent:%s", DEBUG_PORT,
                      CLASS_HIDING_JAVA_AGENT));

      branches.peek().log(Type.INFO, "Redirecting stdout and stderr to share with this process");
      builder.inheritIO();

      process = builder.start();
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

    branches.push(branches.peek().branch(Type.INFO, "Creating servlet container controller..."));

    try {
      JBossServletContainerAdaptor controller = new JBossServletContainerAdaptor(port, appRootDir, branches.peek(),
              process);
      branches.pop().log(Type.INFO, "Controller created");
      return controller;
    } catch (UnableToCompleteException e) {
      branches.peek().log(Type.ERROR, "Could not start servlet container controller", e);
      throw new UnableToCompleteException();
    }
  }

  private void copyConfigFile(String fromName, String toName, String jBossHome) throws IOException,
          UnableToCompleteException {
    File configDir = new File(jBossHome, "standalone/configuration");
    File from = new File(configDir, fromName);
    File to = new File(configDir, toName);

    if (!from.exists()) {
      branches.peek()
              .log(Type.ERROR,
                      String.format(
                              "Config file %s does not exit. It must be created or another one must be specified with the %s JVM property.",
                              from.getAbsolutePath(), TEMPLATE_CONFIG_FILE_PROPERTY));
      throw new UnableToCompleteException();
    }

    if (to.exists()) {
      branches.peek().log(Type.WARN,
              String.format("Temporary config file %s already exists and will be deleted", to.getAbsolutePath()));
      to.delete();
    }

    to.createNewFile();

    CopyUtil.copyFile(to, from);
  }

  // TODO make portable
  private String getStartScriptName() {
    return "standalone.sh";
  }
}
