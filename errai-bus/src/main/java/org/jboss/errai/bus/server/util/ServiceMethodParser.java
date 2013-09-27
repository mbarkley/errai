package org.jboss.errai.bus.server.util;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.errai.bus.client.api.Local;
import org.jboss.errai.bus.client.api.messaging.MessageBus;
import org.jboss.errai.bus.client.api.messaging.MessageCallback;
import org.jboss.errai.bus.server.annotations.Command;
import org.jboss.errai.bus.server.annotations.Service;
import org.jboss.errai.bus.server.io.CommandBindingsCallback;
import org.jboss.errai.bus.server.io.ServiceMethodCallback;

public class ServiceMethodParser implements ServiceParser {

  private final String svcName;
  private final boolean local;
  private final Map<String, Method> commandPoints;
  private final Method method;

  public ServiceMethodParser(Method method) throws NotAService {
    if (!method.isAnnotationPresent(Service.class)) {
      throw new NotAService("The method " + method.getName() + " is not a service.");
    }
    this.method = method;
    svcName = ("".equals(method.getAnnotation(Service.class).value())) ? method.getName() : method.getAnnotation(
            Service.class).value();
    local = method.isAnnotationPresent(Local.class);
    commandPoints = Collections.unmodifiableMap(getCommandPoints(method));
  }

  @Override
  public String getServiceName() {
    return svcName;
  }

  @Override
  public boolean isLocal() {
    return local;
  }
  
  @Override
  public Map<String, Method> getCommandPoints() {
    return commandPoints;
  }
  
  @Override
  public boolean hasCommandPoints() {
    return getCommandPoints().size() != 0;
  }
  
  private static Map<String, Method> getCommandPoints(Method method) {
    Map<String, Method> commandPoints = new HashMap<String, Method>();
    if (method.isAnnotationPresent(Command.class)) {
      Command command = method.getAnnotation(Command.class);
      for (String cmdName : command.value()) {
        if (cmdName.equals(""))
          cmdName = method.getName();
        commandPoints.put(cmdName, method);
      }
    }
    return commandPoints;
  }

  @Override
  public Class<?> getDelegateClass() {
    return method.getDeclaringClass();
  }

  @Override
  public boolean isCallback() {
    return false;
  }

  @Override
  public boolean hasRule() {
    return false;
  }

  @Override
  public boolean hasAuthentication() {
    return false;
  }

  @Override
  public MessageCallback getCallback(Object delegate, MessageBus bus) {
    if (hasCommandPoints()) {
      return new CommandBindingsCallback(getCommandPoints(), delegate, bus);
    }
    else {
      return new ServiceMethodCallback(delegate, method);
    }
  }

  @Override
  public String toString() {
    return method.toString();
  }
  
}
