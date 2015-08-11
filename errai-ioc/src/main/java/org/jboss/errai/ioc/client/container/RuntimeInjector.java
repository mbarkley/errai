package org.jboss.errai.ioc.client.container;

public interface RuntimeInjector<T> {

  T create(Context context);

}
