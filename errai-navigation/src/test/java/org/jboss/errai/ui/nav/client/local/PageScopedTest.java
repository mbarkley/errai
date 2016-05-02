/**
 * Copyright (C) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ui.nav.client.local;

import org.jboss.errai.enterprise.client.cdi.AbstractErraiCDITest;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ui.nav.client.local.res.ApplicationScopedWithPageScopedDep;
import org.jboss.errai.ui.nav.client.local.res.PageScopedA;
import org.jboss.errai.ui.nav.client.local.res.PageScopedInCycle;
import org.jboss.errai.ui.nav.client.local.testpages.CycleWithPageScoped;
import org.jboss.errai.ui.nav.client.local.testpages.PageB;

import com.google.common.collect.ImmutableMultimap;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class PageScopedTest extends AbstractErraiCDITest {

  private Navigation navigation;

  @Override
  public String getModuleName() {
    return "org.jboss.errai.ui.nav.NavigationTest";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    disableBus = true;
    super.gwtSetUp();
    navigation = IOC.getBeanManager().lookupBean(Navigation.class).getInstance();
  }

  @Override
  protected void gwtTearDown() throws Exception {
    navigation.cleanUp();
    super.gwtTearDown();
  }

  public void testPageScopedBeanHasDifferentInstancesForSeparatePages() throws Exception {
    navigation.goTo("");
    final PageScopedA page = IOC.getBeanManager().lookupBean(PageScopedA.class).getInstance();
    int lastPageInstanceNum = page.getInstanceNum();

    navigation.goTo(PageB.class, ImmutableMultimap.of());
    assertTrue("Active instance should have been different between pages.", page.getInstanceNum() != lastPageInstanceNum);
    lastPageInstanceNum = page.getInstanceNum();
    navigation.goTo("");
    assertTrue("Active instance should have been different between pages.", page.getInstanceNum() != lastPageInstanceNum);

    lastPageInstanceNum = page.getInstanceNum();
    navigation.goTo(PageB.class, ImmutableMultimap.of());
    assertTrue("Active instance should have been different between pages.", page.getInstanceNum() != lastPageInstanceNum);
  }

  public void testPageScopedInApplicationScoped() throws Exception {
    navigation.goTo("");
    final ApplicationScopedWithPageScopedDep appScoped = IOC.getBeanManager().lookupBean(ApplicationScopedWithPageScopedDep.class).getInstance();
    final PageScopedA page = appScoped.getPageScopedA();
    int lastPageInstanceNum = page.getInstanceNum();

    navigation.goTo(PageB.class, ImmutableMultimap.of());
    assertTrue("Active instance should have been different between pages.", page.getInstanceNum() != lastPageInstanceNum);

    lastPageInstanceNum = page.getInstanceNum();
    navigation.goTo("");
    assertTrue("Active instance should have been different between pages.", page.getInstanceNum() != lastPageInstanceNum);

    lastPageInstanceNum = page.getInstanceNum();
    navigation.goTo(PageB.class, ImmutableMultimap.of());
    assertTrue("Active instance should have been different between pages.", page.getInstanceNum() != lastPageInstanceNum);
  }

  public void testPageScopedObjectIsDestroyedAfterNavigatingAway() throws Exception {
    navigation.goTo(PageB.class, ImmutableMultimap.of());
    PageScopedA.numsOfDestroyed.clear();
    final PageScopedA page = IOC.getBeanManager().lookupBean(PageScopedA.class).getInstance();
    final int pageBInstanceNum = page.getInstanceNum();
    navigation.goTo("");

    assertTrue("PageScopedA predestroy was not called for PageB's instance. Destroyed instance nums: "
            + PageScopedA.numsOfDestroyed, PageScopedA.numsOfDestroyed.contains(pageBInstanceNum));
  }

  public void testCycleWithPageAndPageScoped() throws Exception {
    navigation.goTo(CycleWithPageScoped.class, ImmutableMultimap.of());
    final CycleWithPageScoped page = IOC.getBeanManager().lookupBean(CycleWithPageScoped.class).getInstance();
    final PageScopedInCycle pageScoped = IOC.getBeanManager().lookupBean(PageScopedInCycle.class).getInstance();
    assertEquals(pageScoped.getId(), page.getPageScopedInCycle().getId());
    assertEquals(page.getId(), pageScoped.getPage().getId());
  }

}
