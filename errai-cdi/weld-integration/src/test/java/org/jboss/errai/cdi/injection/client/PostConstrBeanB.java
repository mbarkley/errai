package org.jboss.errai.cdi.injection.client;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author Mike Brock
 */
@ApplicationScoped
public class PostConstrBeanB {
  @Inject
  PostConstrBeanC postConstrBeanC;

  // required to make proxyable
  public PostConstrBeanB() {
  }

  @Inject
  public PostConstrBeanB(PostConstrBeanA selfRefProxy) {

  }

  @PostConstruct
  private void postConstr() {
    PostConstructTestUtil.record(PostConstrBeanB.class.getName());
  }
}
