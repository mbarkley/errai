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

package org.jboss.errai.ui.nav.client.local.res;

import java.util.Random;

import javax.inject.Inject;

import org.jboss.errai.ui.nav.client.local.api.PageScoped;
import org.jboss.errai.ui.nav.client.local.testpages.CycleWithPageScoped;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@PageScoped
public class PageScopedInCycle {

  private static final Random rand = new Random(12345);

  private final int id = rand.nextInt();

  @Inject
  private CycleWithPageScoped page;

  public CycleWithPageScoped getPage() {
    return page;
  }

  public int getId() {
    return id;
  }

}
