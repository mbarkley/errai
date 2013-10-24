package org.jboss.errai.cdi.server.gwt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jboss.errai.cdi.server.as.JBossServletContainerAdaptor;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.ServletContainerLauncher;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.thirdparty.guava.common.io.Files;

public class JBossLauncher extends ServletContainerLauncher {

  private final String JBOSS_ZIP_RESOURCE = "jboss-as-7.1.1.Final.zip";
  // TODO make portable

  private final Stack<TreeLogger> branches = new Stack<TreeLogger>();

  private final int BUF_SIZE = 1024;

  @Override
  public ServletContainer start(TreeLogger logger, int port, File appRootDir) throws BindException, Exception {
    branches.push(logger);

    branches.push(branches.peek().branch(Type.INFO, "Starting launcher..."));

    branches.push(branches.peek().branch(Type.INFO, String.format("Unpacking %s...", JBOSS_ZIP_RESOURCE)));
    final String JBOSS_HOME = unpack();
    branches.pop().log(Type.INFO, String.format("Finished unpacking %s", JBOSS_ZIP_RESOURCE));

    final String JBOSS_START = JBOSS_HOME + "/bin/standalone-debug.sh";

    Process process;
    try {
      branches.push(branches.peek().branch(Type.INFO, String.format("Preparing JBoss AS instance (%s)", JBOSS_START)));
      ProcessBuilder builder = new ProcessBuilder(JBOSS_START);

      branches.peek().log(Type.INFO, String.format("Adding JBOSS_HOME=%s to instance environment", JBOSS_HOME));
      builder.environment().put("JBOSS_HOME", JBOSS_HOME);

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

  // TODO clean up these files when done
  private String unpack() throws UnableToCompleteException {
    File tmpDir = Files.createTempDir();
    branches.peek().log(Type.INFO, String.format("Created root directory %s", tmpDir.getAbsolutePath()));

    branches.peek().log(Type.INFO, String.format("Opening stream for %s", JBOSS_ZIP_RESOURCE));
    ZipInputStream in = new ZipInputStream(ClassLoader.getSystemResourceAsStream(JBOSS_ZIP_RESOURCE));
    ZipEntry entry;
    // Get first file/directory in zip
    try {
      entry = in.getNextEntry();
    } catch (IOException e1) {
      branches.peek().log(Type.ERROR, String.format("Could not unpack %s", JBOSS_ZIP_RESOURCE), e1);
      throw new UnableToCompleteException();
    }
    while (entry != null) {
      if (entry.isDirectory()){
        final File file = new File(tmpDir, entry.getName());
        if (!file.mkdirs()) {
          branches.peek().log(Type.WARN, String.format("Failed to make directories for %s", file.getAbsolutePath()));
        }
      }
      else {
        final File file = new File(tmpDir, entry.getName());
        if (!file.getParentFile().mkdirs()) {
          branches.peek().log(Type.WARN, String.format("Failed to make directories for %s", file.getAbsolutePath()));
        }
        try {
          branches.peek().log(Type.DEBUG, String.format("Creating %s...", file.getAbsolutePath()));
          file.createNewFile();
        } catch (IOException e1) {
          branches.peek().log(Type.ERROR, String.format("Could not create file %s", file.getAbsolutePath()), e1);
          throw new UnableToCompleteException();
        }

        // Create file writer
        final FileOutputStream writer;
        try {
          writer = new FileOutputStream(file);
        } catch (FileNotFoundException e1) {
          branches.peek().log(Type.ERROR,
                  String.format("Could not create FileOutputStream for %s", file.getAbsolutePath()), e1);
          throw new UnableToCompleteException();
        }

        branches.peek().log(Type.DEBUG, String.format("Writing to %s...", file.getAbsolutePath()));
        final byte[] buf = new byte[BUF_SIZE];
        int len = 0;
        while (len != -1) {
          // Fill and write buffer
          try {
            len = in.read(buf);
            if (len > 0) {
              writer.write(buf, 0, len);
            }
          } catch (IOException e) {
            branches.peek().log(Type.ERROR, String.format("Could not write to %s", file.getAbsolutePath()), e);
            try {
              writer.close();
            } catch (IOException e1) {
              branches.peek().log(Type.WARN, String.format("Could not close writer stream for %s", file.getName()), e1);
            }
            throw new UnableToCompleteException();
          }
        }

        try {
          writer.close();
        } catch (IOException e) {
          branches.pop().log(Type.WARN, String.format("Could not close writer stream for %s", file.getName()), e);
        }
      }
      // Get next file/directory
      try {
        entry = in.getNextEntry();
      } catch (IOException e1) {
        branches.peek().log(Type.ERROR, String.format("Could not unpack %s", JBOSS_ZIP_RESOURCE), e1);
        throw new UnableToCompleteException();
      }
    }

    branches.push(branches.peek().branch(Type.INFO, "Fixing permissions..."));
    fixPerms(tmpDir);

    return tmpDir.getAbsolutePath() + File.separator + tmpDir.listFiles()[0].getName();
  }

  private void fixPerms(File file) {
    branches.peek().log(Type.DEBUG, String.format("Setting permissions for %s", file.getAbsolutePath()));
    file.setReadable(true);
    file.setWritable(true);
    file.setExecutable(true);

    branches.peek().log(Type.DEBUG, String.format("Descending into %s...", file.getAbsolutePath()));
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        fixPerms(child);
      }
    }
  }
}
