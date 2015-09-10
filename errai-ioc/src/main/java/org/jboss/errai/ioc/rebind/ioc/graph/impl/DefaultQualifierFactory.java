package org.jboss.errai.ioc.rebind.ioc.graph.impl;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.inject.Named;

import org.jboss.errai.codegen.meta.HasAnnotations;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Qualifier;
import org.jboss.errai.ioc.rebind.ioc.graph.api.QualifierFactory;
import org.jboss.errai.ioc.util.CDIAnnotationUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class DefaultQualifierFactory implements QualifierFactory {

  private static final Any ANY = new Any() {
    @Override
    public Class<? extends Annotation> annotationType() {
      return Any.class;
    }
    @Override
    public String toString() {
      return "@Any";
    }
    @Override
    public boolean equals(Object obj) {
      return obj instanceof Any;
    }
  };

  private static final AnnotationWrapper ANY_WRAPPER = new AnnotationWrapper(ANY);

  private static final Default DEFAULT = new Default() {
    @Override
    public Class<? extends Annotation> annotationType() {
      return Default.class;
    }
    @Override
    public String toString() {
      return "@Default";
    }
    @Override
    public boolean equals(Object obj) {
      return obj instanceof Default;
    }
  };

  private static final AnnotationWrapper DEFAULT_WRAPPER = new AnnotationWrapper(DEFAULT);

  private static final Qualifier UNIVERSAL = new Universal();

  private static final SortedSet<AnnotationWrapper> EMPTY_SORTED_SET = new TreeSet<AnnotationWrapper>();

  private final Map<SortedSet<AnnotationWrapper>, NormalQualifier> qualifiers = new HashMap<SortedSet<AnnotationWrapper>, NormalQualifier>();

  {
    qualifiers.put(EMPTY_SORTED_SET, new NormalQualifier(Collections.<AnnotationWrapper>emptySet()));
  }

  @Override
  public Qualifier forSource(final HasAnnotations annotated) {
    final SortedSet<AnnotationWrapper> annos = getRawQualifiers(annotated);
    maybeAddDefaultForSource(annos);
    annos.add(ANY_WRAPPER);

    return getOrCreateQualifier(annos);
  }

  @Override
  public Qualifier forSink(final HasAnnotations annotated) {
    final SortedSet<AnnotationWrapper> annos = getRawQualifiers(annotated);
    maybeAddDefaultForSink(annos);

    return getOrCreateQualifier(annos);
  }

  private NormalQualifier getOrCreateQualifier(final SortedSet<AnnotationWrapper> annos) {
    NormalQualifier qualifier = qualifiers.get(annos);
    if (qualifier == null) {
      qualifier = new NormalQualifier(annos);
      qualifiers.put(annos, qualifier);
    }

    return qualifier;
  }

  private SortedSet<AnnotationWrapper> getRawQualifiers(final HasAnnotations annotated) {
    final SortedSet<AnnotationWrapper> annos = new TreeSet<AnnotationWrapper>();
    for (final Annotation anno : annotated.getAnnotations()) {
      // TODO handle stereotypes
      if (anno.annotationType().isAnnotationPresent(javax.inject.Qualifier.class)) {
        annos.add(new AnnotationWrapper(anno));
      }
    }

    return annos;
  }

  private void maybeAddDefaultForSource(final Set<AnnotationWrapper> annos) {
    if (annos.isEmpty() || onlyContainsNamed(annos)) {
      annos.add(DEFAULT_WRAPPER);
    }
  }

  private void maybeAddDefaultForSink(final Set<AnnotationWrapper> annos) {
    if (annos.isEmpty()) {
      annos.add(DEFAULT_WRAPPER);
    }
  }

  private boolean onlyContainsNamed(final Set<AnnotationWrapper> annos) {
    return annos.size() == 1 && annos.iterator().next().anno.annotationType().equals(Named.class);
  }

  @Override
  public Qualifier forUnqualified() {
    return qualifiers.get(Collections.emptySet());
  }

  @Override
  public Qualifier forUniversallyQualified() {
    return UNIVERSAL;
  }

  @Override
  public Qualifier combine(final Qualifier q1, final Qualifier q2) {
    if (q1 instanceof Universal || q2 instanceof Universal) {
      return UNIVERSAL;
    } else if (q1 instanceof NormalQualifier && q2 instanceof NormalQualifier) {
      return combineNormal((NormalQualifier) q1, (NormalQualifier) q2);
    } else {
      throw new RuntimeException("At least one unrecognized qualifier implementation: " + q1.getClass().getName()
              + " and " + q2.getClass().getName());
    }
  }

  private Qualifier combineNormal(final NormalQualifier q1, final NormalQualifier q2) {
    final Multimap<Class<? extends Annotation>, AnnotationWrapper> allAnnosByType = HashMultimap.create();
    for (final AnnotationWrapper wrapper : q1.annotations) {
      allAnnosByType.put(wrapper.anno.annotationType(), wrapper);
    }
    for (final AnnotationWrapper wrapper : q2.annotations) {
      allAnnosByType.put(wrapper.anno.annotationType(), wrapper);
    }

    for (final Class<? extends Annotation> annoType : allAnnosByType.keySet()) {
      if (allAnnosByType.get(annoType).size() == 2) {
        final Iterator<AnnotationWrapper> iter = allAnnosByType.get(annoType).iterator();
        throw new RuntimeException("Found two annotations of same type but with different values:\n\t"
                + iter.next() + "\n\t" + iter.next());
      }
    }

    return getOrCreateQualifier(new TreeSet<DefaultQualifierFactory.AnnotationWrapper>(allAnnosByType.values()));
  }

  private static final class NormalQualifier implements Qualifier {

    private final Set<AnnotationWrapper> annotations;
    private String identifier;

    private NormalQualifier(final Set<AnnotationWrapper> set) {
      this.annotations = set;
    }

    @Override
    public boolean isSatisfiedBy(final Qualifier other) {
      if (other instanceof Universal) {
        return true;
      } else if (other instanceof NormalQualifier) {
        return ((NormalQualifier) other).annotations.containsAll(annotations);
      } else {
        throw new RuntimeException("Unrecognized qualifier type: " + other.getClass().getName());
      }
    }

    @Override
    public String getIdentifierSafeString() {
      if (identifier == null) {
        final StringBuilder builder = new StringBuilder();
        for (final AnnotationWrapper wrapper : annotations) {
          builder.append(wrapper.anno.annotationType().getName().replace('.', '_'))
                 .append("__");
        }
        // Remove last delimeter
        if (annotations.size() > 0) {
          builder.delete(builder.length()-2, builder.length());
        }

        identifier = builder.toString();
      }

      return identifier;
    }

    @Override
    public String getName() {
      for (final AnnotationWrapper wrapper : annotations) {
        if (wrapper.anno.annotationType().equals(Named.class)) {
          return ((Named) wrapper.anno).value();
        }
      }

      return null;
    }

    @Override
    public String toString() {
      return annotations.toString();
    }

    @Override
    public int hashCode() {
      return annotations.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
      if (!(obj instanceof NormalQualifier)) {
        return false;
      }
      final NormalQualifier other = (NormalQualifier) obj;

      return annotations.equals(other.annotations);
    }

    @Override
    public Iterator<Annotation> iterator() {
      final Iterator<AnnotationWrapper> iter = annotations.iterator();
      return new Iterator<Annotation>() {

        @Override
        public boolean hasNext() {
          return iter.hasNext();
        }

        @Override
        public Annotation next() {
          return iter.next().anno;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  private static final class Universal implements Qualifier {
    @Override
    public boolean isSatisfiedBy(final Qualifier other) {
      return other == this;
    }

    @Override
    public String getIdentifierSafeString() {
      return "Universal";
    }

    @Override
    public String toString() {
      return getIdentifierSafeString();
    }

    @Override
    public boolean equals(final Object obj) {
      return obj instanceof Universal;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public Iterator<Annotation> iterator() {
      return Collections.<Annotation>emptyList().iterator();
    }
  }

  private static final class AnnotationWrapper implements Comparable<AnnotationWrapper> {
    private final Annotation anno;

    private AnnotationWrapper(final Annotation anno) {
      this.anno = anno;
    }

    @Override
    public int hashCode() {
      return anno.annotationType().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof AnnotationWrapper)) {
        return false;
      }
      final AnnotationWrapper other = (AnnotationWrapper) obj;

      return CDIAnnotationUtils.equals(anno, other.anno);
    }

    @Override
    public String toString() {
      return anno.toString();
    }

    @Override
    public int compareTo(final AnnotationWrapper o) {
      final int compareTo = anno.annotationType().getName().compareTo(o.anno.annotationType().getName());
      if (compareTo != 0) {
        return compareTo;
      } else if (equals(o)) {
        return 0;
      } else {
        // Arbitrary stable ordering for annotations of same type with different values.
        return anno.toString().compareTo(o.anno.toString());
      }
    }
  }

}
