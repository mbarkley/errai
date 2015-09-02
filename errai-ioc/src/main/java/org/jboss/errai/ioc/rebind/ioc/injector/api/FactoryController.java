package org.jboss.errai.ioc.rebind.ioc.injector.api;

import static org.jboss.errai.codegen.util.PrivateAccessUtil.getPrivateFieldAccessorName;
import static org.jboss.errai.codegen.util.PrivateAccessUtil.getPrivateMethodName;
import static org.jboss.errai.codegen.util.Stmt.loadVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.errai.codegen.Context;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ContextualStatementBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.InjectUtil;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class FactoryController {

  private final ListMultimap<MetaMethod, Statement> invokeBefore = ArrayListMultimap.create();
  private final ListMultimap<MetaMethod, Statement> invokeAfter = ArrayListMultimap.create();
  private final Map<String, Statement> proxyProperties = new HashMap<String, Statement>();
  private final List<Statement> initializationStatements = new ArrayList<Statement>();
  private final List<Statement> destructionStatements = new ArrayList<Statement>();
  private final Map<String, Object> attributes = new HashMap<String, Object>();
  private final Set<MetaField> exposedFields = new HashSet<MetaField>();
  private final Set<MetaMethod> exposedMethods = new HashSet<MetaMethod>();

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

  public void addInitializationStatements(final List<Statement> callbackBodyStatements) {
    initializationStatements.addAll(callbackBodyStatements);
  }

  public List<Statement> getInitializationStatements() {
    return initializationStatements;
  }

  public void addDestructionStatements(final List<Statement> callbackInstanceStatement) {
    destructionStatements.addAll(callbackInstanceStatement);
  }

  public List<Statement> getDestructionStatements() {
    return destructionStatements;
  }

  public boolean hasAttribute(final String name) {
    return attributes.containsKey(name);
  }

  public void setAttribute(final String name, final Object value) {
    attributes.put(name, value);
  }

  public Object getAttribute(final String name) {
    return attributes.get(name);
  }

  public ContextualStatementBuilder constructGetReference(final String name, final Class<?> refType) {
    return InjectUtil.constructGetReference(name, refType);
  }

  public ContextualStatementBuilder constructSetReference(final String name, final Statement value) {
    return InjectUtil.constructSetReference(name, value);
  }

  public ContextualStatementBuilder getExposedFieldStmt(final MetaField field) {
    exposedFields.add(field);

    return loadVariable("this").invoke(getPrivateFieldAccessorName(field), loadVariable("instance"));
  }

  public Statement getExposedMethodStmt(final MetaMethod method, final Statement... params) {
    exposedMethods.add(method);

    return loadVariable("this").invoke(getPrivateMethodName(method), loadVariable("instance"));
  }

}
