/*
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

package org.jboss.errai.ui.test.quickhandler.client.res;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.jboss.errai.common.client.dom.Anchor;
import org.jboss.errai.common.client.dom.Button;
import org.jboss.errai.common.client.dom.Event;
import org.jboss.errai.common.client.dom.MouseEvent;
import org.jboss.errai.common.client.dom.TextInput;
import org.jboss.errai.common.client.logging.util.StringFormat;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.ForEvent;
import org.jboss.errai.ui.shared.api.annotations.Templated;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@Templated
public class InteropEventQuickHandlerTemplate {

  public static class ObservedEvent {
    public final String dataField;
    public final String eventType;

    public ObservedEvent(final String dataField, final String eventType) {
      this.dataField = dataField;
      this.eventType = eventType;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj instanceof ObservedEvent) {
        final ObservedEvent other = (ObservedEvent) obj;
        return Objects.equals(dataField, other.dataField) && Objects.equals(eventType, other.eventType);
      }
      else {
        return false;
      }
    }

    @Override
    public String toString() {
      return StringFormat.format("[data-field=%s, eventType=%s]", dataField, eventType);
    }
  }

  @Inject
  @DataField
  public Anchor anchor;

  @Inject
  @DataField
  public Button button;

  @Inject
  @DataField
  public TextInput input;

  public List<ObservedEvent> observed = new ArrayList<>();

  @EventHandler("anchor")
  public void onAnchorSingleOrDoubleClicked(final @ForEvent({"click", "dblclick"}) MouseEvent evt) {
    observed.add(new ObservedEvent("anchor", evt.getType()));
  }

  @EventHandler("button")
  public void onButtonSingle(final @ForEvent("click") MouseEvent evt) {
    observed.add(new ObservedEvent("button", evt.getType()));
  }

  @EventHandler("input")
  public void onInputChanged(final @ForEvent("change") Event evt) {
    observed.add(new ObservedEvent("input", evt.getType()));
  }

}
