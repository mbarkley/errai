package org.jboss.errai.bus.server.util;

import java.lang.reflect.Method;
import java.util.Map;

import org.jboss.errai.bus.client.api.messaging.MessageBus;
import org.jboss.errai.bus.client.api.messaging.MessageCallback;

public abstract class ServiceParser {
  
  protected boolean local;
  protected String svcName;
  protected Map<String, Method> commandPoints;

  public Map<String, Method> getCommandPoints() {
    return commandPoints;
  }

  public boolean hasCommandPoints() {
    return getCommandPoints().size() != 0;
  }

  public String getServiceName() {
    return svcName;
  }

  public boolean isLocal() {
    return local;
  }

  public abstract Class<?> getDelegateClass();

  public abstract boolean isCallback();

  public abstract boolean hasRule();

  public abstract boolean hasAuthentication();

  public abstract MessageCallback getCallback(Object delegateInstance, MessageBus bus);

}