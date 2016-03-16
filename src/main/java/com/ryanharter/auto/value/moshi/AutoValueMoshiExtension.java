package com.ryanharter.auto.value.moshi;

import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.ryanharter.auto.value.moshi.annotation.DefaultBoolean;
import com.ryanharter.auto.value.moshi.annotation.DefaultByte;
import com.ryanharter.auto.value.moshi.annotation.DefaultChar;
import com.ryanharter.auto.value.moshi.annotation.DefaultDouble;
import com.ryanharter.auto.value.moshi.annotation.DefaultFloat;
import com.ryanharter.auto.value.moshi.annotation.DefaultInt;
import com.ryanharter.auto.value.moshi.annotation.DefaultLong;
import com.ryanharter.auto.value.moshi.annotation.DefaultShort;
import com.ryanharter.auto.value.moshi.annotation.DefaultString;
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
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

@AutoService(AutoValueExtension.class)
public class AutoValueMoshiExtension extends AutoValueExtension {

  public static class Property {
    String name;
    ExecutableElement element;
    TypeName type;
    ImmutableSet<String> annotations;

    public Property() {}

    public Property(String name, ExecutableElement element) {
      this.name = name;
      this.element = element;

      type = TypeName.get(element.getReturnType());
      annotations = buildAnnotations(element);
    }

    public String serializedName() {
      Json json = element.getAnnotation(Json.class);
      if (json != null) {
        return json.name();
      } else {
        return name;
      }
    }

    public Boolean nullable() {
      return annotations.contains("Nullable");
    }

    public Boolean defaultBoolean() {
      DefaultBoolean annotation = element.getAnnotation(DefaultBoolean.class);
      return annotation == null ? null : annotation.value();
    }

    public Byte defaultByte() {
      DefaultByte annotation = element.getAnnotation(DefaultByte.class);
      return annotation == null ? null : annotation.value();
    }

    public Character defaultChar() {
      DefaultChar annotation = element.getAnnotation(DefaultChar.class);
      return annotation == null ? null : annotation.value();
    }

    public Double defaultDouble() {
      DefaultDouble annotation = element.getAnnotation(DefaultDouble.class);
      return annotation == null ? null : annotation.value();
    }

    public Float defaultFloat() {
      DefaultFloat annotation = element.getAnnotation(DefaultFloat.class);
      return annotation == null ? null : annotation.value();
    }

    public Integer defaultInt() {
      DefaultInt annotation = element.getAnnotation(DefaultInt.class);
      return annotation == null ? null : annotation.value();
    }

    public Long defaultLong() {
      DefaultLong annotation = element.getAnnotation(DefaultLong.class);
      return annotation == null ? null : annotation.value();
    }

    public Short defaultShort() {
      DefaultShort annotation = element.getAnnotation(DefaultShort.class);
      return annotation == null ? null : annotation.value();
    }

    public String defaultString() {
      DefaultString annotation = element.getAnnotation(DefaultString.class);
      return annotation == null ? null : annotation.value();
    }

    private ImmutableSet<String> buildAnnotations(ExecutableElement element) {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();

      List<? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
      for (AnnotationMirror annotation : annotations) {
        builder.add(annotation.getAnnotationType().asElement().getSimpleName().toString());
      }

      return builder.build();
    }
  }

  @Override
  public boolean applicable(Context context) {
    return true;
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

  ImmutableMap<Property, FieldSpec> createFields(List<Property> properties) {
    ImmutableMap.Builder<Property, FieldSpec> fields = ImmutableMap.builder();

    ClassName jsonAdapter = ClassName.get(JsonAdapter.class);
    for (Property property : properties) {
      TypeName type = property.type.isPrimitive() ? property.type.box() : property.type;
      ParameterizedTypeName adp = ParameterizedTypeName.get(jsonAdapter, type);
      fields.put(property,
          FieldSpec.builder(adp, property.name + "Adapter", PRIVATE, FINAL).build());
    }

    return fields.build();
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

    ImmutableMap<Property, FieldSpec> adapters = createFields(properties);

    MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameter(Moshi.class, "moshi");

    for (Map.Entry<Property, FieldSpec> entry : adapters.entrySet()) {
      Property prop = entry.getKey();
      FieldSpec field = entry.getValue();
      if (entry.getKey().type instanceof ParameterizedTypeName) {
        constructor.addStatement("this.$N = moshi.adapter($L)", field,
            makeType((ParameterizedTypeName) prop.type));
      } else {
        TypeName type = prop.type.isPrimitive() ? prop.type.box() : prop.type;
        constructor.addStatement("this.$N = moshi.adapter($T.class)", field, type);
      }
    }

    String customTypeAdapterClass = String.format("AutoValue_%sJsonAdapter", autoValueClassName.simpleName());
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(customTypeAdapterClass)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .superclass(typeAdapterClass)
        .addFields(adapters.values())
        .addMethod(constructor.build())
        .addMethod(createReadMethod(className, autoValueClassName, adapters))
        .addMethod(createWriteMethod(autoValueClassName, adapters));


    return classBuilder.build();
  }

  public MethodSpec createWriteMethod(ClassName autoValueClassName,
                                      ImmutableMap<Property, FieldSpec> adapters) {
    ParameterSpec writer = ParameterSpec.builder(JsonWriter.class, "writer").build();
    ParameterSpec value = ParameterSpec.builder(autoValueClassName, "value").build();
    MethodSpec.Builder writeMethod = MethodSpec.methodBuilder("toJson")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .addParameter(writer)
        .addParameter(value)
        .addException(IOException.class);

    writeMethod.addStatement("$N.beginObject()", writer);
    for (Map.Entry<Property, FieldSpec> entry : adapters.entrySet()) {
      Property prop = entry.getKey();
      FieldSpec field = entry.getValue();

      if (prop.nullable()) {
        writeMethod.beginControlFlow("if ($N.$N() != null)", value, prop.name);
        writeMethod.addStatement("$N.name($S)", writer, prop.serializedName());
        writeMethod.addStatement("$N.toJson($N, $N.$N())", field, writer, value, prop.name);
        writeMethod.endControlFlow();
      } else {
        writeMethod.addStatement("$N.name($S)", writer, prop.serializedName());
        writeMethod.addStatement("$N.toJson($N, $N.$N())", field, writer, value, prop.name);
      }
    }
    writeMethod.addStatement("$N.endObject()", writer);

    return writeMethod.build();
  }

  public MethodSpec createReadMethod(ClassName className,
                                     ClassName autoValueClassName,
                                     ImmutableMap<Property, FieldSpec> adapters) {
    ParameterSpec reader = ParameterSpec.builder(JsonReader.class, "reader").build();
    MethodSpec.Builder readMethod = MethodSpec.methodBuilder("fromJson")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(autoValueClassName)
        .addParameter(reader)
        .addException(IOException.class);

    ClassName token = ClassName.get(JsonReader.Token.NULL.getClass());

    readMethod.addStatement("$N.beginObject()", reader);

    // add the properties
    Map<Property, FieldSpec> fields = new LinkedHashMap<Property, FieldSpec>(adapters.size());
    for (Property prop : adapters.keySet()) {
      FieldSpec field = FieldSpec.builder(prop.type, prop.name).build();
      fields.put(prop, field);

      TypeName boxedType = field.type.isPrimitive() ? field.type.box() : field.type;
      if (TypeName.get(Boolean.class).equals(boxedType)) {
        readMethod.addStatement("$T $N = $L", boxedType, field, prop.defaultBoolean());
      } else if (TypeName.get(Byte.class).equals(boxedType)) {
        readMethod.addStatement("$T $N = $L", boxedType, field, prop.defaultByte());
      } else if (TypeName.get(Character.class).equals(boxedType)) {
        readMethod.addStatement("$T $N = $L", boxedType, field, prop.defaultChar());
      } else if (TypeName.get(Double.class).equals(boxedType)) {
        readMethod.addStatement("$T $N = $L", boxedType, field, prop.defaultDouble());
      } else if (TypeName.get(Float.class).equals(boxedType)) {
        readMethod.addStatement("$T $N = $L", boxedType, field, prop.defaultFloat());
      } else if (TypeName.get(Integer.class).equals(boxedType)) {
        readMethod.addStatement("$T $N = $L", boxedType, field, prop.defaultInt());
      } else if (TypeName.get(Long.class).equals(boxedType)) {
        readMethod.addStatement("$T $N = $L", boxedType, field, prop.defaultLong());
      } else if (TypeName.get(Short.class).equals(boxedType)) {
        readMethod.addStatement("$T $N = $L", boxedType, field, prop.defaultShort());
      } else if (TypeName.get(String.class).equals(prop.type)) {
        readMethod.addStatement("$T $N = $S", boxedType, field, prop.defaultString());
      } else {
        readMethod.addStatement("$T $N = null", boxedType, field);
      }
    }

    readMethod.beginControlFlow("while ($N.hasNext())", reader);

    FieldSpec name = FieldSpec.builder(String.class, "_name").build();
    readMethod.addStatement("$T $N = $N.nextName()", name.type, name, reader);

    // check if JSON field value is NULL
    readMethod.beginControlFlow("if ($N.peek() == $T.NULL)", reader, token);
    readMethod.addStatement("$N.skipValue()", reader);
    readMethod.addStatement("continue");
    readMethod.endControlFlow();

    readMethod.beginControlFlow("switch ($N)", name);
    for (Map.Entry<Property, FieldSpec> entry : fields.entrySet()) {
      Property prop = entry.getKey();
      FieldSpec field = entry.getValue();

      readMethod.beginControlFlow("case $S:", prop.serializedName());
      readMethod.addStatement("$N = $N.fromJson($N)", field, adapters.get(prop), reader);
      readMethod.addStatement("break");
      readMethod.endControlFlow();
    }

    // skip value if field is not serialized...
    readMethod.beginControlFlow("default:");
    readMethod.addStatement("$N.skipValue()", reader);
    readMethod.endControlFlow();

    readMethod.endControlFlow(); // switch
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
