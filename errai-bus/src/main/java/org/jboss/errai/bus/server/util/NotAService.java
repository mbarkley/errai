package org.jboss.errai.bus.server.util;

public class NotAService extends Exception {
  private static final long serialVersionUID = 1L;

  public NotAService(String message) {
    super(message);
  }
}