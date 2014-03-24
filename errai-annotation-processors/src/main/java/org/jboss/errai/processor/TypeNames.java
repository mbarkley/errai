package org.jboss.errai.processor;

/**
 * A collection of fully qualified type names that are of interest to our
 * annotation processors. Because Eclipse does not like annotation processors to
 * use types defined in the same workspace where they are being used, and
 * because the javax.lang.mirror API works mostly on strings rather than runtime
 * Class objects, we define all the interesting type names from GWT and Errai in
 * here as string constants.
 * 
 * @author jfuerth
 *
 */
public class TypeNames {

  static final String GWT_WIDGET = "com.google.gwt.user.client.ui.Widget";
  static final String GWT_ELEMENT = "com.google.gwt.dom.client.Element";
  static final String GWT_COMPOSITE = "com.google.gwt.user.client.ui.Composite";

  static final String TEMPLATED = "org.jboss.errai.ui.shared.api.annotations.Templated";
  static final String DATA_FIELD = "org.jboss.errai.ui.shared.api.annotations.DataField";
  static final String EVENT_HANDLER = "org.jboss.errai.ui.shared.api.annotations.EventHandler";
  static final String SINK_NATIVE = "org.jboss.errai.ui.shared.api.annotations.SinkNative";

}
