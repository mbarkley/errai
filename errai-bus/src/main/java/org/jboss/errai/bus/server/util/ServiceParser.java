package org.jboss.errai.bus.server.util;

import java.lang.reflect.Method;
import java.util.Map;

import org.jboss.errai.bus.client.api.messaging.MessageBus;
import org.jboss.errai.bus.client.api.messaging.MessageCallback;

public interface ServiceParser {

  public abstract Map<String, Method> getCommandPoints();

  public abstract boolean hasCommandPoints();

  public abstract String getServiceName();

  public abstract boolean isLocal();

  public abstract Class<?> getDelegateClass();

  public abstract boolean isCallback();

  public abstract boolean hasRule();

  public abstract boolean hasAuthentication();

  public abstract MessageCallback getCallback(Object delegate, MessageBus bus);

}