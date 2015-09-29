package org.jboss.errai.ioc.client.container.async;

import java.lang.annotation.Annotation;
import java.util.Collection;

import javax.enterprise.inject.Alternative;

import org.jboss.errai.ioc.client.container.DestructionCallback;

/**
 * @author Mike Brock
 */
@Alternative
public class AsyncBeanManagerImpl implements AsyncBeanManager {

  @Override
  public void destroyBean(Object ref) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public boolean isManaged(Object ref) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public Object getActualBeanReference(Object ref) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public boolean isProxyReference(Object ref) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public boolean addDestructionCallback(Object beanInstance, DestructionCallback<?> destructionCallback) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public void destroyAllBeans() {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public void destroyBean(Object ref, Runnable runnable) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public Collection<AsyncBeanDef> lookupBeans(String name) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public <T> Collection<AsyncBeanDef<T>> lookupBeans(Class<T> type) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public <T> Collection<AsyncBeanDef<T>> lookupBeans(Class<T> type, Annotation... qualifiers) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  @Override
  public <T> AsyncBeanDef<T> lookupBean(Class<T> type, Annotation... qualifiers) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }
}
