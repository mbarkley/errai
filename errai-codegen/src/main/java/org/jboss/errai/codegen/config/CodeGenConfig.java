package org.jboss.errai.codegen.config;

public abstract class CodeGenConfig {

  public static final String ENABLE_BLAME_PROPERTY = "errai.codegen.enable_blame";
  public static final String ENABLE_SCOPE_CHECK_PROPERTY = "errai.codegen.enable_scope_check";

  private static final boolean isBlameEnabled;
  private static final boolean isScopeCheckEnabled;

  static {
    isBlameEnabled = Boolean.valueOf(System.getProperty(ENABLE_BLAME_PROPERTY, "false"));
    isScopeCheckEnabled = Boolean.valueOf(System.getProperty(ENABLE_SCOPE_CHECK_PROPERTY, "false"));
  }

  public static boolean isBlameEnabled() {
    return isBlameEnabled;
  }

  public static boolean isScopeCheckEnabled() {
    return isScopeCheckEnabled;
  }

}
