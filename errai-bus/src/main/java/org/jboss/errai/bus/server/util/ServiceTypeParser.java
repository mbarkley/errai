package org.jboss.errai.bus.server.util;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.errai.bus.client.api.Local;
import org.jboss.errai.bus.server.annotations.Command;
import org.jboss.errai.bus.server.annotations.Remote;
import org.jboss.errai.bus.server.annotations.Service;

public class ServiceTypeParser {
  
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
  
  public Map<String, Method> getCommandPoints() {
    return commandPoints;
  }
  
  public boolean hasCommandPoints() {
    return commandPoints.size() != 0;
  }
  
  public Class<?> getRemoteImplementation() {
    return getRemoteImplementation(clazz);
  }
  
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
      if (method.isAnnotationPresent(Command.class)) {
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
  
  public class NotAService extends Exception {
    private static final long serialVersionUID = 1L;

    public NotAService(String message) {
      super(message);
    }
  }
  
}
