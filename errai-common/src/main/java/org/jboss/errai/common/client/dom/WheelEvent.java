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

package org.jboss.errai.common.client.dom;

import org.jboss.errai.common.client.api.annotations.BrowserEvent;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@BrowserEvent("wheel")
@JsType(isNative = true)
public interface WheelEvent extends MouseEvent {

  @JsOverlay static final int DOM_DELTA_PIXEL = 0;
  @JsOverlay static final int DOM_DELTA_LINE = 1;
  @JsOverlay static final int DOM_DELTA_PAGE = 2;

  @JsProperty double getDeltaX();
  @JsProperty double getDeltaY();
  @JsProperty double getDeltaZ();
  @JsProperty int getDeltaMode();

}
