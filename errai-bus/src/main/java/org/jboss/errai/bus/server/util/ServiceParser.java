package org.jboss.errai.bus.server.util;

import java.lang.reflect.Method;
import java.util.Map;

public interface ServiceParser {

  public abstract Map<String, Method> getCommandPoints();

  public abstract boolean hasCommandPoints();

  public abstract String getServiceName();

  public abstract boolean isLocal();

}