package org.jboss.errai.ioc.rebind.ioc.injector.api;

import static org.jboss.errai.codegen.util.Stmt.loadVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.errai.codegen.Context;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaField;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.util.PrivateAccessType;
import org.jboss.errai.codegen.util.Stmt;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class FactoryController {

  private final ListMultimap<MetaMethod, Statement> invokeBefore = ArrayListMultimap.create();
  private final ListMultimap<MetaMethod, Statement> invokeAfter = ArrayListMultimap.create();
  private final Map<String, Statement> proxyProperties = new HashMap<String, Statement>();
  private final List<Statement> initializationStatements = new ArrayList<Statement>();
  private final List<Statement> destructionStatements = new ArrayList<Statement>();

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

  public boolean hasAttribute(String dataBindingConfigAttr) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  public void setAttribute(String dataBindingConfigAttr, Boolean true1) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  public Statement constructGetReference(final String name, final Class<?> refType) {
    return Stmt.loadVariable("this").invoke("getReferenceAs", Stmt.loadVariable("instance"), name, refType);
  }

  public Statement constructSetReference(final String name, final Statement value) {
    return Stmt.loadVariable("this").invoke("setReference", Stmt.loadVariable("instance"), name, value);
  }

  public void addExposedField(MetaField field, PrivateAccessType both) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  public Object getAttribute(String binderModelTypeValue) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

  public void setAttribute(String binderModelTypeValue, MetaClass dataModelType) {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not yet implemented.");
  }

}
