package org.jboss.errai.ioc.rebind.ioc.injector;

import java.util.Map;

import org.jboss.errai.codegen.ProxyMaker;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaMethod;

/**
 * Defines an injector which is responsible for providing instance references of beans to the code generating
 * container.
 *
 * @author Mike Brock
 */
public interface Injector {

  /**
   * The injected type of the injector. This is the absolute type which the injector produces. For producers, this
   * is the bean type which the producer method returns.
   *
   * @return the return type from the injector.
   */
  MetaClass getInjectedType();

  /**
   * The unique variable name for the bean instance. Usually used to reference the bean during the wiring of the
   * bean within the CreationalContext.getInstance() method body. This variable name is also used to provide
   * a name to variable which holds a reference to singleton instances.
   *
   * @return the unique variable name for a bean in the bootstrapper and CreationalContext.getInstance() method.
   */
  String getInstanceVarName();

  /**
   * Sets a persistent attribute to be associated with this injector.
   *
   * @param name the name of the attribute
   * @param value the value of the attribute.
   */
  public void setAttribute(String name, Object value);

  /**
   * Gets a persistent attribute associated with this injector.
   *
   * @param name the name of the attribute
   * @return the value of the attribute. null if the attribute does not exist.
   */
  public Object getAttribute(String name);

  /**
   * Checks if injector has the specified named attribute
   *
   * @param name the name of the attribute.
   * @return true if the attribute exists.
   */
  public boolean hasAttribute(String name);

  /**
   * Adds a statement to be appended to the end of the generated {@link org.jboss.errai.ioc.client.container.BeanProvider}
   * code. Statements added here will be executed after all bean wiring activity has finished.
   *
   * @param statement
   */
  public void addStatementToEndOfInjector(Statement statement);

  /**
   * Adds an invoke before statement on the specified method.
   *
   * Calling this method automatically converts this injector into a proxied injector, as AOP activities must be done
   * through the creation of a proxy.

   * @param method the method to invoke around
   * @param statement the statement to execute.
   */
  void addInvokeBefore(MetaMethod method, Statement statement);

  /**
   * Adds an invoke after statement on the specified method.
   *
   * Calling this method automatically converts this injector into a proxied injector, as AOP activities must be done
   * through the creation of a proxy.

   * @param method the method to invoke around
   * @param statement the statement to execute.
   */
  void addInvokeAfter(MetaMethod method, Statement statement);

  /**
   * Adds a proxy property to the generated proxy. Effectively this means the proxy will be given an instance
   * field to hold the value yielded by the specified statement.
   *
   * @param propertyName
   *        the name of the property.
   * @param type
   *        the type of the property.
   * @param statement
   *        the statement which will yield the value to be put into the property.
   *
   * @return
   *        a {@link org.jboss.errai.codegen.ProxyMaker.ProxyProperty} reference which can be used as a regular
   *        statement reference in Errai Codegen. The instance of ProxyProperty can be used in generated code
   *        (such as in AOP statements) to refer to the injected proxy property.
   */
  ProxyMaker.ProxyProperty addProxyProperty(String propertyName, Class<?> type, Statement statement);

  /**
   * Adds a proxy property to the generated proxy. Effectively this means the proxy will be given an instance
   * field to hold the value yielded by the specified statement.
   *
   * @param propertyName
   *        the name of the property.
   * @param type
   *        the type of the property.
   * @param statement
   *        the statement which will yield the value to be put into the property.
   *
   * @return
   *        a {@link org.jboss.errai.codegen.ProxyMaker.ProxyProperty} reference which can be used as a regular
   *        statement reference in Errai Codegen. The instance of ProxyProperty can be used in generated code
   *        (such as in AOP statements) to refer to the injected proxy property.
   */
  ProxyMaker.ProxyProperty addProxyProperty(String propertyName, MetaClass type, Statement statement);


  /**
   * Returns a map of all proxy properties in the injector. The keys are the names of the property, and the values
   * are {@link org.jboss.errai.codegen.ProxyMaker.ProxyProperty} references.
   *
   * @return
   *      a map of proxy properties.
   */
  Map<String, ProxyMaker.ProxyProperty> getProxyPropertyMap();
}
