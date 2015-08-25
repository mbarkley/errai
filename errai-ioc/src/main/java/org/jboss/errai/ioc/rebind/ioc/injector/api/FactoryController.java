package org.jboss.errai.ioc.rebind.ioc.injector.api;

import static org.jboss.errai.codegen.util.Stmt.loadVariable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.errai.codegen.Context;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaMethod;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class FactoryController {

  private final ListMultimap<MetaMethod, Statement> invokeBefore = ArrayListMultimap.create();
  private final ListMultimap<MetaMethod, Statement> invokeAfter = ArrayListMultimap.create();
  private final Map<String, Statement> proxyProperties = new HashMap<String, Statement>();

  public void addInvokeBefore(final MetaMethod method, Statement statement) {
    invokeBefore.put(method, statement);
  }

  public List<Statement> getInvokeBeforeStatements(final MetaMethod method) {
    return invokeBefore.get(method);
  }

  public void addInvokeAfter(final MetaMethod method, Statement statement) {
    invokeAfter.put(method, statement);
  }

  public List<Statement> getInvokeAfterStatements(final MetaMethod method) {
    return invokeAfter.get(method);
  }

  public Statement addProxyProperty(final String name, final Class<?> type, final Statement statement) {
    proxyProperties.put(name, new Statement() {
      @Override
      public MetaClass getType() {
        return MetaClassFactory.get(type);
      }

      @Override
      public String generate(Context context) {
        return statement.generate(context);
      }
    });

    return loadVariable(name);
  }

  public Collection<Entry<String, Statement>> getProxyProperties() {
    return proxyProperties.entrySet();
  }

}
