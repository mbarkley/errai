package org.jboss.errai.ioc.rebind.ioc.graph;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;

import org.jboss.errai.codegen.meta.HasAnnotations;

public class DefaultQualifierFactory implements QualifierFactory {

  private static final Any ANY = new Any() {
    @Override
    public Class<? extends Annotation> annotationType() {
      return Any.class;
    }
  };

  private static final Default DEFAULT = new Default() {
    @Override
    public Class<? extends Annotation> annotationType() {
      return Default.class;
    }
  };

  private static final Qualifier UNIVERSAL = new Universal();

  private final Map<Set<Annotation>, NormalQualifier> qualifiers = new HashMap<Set<Annotation>, NormalQualifier>();

  {
    qualifiers.put(Collections.<Annotation>emptySet(), new NormalQualifier(Collections.<Annotation>emptySet()));
  }

  @Override
  public Qualifier forConcreteInjectable(final HasAnnotations annotated) {
    final Set<Annotation> annos = getQualifierAnnotations(annotated);
    annos.add(ANY);

    return getOrCreateQualifier(annos);
  }

  @Override
  public Qualifier forAbstractInjectable(final HasAnnotations annotated) {
    final Set<Annotation> annos = getQualifierAnnotations(annotated);

    return getOrCreateQualifier(annos);
  }

  private NormalQualifier getOrCreateQualifier(final Set<Annotation> annos) {
    NormalQualifier qualifier = qualifiers.get(annos);
    if (qualifier == null) {
      qualifier = new NormalQualifier(annos);
      qualifiers.put(annos, qualifier);
    }
    return qualifier;
  }

  private Set<Annotation> getQualifierAnnotations(final HasAnnotations annotated) {
    final Set<Annotation> annos = new HashSet<Annotation>();
    for (final Annotation anno : annotated.getAnnotations()) {
      // TODO handle stereotypes
      if (anno.annotationType().isAnnotationPresent(javax.inject.Qualifier.class)) {
        annos.add(anno);
      }
    }
    addDefaulIfUnqualified(annos);

    return annos;
  }

  private void addDefaulIfUnqualified(final Set<Annotation> annos) {
    if (annos.isEmpty()) {
      annos.add(DEFAULT);
    }
  }

  @Override
  public Qualifier forUnqualified() {
    return qualifiers.get(Collections.emptySet());
  }

  @Override
  public Qualifier forUniversallyQualified() {
    return UNIVERSAL;
  }

  private static final class NormalQualifier implements Qualifier {

    private final Set<Annotation> annotations;
    private String identifier;

    private NormalQualifier(final Set<Annotation> annotations) {
      this.annotations = annotations;
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
        for (final Annotation anno : annotations) {
          builder.append(anno.annotationType().getName().replace('.', '_'));
        }

        identifier = builder.toString();
      }

      return identifier;
    }

    @Override
    public String toString() {
      return getIdentifierSafeString();
    }

    @Override
    public int hashCode() {
      return annotations.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof NormalQualifier)) {
        return false;
      }
      final NormalQualifier other = (NormalQualifier) obj;

      return annotations.equals(other.annotations);
    }
  }

  private static final class Universal implements Qualifier {
    @Override
    public boolean isSatisfiedBy(final Qualifier other) {
      return other == this;
    }

    @Override
    public String getIdentifierSafeString() {
      return "org_jboss_errai_ioc_qual_Universal";
    }

    @Override
    public String toString() {
      return getIdentifierSafeString();
    }
  }

}
