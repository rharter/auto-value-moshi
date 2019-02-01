package com.ryanharter.auto.value.moshi;

import com.google.common.collect.ImmutableSet;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public final class AutoValueMoshiAdapterFactoryProcessorTest {
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
    // This is generated into a different package and not visible to the factory
    JavaFileObject source3 = JavaFileObjects.forSourceString("test2.NotVisibleClass", ""
        + "package test2;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "@AutoValue abstract class NotVisibleClass {\n"
        + "  public static JsonAdapter<NotVisibleClass> jsonAdapter(Moshi moshi) {\n"
        + "    return null;\n"
        + "  }\n"
        + "  public abstract String getName();\n"
        + "}");
    // This adapter method generated into a different package and not visible to the factory
    JavaFileObject source4 = JavaFileObjects.forSourceString("test2.NotVisibleMethod", ""
        + "package test2;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "@AutoValue public abstract class NotVisibleMethod {\n"
        + "  static JsonAdapter<NotVisibleMethod> jsonAdapter(Moshi moshi) {\n"
        + "    return null;\n"
        + "  }\n"
        + "  public abstract String getName();\n"
        + "}");
    // This adapter method is private and thus not applicable
    JavaFileObject source5 = JavaFileObjects.forSourceString("test.PrivateMethod", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "@AutoValue public abstract class PrivateMethod {\n"
        + "  private static JsonAdapter<PrivateMethod> jsonAdapter(Moshi moshi) {\n"
        + "    return null;\n"
        + "  }\n"
        + "  public abstract String getName();\n"
        + "}");
    JavaFileObject source6 = JavaFileObjects.forSourceString("test.MyAdapterFactory", ""
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
    JavaFileObject expected =
        JavaFileObjects.forSourceString("test.AutoValueMoshi_MyAdapterFactory", "package test;\n"
            + "\n"
            + "import com.squareup.moshi.JsonAdapter;\n"
            + "import com.squareup.moshi.Moshi;\n"
            + "import com.squareup.moshi.Types;\n"
            + "import java.lang.Class;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.annotation.Annotation;\n"
            + "import java.lang.reflect.Type;\n"
            + "import java.util.Set;\n"
            + "\n"
            + "public final class AutoValueMoshi_MyAdapterFactory extends MyAdapterFactory {\n"
            + "  @Override\n"
            + "  public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, "
            + "Moshi moshi) {\n"
            + "    if (!annotations.isEmpty()) return null;\n"
            + "    Class<?> rawType = Types.getRawType(type);\n"
            + "    if (Bar.class.isAssignableFrom(rawType)) {\n"
            + "      return Bar.jsonAdapter(moshi);\n"
            + "    } else if (Foo.class.isAssignableFrom(rawType)) {\n"
            + "      return Foo.jsonAdapter(moshi);\n"
            + "    }\n"
            + "    return null;\n"
            + "  }\n"
            + "}");
    assertAbout(javaSources())
        .that(ImmutableSet.of(source1, source2, source3, source4, source5, source6))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void packagePrivateEverything() {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.Foo", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "@AutoValue abstract class Foo {\n"
        + "  static JsonAdapter<Foo> jsonAdapter(Moshi moshi) {\n"
        + "    return null;\n"
        + "  }\n"
        + "  abstract String getName();\n"
        + "  abstract boolean isAwesome();\n"
        + "}");
    JavaFileObject source2 = JavaFileObjects.forSourceString("test.Bar", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "@AutoValue abstract class Bar {\n"
        + "  static JsonAdapter<Bar> jsonAdapter(Moshi moshi) {\n"
        + "    return null;\n"
        + "  }\n"
        + "  abstract String getName();\n"
        + "}");
    JavaFileObject source5 = JavaFileObjects.forSourceString("test.MyAdapterFactory", ""
        + "package test;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;\n"
        + "@MoshiAdapterFactory\n"
        + "abstract class MyAdapterFactory implements JsonAdapter.Factory {\n"
        + "  static JsonAdapter.Factory create() {\n"
        + "    return new AutoValueMoshi_MyAdapterFactory();\n"
        + "  }\n"
        + "}");
    JavaFileObject expected =
        JavaFileObjects.forSourceString("test.AutoValueMoshi_MyAdapterFactory", "package test;\n"
            + "\n"
            + "import com.squareup.moshi.JsonAdapter;\n"
            + "import com.squareup.moshi.Moshi;\n"
            + "import com.squareup.moshi.Types;\n"
            + "import java.lang.Class;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.annotation.Annotation;\n"
            + "import java.lang.reflect.Type;\n"
            + "import java.util.Set;\n"
            + "\n"
            + "final class AutoValueMoshi_MyAdapterFactory extends MyAdapterFactory {\n"
            + "  @Override\n"
            + "  public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, "
            + "Moshi moshi) {\n"
            + "    if (!annotations.isEmpty()) return null;\n"
            + "    Class<?> rawType = Types.getRawType(type);\n"
            + "    if (Bar.class.isAssignableFrom(rawType)) {\n"
            + "      return Bar.jsonAdapter(moshi);\n"
            + "    } else if (Foo.class.isAssignableFrom(rawType)) {\n"
            + "      return Foo.jsonAdapter(moshi);\n"
            + "    }\n"
            + "    return null;\n"
            + "  }\n"
            + "}");
    assertAbout(javaSources()).that(ImmutableSet.of(source1, source2, source5))
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
    JavaFileObject expected =
        JavaFileObjects.forSourceString("com.Test.AutoValueMoshi_MyAdapterFactory", ""
            + "package com.Test;\n"
            + "\n"
            + "import com.squareup.moshi.JsonAdapter;\n"
            + "import com.squareup.moshi.Moshi;\n"
            + "import com.squareup.moshi.Types;\n"
            + "import java.lang.Class;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.annotation.Annotation;\n"
            + "import java.lang.reflect.Type;\n"
            + "import java.util.Set;\n"
            + "\n"
            + "public final class AutoValueMoshi_MyAdapterFactory extends MyAdapterFactory {\n"
            + "  @Override public JsonAdapter<?> create(Type type, "
            + "     Set<? extends Annotation> annotations, Moshi moshi) {\n"
            + "    if (!annotations.isEmpty()) return null;\n"
            + "    Class<?> rawType = Types.getRawType(type);\n"
            + "    if (Foo.class.isAssignableFrom(rawType)) {\n"
            + "      return Foo.jsonAdapter(moshi);\n"
            + "    }\n"
            + "    return null;\n"
            + "  }\n"
            + "}");
    assertAbout(javaSources()).that(ImmutableSet.of(source1, source3))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void failsIfJsonAdapterFactoryNotAbstract() {
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
    assertAbout(javaSources()).that(ImmutableSet.of(source1, source2))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .failsToCompile()
        .withErrorContaining("Must be abstract!");
  }

  @Test public void failsIfJsonAdapterFactoryDoesNotImplementFactory() {
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
    assertAbout(javaSources()).that(ImmutableSet.of(source1, source2))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .failsToCompile()
        .withErrorContaining("Must implement JsonAdapter.Factory!");
  }

  @Test public void generatesJsonAdapterFactoryShouldSearchUpAncestry() {
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
        JavaFileObjects.forSourceString("test.AutoValueMoshi_MyAdapterFactory", "package test;\n"
            + "\n"
            + "import com.squareup.moshi.JsonAdapter;\n"
            + "import com.squareup.moshi.Moshi;\n"
            + "import com.squareup.moshi.Types;\n"
            + "import java.lang.Class;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.annotation.Annotation;\n"
            + "import java.lang.reflect.Type;\n"
            + "import java.util.Set;\n"
            + "\n"
            + "public final class AutoValueMoshi_MyAdapterFactory extends MyAdapterFactory {\n"
            + "  @Override\n"
            + "  public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, "
            + "Moshi moshi) {\n"
            + "    if (!annotations.isEmpty()) return null;\n"
            + "    Class<?> rawType = Types.getRawType(type);\n"
            + "    if (Bar.class.isAssignableFrom(rawType)) {\n"
            + "      return Bar.jsonAdapter(moshi);\n"
            + "    } else if (Foo.class.isAssignableFrom(rawType)) {\n"
            + "      return Foo.jsonAdapter(moshi);\n"
            + "    }\n"
            + "    return null;\n"
            + "  }\n"
            + "}");
    assertAbout(javaSources()).that(ImmutableSet.of(source1, source2, source3, source4))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void generatesJsonAdapterFactoryShouldSearchUpComplexAncestry() {
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
        JavaFileObjects.forSourceString("test.AutoValueMoshi_MyAdapterFactory", "package test;\n"
            + "\n"
            + "import com.squareup.moshi.JsonAdapter;\n"
            + "import com.squareup.moshi.Moshi;\n"
            + "import com.squareup.moshi.Types;\n"
            + "import java.lang.Class;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.annotation.Annotation;\n"
            + "import java.lang.reflect.Type;\n"
            + "import java.util.Set;\n"
            + "\n"
            + "public final class AutoValueMoshi_MyAdapterFactory extends MyAdapterFactory {\n"
            + "  @Override\n"
            + "  public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, "
            + "Moshi moshi) {\n"
            + "    if (!annotations.isEmpty()) return null;\n"
            + "    Class<?> rawType = Types.getRawType(type);\n"
            + "    if (Bar.class.isAssignableFrom(rawType)) {\n"
            + "      return Bar.jsonAdapter(moshi);\n"
            + "    } else if (Foo.class.isAssignableFrom(rawType)) {\n"
            + "      return Foo.jsonAdapter(moshi);\n"
            + "    }\n"
            + "    return null;\n"
            + "  }\n"
            + "}");
    assertAbout(javaSources()).that(ImmutableSet.of(source1, source2, source3, source4, source5))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void generatesJsonAdapterFactoryGenerics() {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.Foo", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import java.lang.reflect.Type;\n"
        + "import java.util.List;\n"
        + "@AutoValue public abstract class Foo<V, T> {\n"
        + "  public static <V, T>JsonAdapter<Foo<V, T>> jsonAdapter(Moshi moshi, "
        + "      Type[] types) {\n"
        + "    return null;\n"
        + "  }\n"
        + "  public abstract List<V> getItems();\n"
        + "  public abstract List<T> getHeaders();\n"
        + "}");
    JavaFileObject source3 = JavaFileObjects.forSourceString("test.MyAdapterFactory", ""
        + "package test.factory;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;\n"
        + "@MoshiAdapterFactory\n"
        + "public abstract class MyAdapterFactory implements JsonAdapter.Factory {\n"
        + "  public static JsonAdapter.Factory create() {\n"
        + "    return new AutoValueMoshi_MyAdapterFactory();\n"
        + "  }\n"
        + "}");
    JavaFileObject expected =
        JavaFileObjects.forSourceString("test.AutoValueMoshi_MyAdapterFactory", ""
            + "package test.factory;\n"
            + "\n"
            + "import com.squareup.moshi.JsonAdapter;\n"
            + "import com.squareup.moshi.Moshi;\n"
            + "import com.squareup.moshi.Types;\n"
            + "import java.lang.Class;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.annotation.Annotation;\n"
            + "import java.lang.reflect.ParameterizedType;\n"
            + "import java.lang.reflect.Type;\n"
            + "import java.util.Set;\n"
            + "import test.Foo;\n"
            + "\n"
            + "public final class AutoValueMoshi_MyAdapterFactory extends MyAdapterFactory {\n"
            + "  @Override public JsonAdapter<?> create(Type type, "
            + "     Set<? extends Annotation> annotations, Moshi moshi) {\n"
            + "    if (!annotations.isEmpty()) return null;\n"
            + "    Class<?> rawType = Types.getRawType(type);\n"
            + "    if (type instanceof ParameterizedType) {\n"
            + "      if (Foo.class.isAssignableFrom(rawType)) {\n"
            + "        return Foo.jsonAdapter(moshi, "
            + "          ((ParameterizedType) type).getActualTypeArguments());\n"
            + "      }\n"
            + "      return null;\n"
            + "    }\n"
            + "    return null;\n"
            + "  }\n"
            + "}");
    assertAbout(javaSources()).that(ImmutableSet.of(source1, source3))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void generatesJsonAdapterFactoryThatReturnsNullSafeAdapters() throws Exception {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.Foo", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import java.lang.reflect.Type;\n"
        + "import java.util.List;\n"
        + "@AutoValue public abstract class Foo<V, T> {\n"
        + "  public static <V, T> JsonAdapter<Foo<V, T>> jsonAdapter(Moshi moshi, "
        + "      Type[] types) {\n"
        + "    return null;\n"
        + "  }\n"
        + "  public abstract List<V> getItems();\n"
        + "  public abstract List<T> getHeaders();\n"
        + "}");
    JavaFileObject source2 = JavaFileObjects.forSourceString("test.Bar", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import java.lang.reflect.Type;\n"
        + "import java.util.List;\n"
        + "@AutoValue public abstract class Bar {\n"
        + "  public static JsonAdapter<Bar> jsonAdapter(Moshi moshi) {\n"
        + "    return null;\n"
        + "  }\n"
        + "  public abstract String value();\n"
        + "}");
    JavaFileObject source3 = JavaFileObjects.forSourceString("test.MyAdapterFactory", ""
        + "package test.factory;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "import com.squareup.moshi.Moshi;\n"
        + "import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;\n"
        + "@MoshiAdapterFactory(nullSafe = true)\n"
        + "public abstract class MyAdapterFactory implements JsonAdapter.Factory {\n"
        + "  public static JsonAdapter.Factory create() {\n"
        + "    return new AutoValueMoshi_MyAdapterFactory();\n"
        + "  }\n"
        + "}");
    JavaFileObject expected =
        JavaFileObjects.forSourceString("test.AutoValueMoshi_MyAdapterFactory", ""
            + "package test.factory;\n"
            + "import com.squareup.moshi.JsonAdapter;\n"
            + "import com.squareup.moshi.Moshi;\n"
            + "import com.squareup.moshi.Types;\n"
            + "import java.lang.Class;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.annotation.Annotation;\n"
            + "import java.lang.reflect.ParameterizedType;\n"
            + "import java.lang.reflect.Type;\n"
            + "import java.util.Set;\n"
            + "import test.Bar;\n"
            + "import test.Foo;\n"
            + "\n"
            + "public final class AutoValueMoshi_MyAdapterFactory extends MyAdapterFactory {\n"
            + "  @Override public JsonAdapter<?> create(Type type,"
            + "      Set<? extends Annotation> annotations, Moshi moshi) {\n"
            + "    if (!annotations.isEmpty()) return null;\n"
            + "    Class<?> rawType = Types.getRawType(type);\n"
            + "    if (type instanceof ParameterizedType) {\n"
            + "      if (Foo.class.isAssignableFrom(rawType)) {\n"
            + "        return Foo.jsonAdapter(moshi, "
            + "          ((ParameterizedType) type).getActualTypeArguments()).nullSafe();\n"
            + "      }\n"
            + "      return null;\n"
            + "    }\n"
            + "    if (Bar.class.isAssignableFrom(rawType)) {\n"
            + "      return Bar.jsonAdapter(moshi).nullSafe();\n"
            + "    }\n"
            + "    return null;\n"
            + "  }\n"
            + "}");
    assertAbout(javaSources()).that(ImmutableSet.of(source1, source2, source3))
        .processedWith(new AutoValueMoshiAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }
}
