package com.ryanharter.auto.value.moshi;

import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValueExtension;
import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

@AutoService(AutoValueExtension.class)
public class AutoValueMoshiExtension implements AutoValueExtension {

  public static class Property {
    String name;
    ExecutableElement element;
    TypeName type;

    public Property() {}

    public Property(String name, ExecutableElement element) {
      this.name = name;
      this.element = element;

      type = TypeName.get(element.getReturnType());
    }

    public String serializedName() {
      Json json = element.getAnnotation(Json.class);
      if (json != null) {
        return json.name();
      } else {
        return name;
      }
    }
  }

  @Override
  public boolean applicable(Context context) {
    return true;
  }

  @Override
  public boolean mustBeAtEnd(Context context) {
    return false;
  }

  @Override
  public String generateClass(Context context, String className, String classToExtend, boolean isFinal) {
    List<Property> properties = readProperties(context.properties());

    Map<String, TypeName> types = convertPropertiesToTypes(context.properties());

    ClassName classNameClass = ClassName.get(context.packageName(), className);
    ClassName autoValueClass = ClassName.bestGuess(context.autoValueClass().getQualifiedName().toString());

    TypeSpec typeAdapter = createTypeAdapter(classNameClass, autoValueClass, properties);
    TypeSpec typeAdapterFactory = createJsonAdapterFactory(autoValueClass, typeAdapter);
    MethodSpec typeAdapterFactoryMethod = createJsonAdapterFactoryMethod(typeAdapterFactory);

    TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
        .superclass(TypeVariableName.get(classToExtend))
        .addType(typeAdapterFactory)
        .addType(typeAdapter)
        .addMethod(generateConstructor(types))
        .addMethod(typeAdapterFactoryMethod);

    if (isFinal) {
      subclass.addModifiers(FINAL);
    } else {
      subclass.addModifiers(ABSTRACT);
    }

    return JavaFile.builder(context.packageName(), subclass.build()).build().toString();
  }

  public List<Property> readProperties(Map<String, ExecutableElement> properties) {
    List<Property> values = new LinkedList<Property>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      values.add(new Property(entry.getKey(), entry.getValue()));
    }
    return values;
  }

  MethodSpec generateConstructor(Map<String, TypeName> properties) {
    List<ParameterSpec> params = Lists.newArrayList();
    for (Map.Entry<String, TypeName> entry : properties.entrySet()) {
      params.add(ParameterSpec.builder(entry.getValue(), entry.getKey()).build());
    }

    MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addParameters(params);

    StringBuilder superFormat = new StringBuilder("super(");
    for (int i = properties.size(); i > 0; i--) {
      superFormat.append("$N");
      if (i > 1) superFormat.append(", ");
    }
    superFormat.append(")");
    builder.addStatement(superFormat.toString(), properties.keySet().toArray());

    return builder.build();
  }

  /**
   * Converts the ExecutableElement properties to TypeName properties
   */
  Map<String, TypeName> convertPropertiesToTypes(Map<String, ExecutableElement> properties) {
    Map<String, TypeName> types = new LinkedHashMap<String, TypeName>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      ExecutableElement el = entry.getValue();
      types.put(entry.getKey(), TypeName.get(el.getReturnType()));
    }
    return types;
  }

  public MethodSpec createJsonAdapterFactoryMethod(TypeSpec typeAdapterFactory) {
    return MethodSpec.methodBuilder("typeAdapterFactory")
        .addModifiers(PUBLIC, STATIC)
        .returns(ClassName.bestGuess(typeAdapterFactory.name))
        .addStatement("return new $N()", typeAdapterFactory)
        .build();
  }

  public TypeSpec createJsonAdapterFactory(ClassName autoValueClassName, TypeSpec typeAdapter) {
    String customJsonAdapterFactory = String.format("AutoValue_%sJsonAdapterFactory", autoValueClassName.simpleName());

    TypeName jsonAdapterType = ParameterizedTypeName.get(ClassName.get(JsonAdapter.class), autoValueClassName);
    TypeName setOfAnnotations = ParameterizedTypeName.get(ClassName.get(Set.class), WildcardTypeName.subtypeOf(Annotation.class));
    ParameterSpec type = ParameterSpec.builder(Type.class, "type").build();
    ParameterSpec annotations = ParameterSpec.builder(setOfAnnotations, "annotations").build();
    ParameterSpec moshi = ParameterSpec.builder(Moshi.class, "moshi").build();
    MethodSpec createMethod = MethodSpec.methodBuilder("create")
        .addModifiers(PUBLIC)
        .addAnnotation(Override.class)
        .returns(jsonAdapterType)
        .addParameters(Arrays.asList(type, annotations, moshi))
        .addStatement("if (!$N.equals($T.class)) return null", type, autoValueClassName)
        .addStatement("return ($T) new $N($N)", jsonAdapterType, typeAdapter, moshi)
        .build();

    return TypeSpec.classBuilder(customJsonAdapterFactory)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addSuperinterface(JsonAdapter.Factory.class)
        .addMethod(createMethod)
        .build();
  }

  public TypeSpec createTypeAdapter(ClassName className, ClassName autoValueClassName, List<Property> properties) {
    ClassName jsonAdapter = ClassName.get(JsonAdapter.class);
    TypeName typeAdapterClass = ParameterizedTypeName.get(jsonAdapter, autoValueClassName);

    FieldSpec moshiField = FieldSpec.builder(Moshi.class, "moshi", PRIVATE, FINAL).build();

    String customTypeAdapterClass = String.format("AutoValue_%sJsonAdapter", autoValueClassName.simpleName());
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(customTypeAdapterClass)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .superclass(typeAdapterClass)
        .addField(moshiField)
        .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(Moshi.class, "moshi")
                .addStatement("this.$N = moshi", moshiField)
                .build()
        )
        .addMethod(createReadMethod(moshiField, className, autoValueClassName, properties))
        .addMethod(createWriteMethod(moshiField, autoValueClassName, properties));


    return classBuilder.build();
  }

  public MethodSpec createWriteMethod(FieldSpec moshiField, ClassName autoValueClassName, List<Property> properties) {
    ParameterSpec writer = ParameterSpec.builder(JsonWriter.class, "writer").build();
    ParameterSpec value = ParameterSpec.builder(autoValueClassName, "value").build();
    MethodSpec.Builder writeMethod = MethodSpec.methodBuilder("toJson")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .addParameter(writer)
        .addParameter(value)
        .addException(IOException.class);

    writeMethod.addStatement("$N.beginObject()", writer);
    for (Property prop : properties) {
      writeMethod.addStatement("$N.name($S)", writer, prop.serializedName());

      TypeName typeName = prop.type.isPrimitive() ? prop.type.box() : prop.type;

      if (typeName instanceof ParameterizedTypeName) {
        writeMethod.addStatement("$N.<$T>adapter($L).toJson($N, $N.$N())", moshiField, typeName,
            makeType((ParameterizedTypeName) typeName), writer, value, prop.name);
      } else {
        writeMethod.addStatement("$N.adapter($T.class).toJson($N, $N.$N())", moshiField, typeName,
            writer, value, prop.name);
      }
    }
    writeMethod.addStatement("$N.endObject()", writer);

    return writeMethod.build();
  }

  public MethodSpec createReadMethod(FieldSpec moshiField, ClassName className,
                                     ClassName autoValueClassName, List<Property> properties) {
    ParameterSpec reader = ParameterSpec.builder(JsonReader.class, "reader").build();
    MethodSpec.Builder readMethod = MethodSpec.methodBuilder("fromJson")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(autoValueClassName)
        .addParameter(reader)
        .addException(IOException.class);

    readMethod.addStatement("$N.beginObject()", reader);

    // add the properties
    Map<Property, FieldSpec> fields = new LinkedHashMap<Property, FieldSpec>(properties.size());
    for (Property prop : properties) {
      FieldSpec field = FieldSpec.builder(prop.type, prop.name).build();
      fields.put(prop, field);

      readMethod.addStatement("$T $N = null", field.type.isPrimitive() ? field.type.box() : field.type, field);
    }

    readMethod.beginControlFlow("while ($N.hasNext())", reader);

    FieldSpec name = FieldSpec.builder(String.class, "_name").build();
    readMethod.addStatement("$T $N = $N.nextName()", name.type, name, reader);

    boolean first = true;
    for (Map.Entry<Property, FieldSpec> entry : fields.entrySet()) {
      Property prop = entry.getKey();
      FieldSpec field = entry.getValue();
      if (first) readMethod.beginControlFlow("if ($S.equals($N))", prop.serializedName(), name);
      else readMethod.nextControlFlow("else if ($S.equals($N))", prop.serializedName(), name);

      TypeName typeName = field.type.isPrimitive() ? field.type.box() : field.type;

      if (typeName instanceof ParameterizedTypeName) {
        readMethod.addStatement("$N = $N.<$T>adapter($L).fromJson($N)", field, moshiField,
            typeName, makeType((ParameterizedTypeName) typeName), reader);
      } else {
        readMethod.addStatement("$N = $N.adapter($T.class).fromJson($N)", field, moshiField,
            typeName, reader);
      }


      first = false;
    }
    readMethod.endControlFlow(); // if/else if
    readMethod.endControlFlow(); // while

    readMethod.addStatement("$N.endObject()", reader);

    StringBuilder format = new StringBuilder("return new ");
    format.append(className.simpleName().replaceAll("\\$", ""));
    format.append("(");
    Iterator<FieldSpec> iterator = fields.values().iterator();
    while (iterator.hasNext()) {
      iterator.next();
      format.append("$N");
      if (iterator.hasNext()) format.append(", ");
    }
    format.append(")");
    readMethod.addStatement(format.toString(), fields.values().toArray());

    return readMethod.build();
  }

  private CodeBlock makeType(ParameterizedTypeName type) {
    CodeBlock.Builder block = CodeBlock.builder();
    block.add("$T.newParameterizedType($T.class", Types.class, type.rawType);
    for (TypeName typeArg : type.typeArguments) {
      if (typeArg instanceof ParameterizedTypeName) {
        block.add(", $L", makeType((ParameterizedTypeName) typeArg));
      } else {
        block.add(", $T.class", typeArg);
      }
    }
    block.add(")");
    return block.build();
  }
}
