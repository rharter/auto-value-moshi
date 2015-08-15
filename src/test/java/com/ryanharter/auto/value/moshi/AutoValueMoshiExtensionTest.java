package com.ryanharter.auto.value.moshi;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Before;
import org.junit.Test;

import javax.tools.JavaFileObject;
import java.util.Arrays;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public class AutoValueMoshiExtensionTest {

  private JavaFileObject factoryFactory;

  @Before public void setup() {
    factoryFactory = JavaFileObjects.forSourceString("com.ryanharter.auto.value.moshi.AutoValueMoshiJsonAdapterFactory", ""
        + "package com.ryanharter.auto.value.moshi;\n"
        + "import com.squareup.moshi.JsonAdapter;\n"
        + "public class AutoValueMoshiJsonAdapterFactory {\n"
        + "public static void register(JsonAdapter.Factory adapter) {}\n"
        + "}"
    );
  }

  @Test public void simple() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import com.squareup.moshi.Json;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "@AutoValue public abstract class Test {\n"
            // Reference type
            + "public abstract String a();\n"
            // Array type
            + "public abstract int[] b();\n"
            // Primitive type
            + "public abstract int c();\n"
            // SerializedName // TODO uncomment this once the target is updated in Moshi
            + "/*@Json(name=\"_D\")*/ public abstract String d();\n"
            + "}\n"
    );

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
            + "package test;\n"
            + "\n"
            + "import com.ryanharter.auto.value.moshi.AutoValueMoshiJsonAdapterFactory;"
            + "import com.squareup.moshi.JsonAdapter;\n"
            + "import com.squareup.moshi.JsonReader;\n"
            + "import com.squareup.moshi.JsonWriter;\n"
            + "import com.squareup.moshi.Moshi;\n"
            + "import java.io.IOException;\n"
            + "import java.lang.Integer;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.String;\n"
            + "import java.lang.annotation.Annotation;\n"
            + "import java.lang.reflect.Type;\n"
            + "import java.util.Set;\n"
            + "\n"
            + "final class AutoValue_Test extends $AutoValue_Test {\n"
            + "  static {\n"
            + "    AutoValueMoshiJsonAdapterFactory.register(jsonAdapterFactory());\n"
            + "  }\n"
            + "\n"
            + "  AutoValue_Test(String a, int[] b, int c, String d) {\n"
            + "    super(a, b, c, d);\n"
            + "  }\n"
            + "\n"
            + "  public static AutoValue_TestJsonAdapterFactory jsonAdapterFactory() {\n"
            + "    return new AutoValue_TestJsonAdapterFactory();\n"
            + "  }\n"
            + "\n"
            + "  public static final class AutoValue_TestJsonAdapterFactory implements JsonAdapter.Factory {\n"
            + "    @Override\n"
            + "    public JsonAdapter<Test> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {\n"
            + "      if (!(type instanceof Test)) return null;\n"
            + "      return (JsonAdapter<Test>) new AutoValue_TestJsonAdapter(moshi);\n"
            + "    }\n"
            + "  }\n"
            + "  \n"
            + "  public static final class AutoValue_TestJsonAdapter extends JsonAdapter<Test> {\n"
            + "  \n"
            + "    private final Moshi moshi;\n"
            + "  \n"
            + "    public AutoValue_TestJsonAdapter(Moshi moshi) {\n"
            + "      this.moshi = moshi;\n"
            + "    }\n"
            + "  \n"
            + "    @Override public Test fromJson(JsonReader reader) throws IOException {\n"
            + "      reader.beginObject();\n"
            + "      String a = null;\n"
            + "      int[] b = null;\n"
            + "      Integer c = null;\n"
            + "      String d = null;\n"
            + "      while (reader.hasNext()) {\n"
            + "        String _name = reader.nextName();\n"
            + "        if (\"a\".equals(_name)) {\n"
            + "          a = moshi.adapter(String.class).fromJson(reader);\n"
            + "        } else if (\"b\".equals(_name)) {\n"
            + "          b = moshi.adapter(int[].class).fromJson(reader);\n"
            + "        } else if (\"c\".equals(_name)) {\n"
            + "          c = moshi.adapter(Integer.class).fromJson(reader);\n"
            + "        } else if (\"d\".equals(_name)) {\n"
            + "          d = moshi.adapter(String.class).fromJson(reader);\n"
            + "        }\n"
            + "      }\n"
            + "      reader.endObject();\n"
            + "      return new AutoValue_Test(a, b, c, d);\n"
            + "    }\n"
            + "  \n"
            + "    @Override public void toJson(JsonWriter writer, Test value) throws IOException {\n"
            + "      writer.beginObject();\n"
            + "      writer.name(\"a\");\n"
            + "      moshi.adapter(String.class).toJson(writer, value.a());\n"
            + "      writer.name(\"b\");\n"
            + "      moshi.adapter(int[].class).toJson(writer, value.b());\n"
            + "      writer.name(\"c\");\n"
            + "      moshi.adapter(Integer.class).toJson(writer, value.c());\n"
            + "      writer.name(\"d\");\n"
            + "      moshi.adapter(String.class).toJson(writer, value.d());\n"
            + "      writer.endObject();\n"
            + "    }\n"
            + "  }\n"
            + "}"
    );

    assertAbout(javaSources())
        .that(Arrays.asList(factoryFactory, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }
}