package com.ryanharter.auto.value.moshi;

import com.google.auto.value.AutoValue;
import com.squareup.javapoet.ClassName;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

/**
 * Represents a proguard configuration for a given spec. This covers three main areas:
 * <ul>
 * <li>Keeping the target class name to Moshi's reflective lookup of the adapter if it's external.</li>
 * <li>Keeping the generated adapter class name + public constructor for reflective lookup if it's external.</li>
 * <li>Keeping any used JsonQualifier annotations and the properties they are attached to.</li>
 * <li>If the target class has default parameter values, also keeping the associated synthetic constructor as well as the DefaultConstructorMarker type Kotlin adds to it.</li>
 * </ul>
 * <p>
 * Each rule is intended to be as specific and targeted as possible to reduce footprint, and each is
 * conditioned on usage of the original target type.
 * <p>
 * To keep this processor as an {@code ISOLATING} incremental processor, we generate one file per
 * target class with a deterministic name (see {@link #outputFile}) with an appropriate originating
 * element.
 */
@AutoValue
abstract class ProguardConfig {
  abstract boolean isExternal();
  abstract ClassName targetClass();
  abstract String adapterName();
  abstract List<String> adapterConstructorParams();
  abstract Set<QualifierAdapterProperty> qualifierProperties();
  abstract String outputFile();

  static ProguardConfig create(
      boolean isExternal,
      ClassName targetClass,
      String adapterName,
      List<String> adapterConstructorParams,
      Set<QualifierAdapterProperty> qualifierProperties) {
    String outputFile = "META-INF/proguard/avm-" + targetClass.canonicalName() + ".pro";
    return new AutoValue_ProguardConfig(isExternal,
        targetClass,
        adapterName,
        adapterConstructorParams,
        qualifierProperties,
        outputFile);
  }

  /** Writes this to {@code filer}. */
  final void writeTo(Filer filer, Element... originatingElements) throws IOException {
    try (Writer writer = filer.createResource(CLASS_OUTPUT, "", outputFile(), originatingElements)
        .openWriter()) {
      writeTo(writer);
    }
  }

  private void writeTo(Appendable out) throws IOException {
    //
    // -if class {the target class}
    // -keepnames class {the target class}
    // -if class {the target class}
    // -keep class {the generated adapter} {
    //    <init>(...);
    //    private final {adapter fields}
    // }
    //
    String targetName = targetClass().canonicalName();
    String adapterCanonicalName
        = ClassName.get(targetClass().packageName(), adapterName()).canonicalName();

    if (isExternal()) {
      // Keep the class name for Moshi's reflective lookup based on it
      out.append("-if class ")
          .append(targetName)
          .append("\n");
      out.append("-keepnames class ")
          .append(targetName)
          .append("\n");
    }

    if (isExternal() || !qualifierProperties().isEmpty()) {
      out.append("-if class ")
          .append(targetName)
          .append("\n");
      out.append("-keep class ")
          .append(adapterCanonicalName)
          .append(" {\n");

      if (isExternal()) {
        // Keep the constructor for Moshi's reflective lookup
        String constructorArgs = String.join(",", adapterConstructorParams());
        out.append("    public <init>(")
            .append(constructorArgs)
            .append(");\n");
      }
      // Keep any qualifier properties
      for (QualifierAdapterProperty qualifierProperty : qualifierProperties()) {
        out.append("    private com.squareup.moshi.JsonAdapter ")
            .append(qualifierProperty.name())
            .append(";\n");
      }
      out.append("}\n");

      qualifierProperties().stream()
          .flatMap(prop -> prop.qualifiers().stream())
          .map(ClassName::canonicalName)
          .sorted()
          .forEach(qualifier -> {
            try {
              out.append("-if class ")
                  .append(targetName)
                  .append("\n")
                  .append("-keep @interface ")
                  .append(qualifier)
                  .append("\n");
            } catch (IOException e) {
              // Annoyingly in Java lambdas, this block does not inherit the throws signature of the
              // method this is run in.
              throw new RuntimeException(e);
            }
          });
    }
  }

  /**
   * Represents a qualified property with its {@link #name} in the adapter fields and list of
   * {@link #qualifiers} associated with it.
   */
  @AutoValue
  abstract static class QualifierAdapterProperty {

    static QualifierAdapterProperty create(String name, Set<ClassName> qualifiers) {
      return new AutoValue_ProguardConfig_QualifierAdapterProperty(name, qualifiers);
    }

    abstract String name();
    abstract Set<ClassName> qualifiers();
  }
}
