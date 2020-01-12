package com.ryanharter.auto.value.moshi;

import com.google.auto.common.GeneratedAnnotationSpecs;
import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.squareup.javapoet.AnnotationSpec;
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
import com.squareup.javapoet.WildcardTypeName;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonClass;
import com.squareup.moshi.JsonQualifier;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import io.sweers.autotransient.AutoTransient;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
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
public final class AutoValueMoshiExtension extends AutoValueExtension {
  private static final ClassName ADAPTER_CLASS_NAME = ClassName.get(JsonAdapter.class);
  private static final String MOSHI_GENERATOR_KEY = "avm";

  private static class Property {
    final String methodName;
    final String humanName;
    final ExecutableElement element;
    final TypeName type;
    final ImmutableSet<String> annotations;
    final boolean isTransient;
    final ImmutableSet<AnnotationMirror> jsonQualifiers;
    final boolean hasJsonQualifiers;

    @Nullable
    static Property create(Messager messager, String name, ExecutableElement element) {
      Property property = new Property(name, element);
      if (property.isTransient() && !property.nullable()) {
        messager.printMessage(Diagnostic.Kind.ERROR, "Required property cannot be transient!", element);
        return null;
      } else {
        return property;
      }
    }

    private Property(String name, ExecutableElement element) {
      this.methodName = element.getSimpleName().toString();
      this.humanName = name;
      this.element = element;

      type = TypeName.get(element.getReturnType());
      isTransient = element.getAnnotation(AutoTransient.class) != null;
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      ImmutableSet.Builder<AnnotationMirror> qualifiersBuilder = ImmutableSet.builder();
      for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
        builder.add(annotation.getAnnotationType().asElement().toString());
        //noinspection UnstableApiUsage
        if (MoreElements.isAnnotationPresent(annotation.getAnnotationType().asElement(), JsonQualifier.class)) {
          qualifiersBuilder.add(annotation);
        }
      }

      annotations = builder.build();
      jsonQualifiers = qualifiersBuilder.build();
      hasJsonQualifiers = !jsonQualifiers.isEmpty();
    }

    String serializedName() {
      Json json = element.getAnnotation(Json.class);
      if (json != null) {
        return json.name();
      } else {
        return humanName;
      }
    }

    boolean nullable() {
      return nullableAnnotation() != null;
    }

    boolean isTransient() {
      return isTransient;
    }

    public String nullableAnnotation() {
      for (String annotationString : annotations) {
        if (annotationString.equals("@Nullable") || annotationString.endsWith(".Nullable")) {
          return annotationString;
        }
      }
      return null;
    }
  }

  @Override
  public IncrementalExtensionType incrementalType(ProcessingEnvironment processingEnvironment) {
    return IncrementalExtensionType.ISOLATING;
  }

  @Override public boolean applicable(Context context) {
    TypeElement type = context.autoValueClass();
    if (generateExternalAdapter(type)) {
      return true;
    }
    // check that the class contains a static method returning a JsonAdapter
    ParameterizedTypeName jsonAdapterType = ParameterizedTypeName.get(
        ADAPTER_CLASS_NAME, TypeName.get(type.asType()));
    TypeName returnedJsonAdapter = null;
    for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
      if (method.getModifiers().contains(STATIC) && !method.getModifiers().contains(PRIVATE)) {
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
            String.format("Found static method returning JsonAdapter<%s> on %s class. "
                + "Skipping MoshiJsonAdapter generation.", argument, type));
      }
    } else {
      messager.printMessage(Diagnostic.Kind.WARNING, "Found static method returning "
          + "JsonAdapter with no type arguments, skipping MoshiJsonAdapter generation.");
    }

    return false;
  }

  private static boolean generateExternalAdapter(TypeElement element) {
    JsonClass jsonClass = element.getAnnotation(JsonClass.class);
    return jsonClass != null && jsonClass.generateAdapter() && MOSHI_GENERATOR_KEY.equals(jsonClass.generator());
  }

  @Override public String generateClass(Context context, String className, String classToExtend,
      boolean isFinal) {
    List<Property> properties = readProperties(
        context.processingEnvironment().getMessager(),
        context.properties());

    List<? extends TypeParameterElement> typeParams = context.autoValueClass().getTypeParameters();
    boolean shouldCreateGenerics = typeParams != null && typeParams.size() > 0;

    ClassName classNameClass = ClassName.get(context.packageName(), className);
    ClassName autoValueClassName = ClassName.get(context.autoValueClass());
    TypeVariableName[] genericTypeNames = null;

    TypeName superclass;

    if (shouldCreateGenerics) {
      genericTypeNames = new TypeVariableName[typeParams.size()];
      for (int i = 0; i < typeParams.size(); i++) {
        genericTypeNames[i] = TypeVariableName.get(typeParams.get(i));
      }
      superclass = ParameterizedTypeName.get(ClassName.get(context.packageName(), classToExtend),
              genericTypeNames);
    } else {
      superclass = TypeVariableName.get(classToExtend);
    }

    boolean generateExternalAdapter = generateExternalAdapter(context.autoValueClass());

    String adapterClassName = generateExternalAdapter
        ? Types.generatedJsonAdapterName(Joiner.on("$").join(autoValueClassName.simpleNames()))
        : "MoshiJsonAdapter";

    TypeSpec.Builder typeAdapterBuilder = createTypeAdapter(classNameClass,
        autoValueClassName,
        genericTypeNames,
        properties,
        context.builder().orElse(null),
        context.processingEnvironment(),
        adapterClassName);

    Optional<AnnotationSpec> generatedAnnotation = GeneratedAnnotationSpecs.generatedAnnotationSpec(
        context.processingEnvironment().getElementUtils(),
        context.processingEnvironment().getSourceVersion(),
        AutoValueMoshiExtension.class
    );

    if (generateExternalAdapter(context.autoValueClass())) {
      typeAdapterBuilder.addOriginatingElement(context.autoValueClass());
      generatedAnnotation.ifPresent(typeAdapterBuilder::addAnnotation);
      JavaFile javaFile = JavaFile.builder(context.packageName(), typeAdapterBuilder.build())
          .skipJavaLangImports(true)
          .build();
      try {
        javaFile.writeTo(context.processingEnvironment().getFiler());
      } catch (IOException e) {
        context.processingEnvironment().getMessager()
            .printMessage(Diagnostic.Kind.ERROR,
                String.format(
                    "Failed to write external TypeAdapter for element \"%s\" with reason \"%s\"",
                    context.autoValueClass(),
                    e.getMessage()));
      }
      return null;
    } else {
      TypeSpec typeAdapter = typeAdapterBuilder.addModifiers(STATIC).build();

      TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
          .superclass(superclass)
          .addType(typeAdapter)
          .addMethod(generateConstructor(properties));

      generatedAnnotation.ifPresent(subclass::addAnnotation);

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
  }

  private List<Property> readProperties(
      Messager messager,
      Map<String, ExecutableElement> properties) {
    List<Property> values = new LinkedList<>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      Property prop = Property.create(messager, entry.getKey(), entry.getValue());
      if (prop != null) {
        values.add(prop);
      }
    }
    return values;
  }

  private ImmutableMap<Property, FieldSpec> createFields(List<Property> properties) {
    ImmutableMap.Builder<Property, FieldSpec> fields = ImmutableMap.builder();

    for (Property property : properties) {
      if (property.isTransient()) {
        continue;
      }
      TypeName type = property.type.isPrimitive() ? property.type.box() : property.type;
      ParameterizedTypeName adp = ParameterizedTypeName.get(ADAPTER_CLASS_NAME, type);
      FieldSpec.Builder builder
          = FieldSpec.builder(adp, property.humanName + "Adapter", PRIVATE, FINAL);

      // Bolt qualifier annotations onto the adapter field they're used for. We'll look these up
      // at runtime.
      if (property.hasJsonQualifiers) {
        for (AnnotationMirror qualifier : property.jsonQualifiers) {
          builder.addAnnotation(AnnotationSpec.get(qualifier));
        }
      }

      fields.put(property, builder.build());
    }

    return fields.build();
  }

  private MethodSpec generateConstructor(List<Property> properties) {
    List<ParameterSpec> params = Lists.newArrayListWithCapacity(properties.size());
    for (Property property : properties) {
      ParameterSpec.Builder builder = ParameterSpec.builder(property.type, property.humanName);
      if (property.nullable()) {
        builder.addAnnotation(ClassName.bestGuess(property.nullableAnnotation()));
      }
      params.add(builder.build());
    }

    MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addParameters(params);

    StringBuilder superFormat = new StringBuilder("super(");
    List<ParameterSpec> args = new ArrayList<>();
    for (int i = 0, n = params.size(); i < n; i++) {
      args.add(params.get(i));
      superFormat.append("$N");
      if (i < n - 1) superFormat.append(", ");
    }
    superFormat.append(")");
    builder.addStatement(superFormat.toString(), args.toArray());

    return builder.build();
  }

  private TypeSpec.Builder createTypeAdapter(
      ClassName className,
      ClassName autoValueClassName,
      TypeVariableName[] genericTypeNames,
      List<Property> properties,
      @Nullable BuilderContext builderContext,
      ProcessingEnvironment processingEnvironment,
      String adapterClassName
  ) {

    final TypeName autoValueTypeName = genericTypeNames != null && genericTypeNames.length > 0
            ? ParameterizedTypeName.get(autoValueClassName, genericTypeNames)
            : autoValueClassName;

    ImmutableMap<Property, FieldSpec> adapters = createFields(properties);

    ParameterSpec moshiInstance = ParameterSpec.builder(Moshi.class, "moshi").build();
    ParameterSpec typesArray = null;

    MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameter(moshiInstance);

    if (genericTypeNames != null) {
      typesArray = ParameterSpec.builder(Type[].class, "types").build();
      constructor.addParameter(typesArray);
    }

    List<String> names = Lists.newArrayListWithCapacity(adapters.size());
    for (Map.Entry<Property, FieldSpec> entry : adapters.entrySet()) {
      Property prop = entry.getKey();
      FieldSpec moshiField = entry.getValue();
      names.add(prop.serializedName());

      CodeBlock possibleQualifierLookup = prop.hasJsonQualifiers
          ? CodeBlock.of(", $T.getFieldJsonQualifierAnnotations(getClass(), $S)", Types.class, moshiField.name)
          : CodeBlock.of("");

      // if the property is @Nullable, we append a .nullSafe() to the adapter
      String nullableOrNothing = prop.nullable() ? ".nullSafe()" : "";
      if (genericTypeNames != null && prop.type instanceof ParameterizedTypeName) {
        // Property is a parameterized type that may or may not use generics (like "List<T>" or
        // "List<String>"
        ParameterizedTypeName typeName = ((ParameterizedTypeName) prop.type);
        CodeBlock adapterTargetType = makeType(typeName, typesArray, genericTypeNames);
        constructor.addStatement("this.$N = $N.<$T>adapter($L$L)$L",
                moshiField,
                moshiInstance,
                typeName,
                adapterTargetType,
                possibleQualifierLookup,
                nullableOrNothing);
      } else if (genericTypeNames != null
          && getTypeIndexInArray(genericTypeNames, prop.type) >= 0) {
        // Property is a simple generic type (like "T"). Resolve the type at runtime through the
        // types array passed through the constructor
        constructor.addStatement("this.$N = $N.<$T>adapter($N[$L]$L)$L",
            moshiField,
            moshiInstance,
            prop.type,
            typesArray,
            getTypeIndexInArray(genericTypeNames, prop.type),
            possibleQualifierLookup,
            nullableOrNothing);
      } else {
        // Normal property
        CodeBlock possibleGenerics = prop.type instanceof ParameterizedTypeName
            ? CodeBlock.of("<$T>", prop.type)
            : CodeBlock.of("");
        constructor.addStatement("this.$N = $N.$Ladapter($L$L)$L",
            moshiField,
            moshiInstance,
            possibleGenerics,
            makeType(prop.type, typesArray, genericTypeNames),
            possibleQualifierLookup,
            nullableOrNothing);
      }
    }

    ClassName jsonAdapterClassName = ClassName.get(JsonAdapter.class);
    ParameterizedTypeName superClass = ParameterizedTypeName.get(jsonAdapterClassName, autoValueTypeName);
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(adapterClassName)
        .addModifiers(PUBLIC, FINAL)
        .superclass(superClass)
        .addFields(adapters.values())
        .addMethod(constructor.build())
        .addMethod(createReadMethod(className, autoValueClassName, autoValueTypeName, properties,
                adapters, names, builderContext, processingEnvironment))
        .addMethod(createWriteMethod(autoValueTypeName, adapters));

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

    return classBuilder;
  }

  private int getTypeIndexInArray(TypeVariableName[] array, TypeName typeName) {
    for (int i = 0; i < array.length ; i++) {
      if (typeName.equals(array[i])) {
        return i;
      }
    }
    return -1;
  }

  private MethodSpec createWriteMethod(TypeName autoValueTypeName,
      ImmutableMap<Property, FieldSpec> adapters) {
    String writerName = "writer";
    String valueName = "value";
    ParameterSpec writer = ParameterSpec.builder(JsonWriter.class, writerName).build();
    ParameterSpec value = ParameterSpec.builder(autoValueTypeName, valueName).build();
    MethodSpec.Builder writeMethod = MethodSpec.methodBuilder("toJson")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .addParameter(writer)
        .addParameter(value)
        .addException(IOException.class);

    writeMethod.addStatement("$N.beginObject()", writer);

    NameAllocator nameAllocator = new NameAllocator();
    nameAllocator.newName(writerName);
    nameAllocator.newName(valueName);
    for (Map.Entry<Property, FieldSpec> entry : adapters.entrySet()) {
      Property prop = entry.getKey();
      if (prop.isTransient()) {
        continue;
      }
      FieldSpec field = entry.getValue();
      nameAllocator.newName(prop.humanName, prop);

      if (prop.nullable()) {
        String name = nameAllocator.get(prop);
        writeMethod.addStatement("$T $N = $N.$N()", prop.type, name, value, prop.methodName);
        writeMethod.beginControlFlow("if ($N != null)", name);
        writeMethod.addStatement("$N.name($S)", writer, prop.serializedName());
        writeMethod.addStatement("this.$N.toJson($N, $N)", field, writer, name);
        writeMethod.endControlFlow();
      } else {
        writeMethod.addStatement("$N.name($S)", writer, prop.serializedName());
        writeMethod.addStatement("this.$N.toJson($N, $N.$N())", field, writer, value,
            prop.methodName);
      }
    }
    writeMethod.addStatement("$N.endObject()", writer);

    return writeMethod.build();
  }

  private MethodSpec createReadMethod(ClassName className, ClassName autoValueClassName, TypeName autoValueTypeName,
                                      List<Property> properties, ImmutableMap<Property, FieldSpec> adapters,
                                      List<String> names, @Nullable BuilderContext builderContext,
                                      ProcessingEnvironment processingEnvironment) {
    NameAllocator nameAllocator = new NameAllocator();
    ParameterSpec reader = ParameterSpec.builder(JsonReader.class, nameAllocator.newName("reader"))
        .build();
    MethodSpec.Builder readMethod = MethodSpec.methodBuilder("fromJson")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(autoValueTypeName)
        .addParameter(reader)
        .addException(IOException.class);
    // Validate the builderContext if there is one.
    if (builderContext != null) {
      if (!builderContext.buildMethod().isPresent()) {
        processingEnvironment.getMessager()
                .printMessage(
                        Diagnostic.Kind.ERROR,
                        "Could not determine the build method. Make sure it is named \"build\".",
                        builderContext.builderType());
        return readMethod.build();
      }

      Set<ExecutableElement> builderMethods = builderContext.builderMethods();

      if (builderMethods.size() > 1) {
        Set<ExecutableElement> annotatedMethods = builderMethods.stream()
                .filter(e -> MoreElements.isAnnotationPresent(e, AutoValueMoshiBuilder.class))
                .collect(Collectors.toSet());

        if (annotatedMethods.size() > 1) {
          processingEnvironment.getMessager()
                  .printMessage(
                          Diagnostic.Kind.ERROR,
                          "Too many @AutoValueMoshiBuilder annotated builder methods.",
                          annotatedMethods.stream().findAny().get()
                  );
          return readMethod.build();
        }

        if (annotatedMethods.isEmpty()) {
          processingEnvironment.getMessager().printMessage(
                  Diagnostic.Kind.ERROR,
                  "Too many builder methods. Annotate builder method with @AutoValueMoshiBuilder.",
                  builderMethods.stream().findAny().get()
          );
          return readMethod.build();
        }
      }
    }

    readMethod.addStatement("$N.beginObject()", reader);

    // Will be empty if using a AutoValue builder
    Map<Property, FieldSpec> fields = new LinkedHashMap<>(properties.size());
    List<CodeBlock> constructorCall = Lists.newArrayListWithExpectedSize(properties.size());
    // Will be absent if not using AutoValue builder
    Optional<FieldSpec> builderField = Optional.ofNullable(builderContext)
            .map(ctx -> FieldSpec
                    .builder(TypeName.get(ctx.builderType().asType()), "builder")
                    .build());

    if (builderField.isPresent()) {
      Set<ExecutableElement> builderMethods = builderContext.builderMethods();

      if (builderMethods.size() == 0) {
        // If no builder method defined, instantiate directly.
        readMethod.addStatement("$T $N = new $T.$L()", builderField.get().type, builderField.get(),
                className, builderContext.builderType().getSimpleName());
      } else {
        ExecutableElement builderMethod;
        if (builderMethods.size() == 1) {
          // If there is only 1, use it.
          builderMethod = builderMethods.stream().findFirst().get();
        } else {
          // Otherwise, find the only builder method that is annotated.
          Set<ExecutableElement> annotatedMethods = builderMethods.stream()
                  .filter(e -> MoreElements.isAnnotationPresent(e, AutoValueMoshiBuilder.class))
                  .collect(Collectors.toSet());

          if (annotatedMethods.size() == 1) {
            builderMethod = annotatedMethods.stream().findFirst().get();
          } else {
            throw new IllegalStateException();
          }
        }

        readMethod.addStatement("$T $N = $T.$N()", builderField.get().type, builderField.get(),
                autoValueClassName, builderMethod.getSimpleName());
      }
    } else {
      // add the properties
      for (Property prop : adapters.keySet()) {
        FieldSpec field = FieldSpec.builder(prop.type, nameAllocator.newName(prop.humanName)).build();
        fields.put(prop, field);

        readMethod.addStatement("$T $N = $L", field.type, field, defaultValue(field.type));
      }
    }

    readMethod.beginControlFlow("while ($N.hasNext())", reader);

    // Leverage the select API for better perf
    readMethod.beginControlFlow("switch ($N.selectName(OPTIONS))", reader);
    for (Property property : properties) {
      if (property.isTransient()) {
        constructorCall.add(CodeBlock.of("null"));
        continue;
      }
      CodeBlock.Builder block = CodeBlock.builder();

      FieldSpec adapter = adapters.get(property);
      readMethod.beginControlFlow("case $L:", names.indexOf(property.serializedName()));
      if (builderField.isPresent()) {
        addBuilderFieldSetting(block, property, adapter, reader, builderField.get(), builderContext);
      } else {
        FieldSpec localField = fields.get(property);
        constructorCall.add(CodeBlock.of("$N", localField));
        addFieldSetting(block, localField, adapter, reader);
      }
      readMethod.addCode(block.build());
      readMethod.addStatement("break");
      readMethod.endControlFlow();
    }

    // skip value if field is not serialized...
    readMethod.beginControlFlow("case -1:");
    readMethod.addCode("// Unknown name, skip it\n");
    readMethod.addStatement("$N.skipName()", reader);
    readMethod.addStatement("$N.skipValue()", reader);
    readMethod.endControlFlow();

    readMethod.endControlFlow(); // switch
    readMethod.endControlFlow(); // while

    readMethod.addStatement("$N.endObject()", reader);
    if (builderField.isPresent()) {
      readMethod.addStatement("return $N.$L", builderField.get(), builderContext.buildMethod().get());
    } else {
      CodeBlock params = CodeBlock.join(constructorCall, ", ");
      readMethod.addStatement("return new $T($L)", className, params);
    }
    return readMethod.build();
  }

  private void addFieldSetting(CodeBlock.Builder block, FieldSpec field, FieldSpec adapter, ParameterSpec reader) {
    block.addStatement("$N = this.$N.fromJson($N)", field, adapter, reader);
  }

  private static void addBuilderFieldSetting(CodeBlock.Builder block,
                                             Property prop,
                                             FieldSpec adapter,
                                             ParameterSpec jsonReader,
                                             FieldSpec builder,
                                             BuilderContext builderContext) {
    Stream<MethodSpec> setterMethodSpecs = builderContext.setters().get(prop.humanName).stream()
            .map(setterMethod -> MethodSpec.overriding(setterMethod).build());

    // If setter param type matches field type
    Optional<MethodSpec> setter = setterMethodSpecs
            // Find setter with param type equal to field type.
            .filter(methodSpec -> methodSpec.parameters.get(0).type.equals(prop.type))
            .findFirst();

    if (setter.isPresent()) {
      block.addStatement("$N.$N($N.fromJson($N))", builder, setter.get(), adapter, jsonReader);
    } else {
      // Optional fields are not supported.
      String errorMsg =
              "Setter not found for " + prop.element;
      throw new IllegalArgumentException(errorMsg);
    }
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

  // typesArray and genericTypeNames only need to be non-null when using generic types
  private CodeBlock makeType(TypeName type, ParameterSpec typesArray,
      TypeVariableName[] genericTypeNames) {

    CodeBlock.Builder block = CodeBlock.builder();
    if (type instanceof ParameterizedTypeName) {
      ParameterizedTypeName pType = (ParameterizedTypeName) type;
      block.add("$T.newParameterizedType($T.class", Types.class, pType.rawType);
      for (TypeName typeArg : pType.typeArguments) {
        if (typeArg instanceof ParameterizedTypeName) {
          block.add(", $L", makeType(typeArg, typesArray, genericTypeNames));
        } else if (typeArg instanceof WildcardTypeName) {
          WildcardTypeName wildcard = (WildcardTypeName) typeArg;
          TypeName target;
          String method;
          if (wildcard.lowerBounds.size() == 1) {
            target = wildcard.lowerBounds.get(0);
            method = "supertypeOf";
          } else if (wildcard.upperBounds.size() == 1) {
            target = wildcard.upperBounds.get(0);
            method = "subtypeOf";
          } else {
            throw new IllegalArgumentException(
                "Unrepresentable wildcard type. Cannot have more than one bound: " + wildcard);
          }
          block.add(", $T.$L($T.class)", Types.class, method, target);
        } else if (typeArg instanceof TypeVariableName) {
          TypeVariableName genericType = (TypeVariableName) typeArg;
          block.add(", $N[$L]", typesArray, getTypeIndexInArray(genericTypeNames, genericType));
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
