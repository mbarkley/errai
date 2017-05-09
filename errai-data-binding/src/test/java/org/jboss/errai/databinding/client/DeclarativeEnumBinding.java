/*
 * Copyright (C) 2017 Red Hat, Inc. and/or its affiliates.
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

package org.jboss.errai.databinding.client;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.databinding.client.api.DataBinder;
import org.jboss.errai.ui.shared.api.annotations.AutoBound;
import org.jboss.errai.ui.shared.api.annotations.Bound;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasValue;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@Dependent
public class DeclarativeEnumBinding {

  @Inject @AutoBound public DataBinder<TestModelWithEnum> binder;

  @Bound public final SimpleHasValue<EnumModel> enumValue = new SimpleHasValue<>();

  public DeclarativeEnumBinding() {
    enumValue.setValue(EnumModel.B, false);
  }

  public static class SimpleHasValue<T> implements HasValue<T> {
    private T value;
    private final List<ValueChangeHandler<T>> handlers = new ArrayList<>();

    @Override
    public T getValue() {
      return value;
    }

    @Override
    public void setValue(final T value) {
      setValue(value, true);
    }

    @Override
    public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<T> handler) {
      handlers.add(handler);
      return () -> handlers.remove(handler);
    }

    @Override
    public void fireEvent(final GwtEvent<?> event) {
      if (event instanceof ValueChangeEvent) {
        handlers.forEach(h -> h.onValueChange((ValueChangeEvent<T>) event));
      }
    }

    @Override
    public void setValue(final T value, final boolean fireEvents) {
      final T oldValue = this.value;
      this.value = value;
      if (fireEvents) {
        ValueChangeEvent.fireIfNotEqual(this, oldValue, value);
      }
    }
  }

}
