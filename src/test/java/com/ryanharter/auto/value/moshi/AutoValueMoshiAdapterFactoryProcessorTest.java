package com.ryanharter.auto.value.moshi;

import com.google.common.collect.ImmutableSet;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static com.ryanharter.auto.value.moshi.AutoValueMoshiAdapterFactoryProcessor.ADAPTER_NAME;
import static com.ryanharter.auto.value.moshi.AutoValueMoshiAdapterFactoryProcessor.ADAPTER_PACKAGE;

/**
 * Created by rharter on 4/27/16.
 */
public class AutoValueMoshiAdapterFactoryProcessorTest {

  @Test public void generatesJsonAdapterFactory() {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.Foo", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "@AutoValue public abstract class Foo {\n"
        + "  public static JsonAdapter<Foo> jsonAdapter(Moshi moshi) {\n"
        + "    return null;\n"
        + "  }\n"
        + "  public abstract String getName();\n"
        + "  public abstract boolean isAwesome();\n"
        + "}");
    JavaFileObject source2 = JavaFileObjects.forSourceString("test.Bar", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "@AutoValue public abstract class Bar {\n"
        + "  public static JsonAdapter<Bar> jsonAdapter(Moshi moshi) {\n"
        + "    return null;\n"
        + "  }\n"
        + "  public abstract String getName();\n"
        + "}");
    JavaFileObject expected = JavaFileObjects.forSourceString("com.ryanharter.auto.value.moshi.AutoValueMoshiJsonAdapterFactory", ""
        + "package com.ryanharter.auto.value.moshi;\n"
        + "\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.annotation.Annotation;\n"
        + "import java.lang.reflect.Type;\n"
        + "import java.util.Set;\n"
        + "import test.Bar;\n"
        + "import test.Foo;\n"
        + "\n"
        + "public final class AutoValueMoshiAdapterFactory implements JsonAdapter.Factory {\n"
        + "  @Override public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {\n"
        + "    if (type.equals(Foo.class)) {\n"
        + "      return Foo.jsonAdapter(moshi);\n"
        + "    } else if (type.equals(Bar.class)) {\n"
        + "      return Bar.jsonAdapter(moshi);\n"
        + "    } else {\n"
        + "      return null;\n"
        + "    }\n"
        + "  }\n"
        + "}");
    assertAbout(javaSources())
        .that(ImmutableSet.of(source1, source2))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void args_generateJsonAdapterFactory_withCustomPackageAndName() {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.Foo", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "@AutoValue public abstract class Foo {\n"
        + "  public static JsonAdapter<Foo> jsonAdapter(Moshi moshi) {\n"
        + "    return null;\n"
        + "  }\n"
        + "  public abstract String getName();\n"
        + "  public abstract boolean isAwesome();\n"
        + "}");
    JavaFileObject source2 = JavaFileObjects.forSourceString("test.Bar", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "@AutoValue public abstract class Bar {\n"
        + "  public static JsonAdapter<Bar> jsonAdapter(Moshi moshi) {\n"
        + "    return null;\n"
        + "  }\n"
        + "  public abstract String getName();\n"
        + "}");
    JavaFileObject expected = JavaFileObjects.forSourceString("testpackage.SomeAdapterFactory", ""
        + "package testpackage;\n"
        + "\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.annotation.Annotation;\n"
        + "import java.lang.reflect.Type;\n"
        + "import java.util.Set;\n"
        + "import test.Bar;\n"
        + "import test.Foo;\n"
        + "\n"
        + "public final class SomeAdapterFactory implements JsonAdapter.Factory {\n"
        + "  @Override public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {\n"
        + "    if (type.equals(Foo.class)) {\n"
        + "      return Foo.jsonAdapter(moshi);\n"
        + "    } else if (type.equals(Bar.class)) {\n"
        + "      return Bar.jsonAdapter(moshi);\n"
        + "    } else {\n"
        + "      return null;\n"
        + "    }\n"
        + "  }\n"
        + "}");
    assertAbout(javaSources())
        .that(ImmutableSet.of(source1, source2))
        .withCompilerOptions("-A" + ADAPTER_PACKAGE + "=testpackage")
        .withCompilerOptions("-A" + ADAPTER_NAME + "=SomeAdapterFactory")
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }
}
