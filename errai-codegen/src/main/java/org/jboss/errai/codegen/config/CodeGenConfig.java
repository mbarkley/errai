package org.jboss.errai.codegen.config;

public abstract class CodeGenConfig {

  public static final String ENABLE_BLAME_PROPERTY = "errai.codegen.enable_blame";

  public static boolean isBlameEnabled() {
    return Boolean.valueOf(System.getProperty(ENABLE_BLAME_PROPERTY, "false"));
  }

}
