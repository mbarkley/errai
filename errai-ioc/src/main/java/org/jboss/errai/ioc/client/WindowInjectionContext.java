package org.jboss.errai.ioc.client;

import java.util.HashMap;
import java.util.Map;

import org.jboss.errai.ioc.client.container.IOCResolutionException;
import org.jboss.errai.ioc.client.container.JsTypeProvider;

import com.google.gwt.core.client.js.JsType;

/**
 * @author Christian Sadilek <csadilek@redhat.com>
 * @author Max Barkley <mbarkley@redhat.com>
 */
@JsType
public class WindowInjectionContext {
  private Map<String, JsTypeProvider<?>> beanProviders = new HashMap<String, JsTypeProvider<?>>();

  public static WindowInjectionContext createOrGet() {
    if (!isWindowInjectionContextDefined()) {
      setWindowInjectionContext(new WindowInjectionContext());
    }
    return getWindowInjectionContext();
  }

  public static native WindowInjectionContext getWindowInjectionContext() /*-{
    return $wnd.injectionContext;
  }-*/;

  public static native void setWindowInjectionContext(WindowInjectionContext ic) /*-{
    $wnd.injectionContext = ic;
  }-*/;

  public static native boolean isWindowInjectionContextDefined() /*-{
    return !($wnd.injectionContext === undefined);
  }-*/;

 public <T> T getInstance(String injectorName) {
   return null;
 }

  public void addBeanProvider(final String name, final JsTypeProvider<?> provider) {
    beanProviders.put(name, provider);
  }

  public void addSuperTypeAlias(final String superTypeName, final String typeName) {
    final JsTypeProvider<?> provider = beanProviders.get(typeName);

    if (provider != null) {
      beanProviders.put(superTypeName, provider);
    }
  }

  public Object getBean(final String name) {
    final JsTypeProvider<?> provider = beanProviders.get(name);

    if (provider == null) {
      throw new IOCResolutionException("no matching bean instances for: " + name);
    }

    return provider.getInstance();
  }

}