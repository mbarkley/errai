package org.jboss.errai.ioc.rebind.ioc.injector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.errai.codegen.ProxyMaker.ProxyProperty;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.ioc.rebind.ioc.graph.Injectable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class InjectorImpl implements Injector {

  private final Injectable injectable;

  private final Map<String, Object> attributes = new HashMap<String, Object>();
  private final List<Statement> endStatements = new ArrayList<Statement>();
  private final ListMultimap<MetaMethod, Statement> invokeBefore = ArrayListMultimap.create();
  private final ListMultimap<MetaMethod, Statement> invokeAfter = ArrayListMultimap.create();
  private final Map<String, ProxyProperty> proxyProperties = new HashMap<String, ProxyProperty>();

  public InjectorImpl(final Injectable injectable) {
    this.injectable = injectable;
  }

  @Override
  public MetaClass getInjectedType() {
    return injectable.getInjectedType();
  }

  @Override
  public String getInstanceVarName() {
    return "instance";
  }

  @Override
  public void setAttribute(final String name, final Object value) {
    attributes.put(name, value);
  }

  @Override
  public Object getAttribute(final String name) {
    return attributes.get(name);
  }

  @Override
  public boolean hasAttribute(final String name) {
    return attributes.containsKey(name);
  }

  @Override
  public void addStatementToEndOfInjector(final Statement statement) {
    endStatements.add(statement);
  }

  @Override
  public void addInvokeBefore(final MetaMethod method, final Statement statement) {
    injectable.setRequiresProxyTrue();
    invokeBefore.put(method, statement);
  }

  @Override
  public void addInvokeAfter(final MetaMethod method, final Statement statement) {
    injectable.setRequiresProxyTrue();
    invokeAfter.put(method, statement);
  }

  @Override
  public ProxyProperty addProxyProperty(final String propertyName, final Class<?> type, final Statement statement) {
    return addProxyProperty(propertyName, MetaClassFactory.get(type), statement);
  }

  @Override
  public ProxyProperty addProxyProperty(final String propertyName, final MetaClass type, final Statement statement) {
    injectable.setRequiresProxyTrue();
    final ProxyProperty proxyProp = new ProxyProperty(propertyName, type, statement);
    proxyProperties.put(propertyName, proxyProp);

    return proxyProp;
  }

  public Map<String, ProxyProperty> getProxyPropertyMap() {
    return proxyProperties;
  }

  public ListMultimap<MetaMethod, Statement> getInvokeBeforeMultimap() {
    return invokeBefore;
  }

  public ListMultimap<MetaMethod, Statement> getInvokeAfterMultimap() {
    return invokeAfter;
  }

}
