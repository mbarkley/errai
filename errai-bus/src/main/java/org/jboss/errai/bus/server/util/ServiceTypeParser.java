package org.jboss.errai.bus.server.util;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.errai.bus.client.api.Local;
import org.jboss.errai.bus.client.api.messaging.MessageBus;
import org.jboss.errai.bus.client.api.messaging.MessageCallback;
import org.jboss.errai.bus.server.annotations.Command;
import org.jboss.errai.bus.server.annotations.Remote;
import org.jboss.errai.bus.server.annotations.Service;
import org.jboss.errai.bus.server.annotations.security.RequireAuthentication;
import org.jboss.errai.bus.server.annotations.security.RequireRoles;
import org.jboss.errai.bus.server.io.CommandBindingsCallback;

public class ServiceTypeParser implements ServiceParser {
  
  private final Class<?> clazz;
  private final Map<String, Method> commandPoints;
  private final String svcName;
  private final boolean local;

  public ServiceTypeParser(Class<?> clazz) throws NotAService {
    this.clazz = clazz;

    Service svcAnnotation = clazz.getAnnotation(Service.class);
    if (null == svcAnnotation) {
      throw new NotAService("The class " + clazz.getName() + " is not a service");
    }

    local = clazz.isAnnotationPresent(Local.class);
    svcName = resolveServiceName(clazz);

    this.commandPoints = Collections.unmodifiableMap(getCommandPoints(clazz));
  }
  
  /* (non-Javadoc)
   * @see org.jboss.errai.bus.server.util.ServiceParser#getCommandPoints()
   */
  @Override
  public Map<String, Method> getCommandPoints() {
    return commandPoints;
  }
  
  /* (non-Javadoc)
   * @see org.jboss.errai.bus.server.util.ServiceParser#hasCommandPoints()
   */
  @Override
  public boolean hasCommandPoints() {
    return commandPoints.size() != 0;
  }
  
  public Class<?> getRemoteImplementation() {
    return getRemoteImplementation(clazz);
  }
  
  /* (non-Javadoc)
   * @see org.jboss.errai.bus.server.util.ServiceParser#getServiceName()
   */
  @Override
  public String getServiceName() {
    return svcName;
  }
  
  private static Class<?> getRemoteImplementation(Class<?> type) {
    for (Class<?> iface : type.getInterfaces()) {
      if (iface.isAnnotationPresent(Remote.class)) {
        return iface;
      }
      else if (iface.getInterfaces().length != 0 && ((iface = getRemoteImplementation(iface)) != null)) {
        return iface;
      }
    }
    return null;
  }
  
  /* (non-Javadoc)
   * @see org.jboss.errai.bus.server.util.ServiceParser#isLocal()
   */
  @Override
  public boolean isLocal() {
    return local;
  }

  public static String resolveServiceName(final Class<?> type) {
    String subjectName = type.getAnnotation(Service.class).value();
  
    if (subjectName.equals(""))
      subjectName = type.getSimpleName();
  
    return subjectName;
  }
  
  public static Map<String, Method> getCommandPoints(Class<?> clazz) {
    Map<String, Method> commandPoints = new HashMap<String, Method>();
    for (final Method method : clazz.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Command.class) && !method.isAnnotationPresent(Service.class)) {
        Command command = method.getAnnotation(Command.class);
        for (String cmdName : command.value()) {
          if (cmdName.equals(""))
            cmdName = method.getName();
          commandPoints.put(cmdName, method);
        }
      }
    }
    return commandPoints;
  }

  @Override
  public Class<?> getDelegateClass() {
    return clazz;
  }

  @Override
  public boolean isCallback() {
    return MessageCallback.class.isAssignableFrom(clazz) && !hasCommandPoints();
  }

  @Override
  public boolean hasRule() {
    return clazz.isAnnotationPresent(RequireRoles.class);
  }

  @Override
  public boolean hasAuthentication() {
    return clazz.isAnnotationPresent(RequireAuthentication.class);
  }

  @Override
  public MessageCallback getCallback(Object delegate, MessageBus bus) {
    if (isCallback()) {
      return (MessageCallback) delegate;
    }
    else if (hasCommandPoints()) {
      return new CommandBindingsCallback(getCommandPoints(), delegate, bus);
    }
    else {
      return null;
    }
  }
  
}
