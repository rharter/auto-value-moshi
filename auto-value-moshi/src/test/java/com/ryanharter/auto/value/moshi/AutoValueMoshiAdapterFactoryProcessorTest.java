package com.ryanharter.auto.value.moshi;

import com.google.common.collect.ImmutableSet;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

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
    JavaFileObject source3 = JavaFileObjects.forSourceString("test.MyAdapterFactory", ""
        + "package test;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;\n"
        + "@MoshiAdapterFactory\n"
        + "public abstract class MyAdapterFactory implements JsonAdapter.Factory {\n"
        + "  public static JsonAdapter.Factory create() {\n"
        + "    return new AutoValueMoshi_MyAdapterFactory();\n"
        + "  }\n"
        + "}");
    JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValueMoshi_MyAdapterFactory", ""
        + "package test;\n"
        + "\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.annotation.Annotation;\n"
        + "import java.lang.reflect.Type;\n"
        + "import java.util.Set;\n"
        + "\n"
        + "public final class AutoValueMoshi_MyAdapterFactory extends MyAdapterFactory {\n"
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
        .that(ImmutableSet.of(source1, source2, source3))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void canHandleUppercasePackageNames() {
    JavaFileObject source1 = JavaFileObjects.forSourceString("com.Test.Foo", ""
        + "package com.Test;\n"
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
    JavaFileObject source3 = JavaFileObjects.forSourceString("com.Test.MyAdapterFactory", ""
        + "package com.Test;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;\n"
        + "@MoshiAdapterFactory\n"
        + "public abstract class MyAdapterFactory implements JsonAdapter.Factory {\n"
        + "  public static JsonAdapter.Factory create() {\n"
        + "    return new AutoValueMoshi_MyAdapterFactory();\n"
        + "  }\n"
        + "}");
    JavaFileObject expected = JavaFileObjects.forSourceString("com.Test.AutoValueMoshi_MyAdapterFactory", ""
        + "package com.Test;\n"
        + "\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.annotation.Annotation;\n"
        + "import java.lang.reflect.Type;\n"
        + "import java.util.Set;\n"
        + "\n"
        + "public final class AutoValueMoshi_MyAdapterFactory extends MyAdapterFactory {\n"
        + "  @Override public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {\n"
        + "    if (type.equals(Foo.class)) {\n"
        + "      return Foo.jsonAdapter(moshi);\n"
        + "    } else {\n"
        + "      return null;\n"
        + "    }\n"
        + "  }\n"
        + "}");
    assertAbout(javaSources())
        .that(ImmutableSet.of(source1, source3))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void generatesJsonAdapterFactory_notAbstract_shouldFail() {
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
    JavaFileObject source2 = JavaFileObjects.forSourceString("test.MyAdapterFactory", ""
        + "package test;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;\n"
        + "@MoshiAdapterFactory\n"
        + "public class MyAdapterFactory implements JsonAdapter.Factory {\n"
        + "  public static JsonAdapter.Factory create() {\n"
        + "    return new AutoValueMoshi_MyAdapterFactory();\n"
        + "  }\n"
        + "}");
    assertAbout(javaSources())
        .that(ImmutableSet.of(source1, source2))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .failsToCompile()
        .withErrorContaining("Must be abstract!");
  }

  @Test public void generatesJsonAdapterFactory_doesNotImplementFactory_shouldFail() {
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
    JavaFileObject source2 = JavaFileObjects.forSourceString("test.MyAdapterFactory", ""
        + "package test;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;\n"
        + "@MoshiAdapterFactory\n"
        + "public abstract class MyAdapterFactory {\n"
        + "  public static JsonAdapter.Factory create() {\n"
        + "    return new AutoValueMoshi_MyAdapterFactory();\n"
        + "  }\n"
        + "}");
    assertAbout(javaSources())
        .that(ImmutableSet.of(source1, source2))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .failsToCompile()
        .withErrorContaining("Must implement JsonAdapter.Factory!");
  }

  @Test public void generatesJsonAdapterFactory_shouldSearchUpAncestry() {
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
    JavaFileObject source3 = JavaFileObjects.forSourceString("test.MyAdapterFactoryBase", ""
        + "package test;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "public abstract class MyAdapterFactoryBase implements JsonAdapter.Factory {\n"
        + "}");
    JavaFileObject source4 = JavaFileObjects.forSourceString("test.MyAdapterFactory", ""
        + "package test;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;\n"
        + "@MoshiAdapterFactory\n"
        + "public abstract class MyAdapterFactory extends MyAdapterFactoryBase {\n"
        + "  public static JsonAdapter.Factory create() {\n"
        + "    return new AutoValueMoshi_MyAdapterFactory();\n"
        + "  }\n"
        + "}");
    JavaFileObject expected =
        JavaFileObjects.forSourceString("test.AutoValueMoshi_MyAdapterFactory", ""
            + "package test;\n"
            + "\n"
            + "import com.squareup.moshi.JsonAdapter;\n"
            + "import com.squareup.moshi.Moshi;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.annotation.Annotation;\n"
            + "import java.lang.reflect.Type;\n"
            + "import java.util.Set;\n"
            + "\n"
            + "public final class AutoValueMoshi_MyAdapterFactory extends MyAdapterFactory {\n"
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
    assertAbout(javaSources()).that(ImmutableSet.of(source1, source2, source3, source4))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void generatesJsonAdapterFactory_shouldSearchUpComplexAncestry() {
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
    JavaFileObject source3 = JavaFileObjects.forSourceString("test.IMyAdapterFactoryBase", ""
        + "package test;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "public interface IMyAdapterFactoryBase extends JsonAdapter.Factory {\n"
        + "}");
    JavaFileObject source4 = JavaFileObjects.forSourceString("test.MyAdapterFactoryBase", ""
        + "package test;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "public abstract class MyAdapterFactoryBase implements IMyAdapterFactoryBase {\n"
        + "}");
    JavaFileObject source5 = JavaFileObjects.forSourceString("test.MyAdapterFactory", ""
        + "package test;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;\n"
        + "@MoshiAdapterFactory\n"
        + "public abstract class MyAdapterFactory extends MyAdapterFactoryBase {\n"
        + "  public static JsonAdapter.Factory create() {\n"
        + "    return new AutoValueMoshi_MyAdapterFactory();\n"
        + "  }\n"
        + "}");
    JavaFileObject expected =
        JavaFileObjects.forSourceString("test.AutoValueMoshi_MyAdapterFactory", ""
            + "package test;\n"
            + "\n"
            + "import com.squareup.moshi.JsonAdapter;\n"
            + "import com.squareup.moshi.Moshi;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.annotation.Annotation;\n"
            + "import java.lang.reflect.Type;\n"
            + "import java.util.Set;\n"
            + "\n"
            + "public final class AutoValueMoshi_MyAdapterFactory extends MyAdapterFactory {\n"
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
    assertAbout(javaSources()).that(ImmutableSet.of(source1, source2, source3, source4, source5))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }
}
