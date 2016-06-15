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

package org.jboss.errai.flow.cdi.api.example;

import java.util.Optional;

import javax.inject.Inject;

import org.jboss.errai.flow.cdi.api.Begin;
import org.jboss.errai.flow.cdi.api.FlowInput;
import org.jboss.errai.flow.cdi.api.FlowOutput;
import org.jboss.errai.flow.cdi.api.Step;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@Step
public class EntityForm {

  @Inject
  private FlowInput<Entity> input;

  @Inject
  private FlowOutput<Optional<Entity>> output;

  @Begin
  private void start() {
  }
}
