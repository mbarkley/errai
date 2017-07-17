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

package org.jboss.errai.common.client.framework;

import static org.jboss.errai.common.client.framework.Constants.ERRAI_CSRF_TOKEN_VAR;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
public abstract class ClientCSRFTokenCache {

  private ClientCSRFTokenCache() {
  }

  public static native boolean hasAssignedCSRFToken()/*-{
    return $wnd.erraiCSRFToken != null;
  }-*/;

  public static native String getAssignedCSRFToken()/*-{
    return $wnd.erraiCSRFToken;
  }-*/;

  public static native void setAssignedCSRFToken(final String newValue)/*-{
    $wnd.erraiCSRFToken = newValue;
  }-*/;

}
