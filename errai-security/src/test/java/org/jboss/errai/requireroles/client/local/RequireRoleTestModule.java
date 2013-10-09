package org.jboss.errai.requireroles.client.local;

import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.security.client.local.Identity;
import org.jboss.errai.security.shared.RequireRoles;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.InlineLabel;

@EntryPoint
public class RequireRoleTestModule extends Composite {
  
  @Inject Identity identity = new Identity();

  @RequireRoles("user") @Inject InlineLabel user;
  
  @RequireRoles("admin") @Inject InlineLabel admin;
  
}
