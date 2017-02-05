package com.ryanharter.auto.value.moshi;

import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonQualifier;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

@AutoService(AutoValueExtension.class)
public class AutoValueMoshiExtension extends AutoValueExtension {
  private static final ClassName ADAPTER_CLASS_NAME = ClassName.get(JsonAdapter.class);

  public static class Property {
    final String methodName;
    final String humanName;
    final ExecutableElement element;
    final TypeName type;
    final ImmutableSet<String> annotations;

    public Property(String name, ExecutableElement element) {
      this.methodName = element.getSimpleName().toString();
      this.humanName = name;
      this.element = element;

      type = TypeName.get(element.getReturnType());
      annotations = buildAnnotations(element);
    }

    public String serializedName() {
      Json json = element.getAnnotation(Json.class);
      if (json != null) {
        return json.name();
      } else {
        return humanName;
      }
    }

    public boolean nullable() {
      return annotations.contains("Nullable");
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

  @Override public boolean applicable(Context context) {
    // check that the class contains a public static method returning a JsonAdapter
    TypeElement type = context.autoValueClass();
    ParameterizedTypeName jsonAdapterType = ParameterizedTypeName.get(
        ADAPTER_CLASS_NAME, TypeName.get(type.asType()));
    TypeName returnedJsonAdapter = null;
    for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
      if (method.getModifiers().contains(Modifier.STATIC)
          && method.getModifiers().contains(Modifier.PUBLIC)) {
        TypeMirror rType = method.getReturnType();
        TypeName returnType = TypeName.get(rType);
        if (returnType.equals(jsonAdapterType)) {
          return true;
        }

        if (returnType.equals(jsonAdapterType.rawType)
            || (returnType instanceof ParameterizedTypeName
            && ((ParameterizedTypeName) returnType).rawType.equals(jsonAdapterType.rawType))) {
          returnedJsonAdapter = returnType;
        }
      }
    }

    if (returnedJsonAdapter == null) {
      return false;
    }

    // emit a warning if the user added a method returning a JsonAdapter, but not of the right type
    Messager messager = context.processingEnvironment().getMessager();
    if (returnedJsonAdapter instanceof ParameterizedTypeName) {
      ParameterizedTypeName paramReturnType = (ParameterizedTypeName) returnedJsonAdapter;
      if (paramReturnType.typeArguments.get(0) instanceof ParameterizedTypeName) {
        return true;
      } else {
        TypeName argument = paramReturnType.typeArguments.get(0);
        messager.printMessage(Diagnostic.Kind.WARNING,
            String.format("Found public static method returning JsonAdapter<%s> on %s class. "
                + "Skipping MoshiJsonAdapter generation.", argument, type));
      }
    } else {
      messager.printMessage(Diagnostic.Kind.WARNING, "Found public static method returning "
          + "JsonAdapter with no type arguments, skipping MoshiJsonAdapter generation.");
    }

    return false;
  }

  @Override public String generateClass(Context context, String className, String classToExtend,
      boolean isFinal) {
    List<Property> properties = readProperties(context.properties());

    Map<String, TypeName> types = convertPropertiesToTypes(context.properties());

    List<? extends TypeParameterElement> typeParams = context.autoValueClass().getTypeParameters();
    boolean shouldCreateGenerics = typeParams != null && typeParams.size() > 0;

    ClassName classNameClass = ClassName.get(context.packageName(), className);
    ClassName autoValueClass = ClassName.get(context.autoValueClass());
    TypeName autoValueClassName = autoValueClass;
    TypeVariableName[] genericTypeNames = null;

    TypeName superclass;

    if (shouldCreateGenerics) {
      genericTypeNames = new TypeVariableName[typeParams.size()];
      for (int i = 0; i < typeParams.size(); i++) {
        genericTypeNames[i] = TypeVariableName.get(typeParams.get(i));
      }

      superclass = ParameterizedTypeName.get(ClassName.get(context.packageName(), classToExtend),
          (TypeName[]) genericTypeNames);
      autoValueClassName = ParameterizedTypeName.get(autoValueClass, (TypeName[]) genericTypeNames);
    } else {
      superclass = TypeVariableName.get(classToExtend);
    }

    TypeSpec typeAdapter =
        createTypeAdapter(classNameClass, autoValueClassName, genericTypeNames, properties);

    TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
        .superclass(superclass)
        .addType(typeAdapter)
        .addMethod(generateConstructor(types));

    if (shouldCreateGenerics) {
      subclass.addTypeVariables(Arrays.asList(genericTypeNames));
    }

    if (isFinal) {
      subclass.addModifiers(FINAL);
    } else {
      subclass.addModifiers(ABSTRACT);
    }

    return JavaFile.builder(context.packageName(), subclass.build()).build().toString();
  }

  public List<Property> readProperties(Map<String, ExecutableElement> properties) {
    List<Property> values = new LinkedList<>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      values.add(new Property(entry.getKey(), entry.getValue()));
    }
    return values;
  }

  ImmutableMap<Property, FieldSpec> createFields(List<Property> properties) {
    ImmutableMap.Builder<Property, FieldSpec> fields = ImmutableMap.builder();

    for (Property property : properties) {
      TypeName type = property.type.isPrimitive() ? property.type.box() : property.type;
      ParameterizedTypeName adp = ParameterizedTypeName.get(ADAPTER_CLASS_NAME, type);
      fields.put(property,
          FieldSpec.builder(adp, property.humanName + "Adapter", PRIVATE, FINAL).build());
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

  /** Converts the ExecutableElement properties to TypeName properties. */
  Map<String, TypeName> convertPropertiesToTypes(Map<String, ExecutableElement> properties) {
    Map<String, TypeName> types = new LinkedHashMap<>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      ExecutableElement el = entry.getValue();
      types.put(entry.getKey(), TypeName.get(el.getReturnType()));
    }
    return types;
  }

  public TypeSpec createTypeAdapter(ClassName className, TypeName autoValueClassName,
      TypeVariableName[] genericTypeNames, List<Property> properties) {
    TypeName typeAdapterClass = ParameterizedTypeName.get(ADAPTER_CLASS_NAME, autoValueClassName);

    ImmutableMap<Property, FieldSpec> adapters = createFields(properties);

    ParameterSpec moshi = ParameterSpec.builder(Moshi.class, "moshi").build();
    ParameterSpec type = null;

    MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameter(moshi);

    if (genericTypeNames != null) {
      type = ParameterSpec.builder(Type[].class, "types").build();
      constructor.addParameter(type);
    }

    boolean needsAdapterMethod = false;
    List<String> names = Lists.newArrayListWithCapacity(adapters.size());
    for (Map.Entry<Property, FieldSpec> entry : adapters.entrySet()) {
      Property prop = entry.getKey();
      FieldSpec field = entry.getValue();
      names.add(prop.serializedName());

      boolean usesJsonQualifier = false;
      for (AnnotationMirror annotationMirror : prop.element.getAnnotationMirrors()) {
        Element annotationType = annotationMirror.getAnnotationType().asElement();
        if (annotationType.getAnnotation(JsonQualifier.class) != null) {
          usesJsonQualifier = true;
          needsAdapterMethod = true;
        }
      }
      if (usesJsonQualifier) {
        constructor.addStatement("this.$N = adapter($N, \"$L\")", field, moshi, prop.methodName);
      } else if (genericTypeNames != null && prop.type instanceof ParameterizedTypeName) {
        ParameterizedTypeName typeName = ((ParameterizedTypeName) prop.type);
        constructor.addStatement("this.$N = $N.adapter($T.newParameterizedType($T.class, $N[$L]))",
            field, moshi, Types.class, typeName.rawType, type,
            getTypeIndexInArray(genericTypeNames, typeName.typeArguments.get(0)));
      } else if (genericTypeNames != null
          && getTypeIndexInArray(genericTypeNames, prop.type) >= 0) {
        constructor.addStatement("this.$N = $N.adapter($N[$L])", field, moshi, type,
            getTypeIndexInArray(genericTypeNames, prop.type));
      } else {
        constructor.addStatement("this.$N = $N.adapter($L)", field, moshi, makeType(prop.type));
      }
    }

    TypeSpec.Builder classBuilder = TypeSpec.classBuilder("MoshiJsonAdapter")
        .addModifiers(PUBLIC, STATIC, FINAL)
        .superclass(typeAdapterClass)
        .addFields(adapters.values())
        .addMethod(constructor.build())
        .addMethod(createReadMethod(className, autoValueClassName, adapters))
        .addMethod(createWriteMethod(autoValueClassName, adapters));

    ArrayTypeName stringArray = ArrayTypeName.of(String.class);
    ClassName optionsCN = ClassName.get(JsonReader.Options.class);
    String initializer = "{\"" + Joiner.on("\",\"").join(names) + "\"}";
    classBuilder
        .addField(FieldSpec.builder(stringArray, "NAMES", PRIVATE, STATIC, FINAL)
            .initializer(CodeBlock.of("new $T $L", stringArray, initializer))
            .build())
        .addField(FieldSpec.builder(optionsCN, "OPTIONS", PRIVATE, STATIC, FINAL)
            .initializer(CodeBlock.of("$T.of(NAMES)", optionsCN))
            .build());

    if (genericTypeNames != null) {
      classBuilder.addTypeVariables(Arrays.asList(genericTypeNames));
    }

    if (needsAdapterMethod) {
      classBuilder.addMethod(createAdapterMethod(autoValueClassName));
    }

    return classBuilder.build();
  }

  private int getTypeIndexInArray(TypeVariableName[] array, TypeName typeName) {
    return Arrays.binarySearch(array, typeName, new Comparator<TypeName>() {
      @Override
      public int compare(TypeName typeName, TypeName t1) {
        return typeName.equals(t1) ? 0 : -1;
      }
    });
  }

  private MethodSpec createAdapterMethod(TypeName autoValueClassName) {
    ParameterSpec moshi = ParameterSpec.builder(Moshi.class, "moshi").build();
    ParameterSpec methodName = ParameterSpec.builder(String.class, "methodName").build();
    return MethodSpec.methodBuilder("adapter")
        .addModifiers(PRIVATE)
        .addParameters(ImmutableSet.of(moshi, methodName))
        .returns(ADAPTER_CLASS_NAME)
        .addCode(CodeBlock.builder()
            .beginControlFlow("try")
            .addStatement("$T method = $T.class.getDeclaredMethod($N)",
                Method.class, autoValueClassName, methodName)
            .addStatement("$T<$T> annotations = new $T<>()",
                Set.class, Annotation.class, LinkedHashSet.class)
            .beginControlFlow("for ($T annotation : method.getAnnotations())", Annotation.class)
            .beginControlFlow("if (annotation.annotationType().isAnnotationPresent($T.class))",
                JsonQualifier.class)
            .addStatement("annotations.add(annotation)")
            .endControlFlow()
            .endControlFlow()
            .addStatement("return $N.adapter(method.getGenericReturnType(), annotations)", moshi)
            .nextControlFlow("catch ($T e)", NoSuchMethodException.class)
            .addStatement("throw new RuntimeException(\"No method named \" + $N, e)", methodName)
            .endControlFlow()
            .build()
        )
        .build();
  }

  public MethodSpec createWriteMethod(TypeName autoValueClassName,
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
        writeMethod.beginControlFlow("if ($N.$N() != null)", value, prop.methodName);
        writeMethod.addStatement("$N.name($S)", writer, prop.serializedName());
        writeMethod.addStatement("$N.toJson($N, $N.$N())", field, writer, value, prop.methodName);
        writeMethod.endControlFlow();
      } else {
        writeMethod.addStatement("$N.name($S)", writer, prop.serializedName());
        writeMethod.addStatement("$N.toJson($N, $N.$N())", field, writer, value, prop.methodName);
      }
    }
    writeMethod.addStatement("$N.endObject()", writer);

    return writeMethod.build();
  }

  public MethodSpec createReadMethod(ClassName className, TypeName autoValueClassName,
      ImmutableMap<Property, FieldSpec> adapters) {
    NameAllocator nameAllocator = new NameAllocator();
    ParameterSpec reader = ParameterSpec.builder(JsonReader.class, nameAllocator.newName("reader"))
        .build();
    MethodSpec.Builder readMethod = MethodSpec.methodBuilder("fromJson")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(autoValueClassName)
        .addParameter(reader)
        .addException(IOException.class);

    ClassName token = ClassName.get(JsonReader.Token.NULL.getClass());

    readMethod.addStatement("$N.beginObject()", reader);

    // add the properties
    Map<Property, FieldSpec> fields = new LinkedHashMap<>(adapters.size());
    for (Property prop : adapters.keySet()) {
      FieldSpec field = FieldSpec.builder(prop.type, nameAllocator.newName(prop.humanName)).build();
      fields.put(prop, field);

      readMethod.addStatement("$T $N = $L", field.type, field, defaultValue(field.type));
    }

    FieldSpec name = FieldSpec.builder(String.class, nameAllocator.newName("name")).build();
    readMethod.beginControlFlow("while ($N.hasNext())", reader);

    // Leverage the select API to avoid unnecessary string allocations
    readMethod.addStatement("int index = reader.selectName(OPTIONS)");
    readMethod.addStatement("$T $N", name.type, name);
    readMethod.beginControlFlow("if (index != -1)");

    // Found it, pull out of our known strings
    readMethod.addStatement("$N = NAMES[index]", name);
    readMethod.nextControlFlow("else");

    // Unrecognized type, skip it
    readMethod.addStatement("$N.nextName()", reader);
    readMethod.addStatement("$N.skipValue()", reader);
    readMethod.addStatement("continue");
    readMethod.endControlFlow();

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

  private String defaultValue(TypeName type) {
    if (type == TypeName.BOOLEAN) {
      return "false";
    } else if (type == TypeName.BYTE) {
      return "(byte) 0";
    } else if (type == TypeName.SHORT) {
      return "0";
    } else if (type == TypeName.INT) {
      return "0";
    } else if (type == TypeName.LONG) {
      return "0L";
    } else if (type == TypeName.CHAR) {
      return "'\0'";
    } else if (type == TypeName.FLOAT) {
      return "0.0f";
    } else if (type == TypeName.DOUBLE) {
      return "0.0d";
    } else {
      return "null";
    }
  }

  private CodeBlock makeType(TypeName type) {
    CodeBlock.Builder block = CodeBlock.builder();
    if (type instanceof ParameterizedTypeName) {
      ParameterizedTypeName pType = (ParameterizedTypeName) type;
      block.add("$T.newParameterizedType($T.class", Types.class, pType.rawType);
      for (TypeName typeArg : pType.typeArguments) {
        if (typeArg instanceof ParameterizedTypeName) {
          block.add(", $L", makeType(typeArg));
        } else {
          block.add(", $T.class", typeArg);
        }
      }
      block.add(")");
    } else {
      block.add("$T.class", type);
    }
    return block.build();
  }
}
