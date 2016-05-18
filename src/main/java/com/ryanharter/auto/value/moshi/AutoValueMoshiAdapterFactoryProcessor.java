package com.ryanharter.auto.value.moshi;

import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Created by rharter on 4/27/16.
 */
@AutoService(Processor.class)
public class AutoValueMoshiAdapterFactoryProcessor extends AbstractProcessor {

  private final AutoValueMoshiExtension extension = new AutoValueMoshiExtension();

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(AutoValue.class.getName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    List<Element> elements = new LinkedList<Element>();
    for (Element element : roundEnv.getElementsAnnotatedWith(AutoValue.class)) {
      AutoValueExtension.Context context = new LimitedContext(processingEnv, (TypeElement) element);
      if (extension.applicable(context)) {
        elements.add(element);
      }
    }

    if (!elements.isEmpty()) {
      TypeSpec jsonAdapterFactory = createJsonAdapterFactory(elements);
      JavaFile file = JavaFile.builder("com.ryanharter.auto.value.moshi", jsonAdapterFactory).build();
      try {
        file.writeTo(processingEnv.getFiler());
      } catch (IOException e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write TypeAdapterFactory: " + e.getLocalizedMessage());
      }
    }

    return false;
  }

  private TypeSpec createJsonAdapterFactory(List<Element> elements) {
    TypeSpec.Builder factory = TypeSpec.classBuilder(ClassName.get("com.ryanharter.auto.value.moshi", "AutoValueMoshiAdapterFactory"));
    factory.addModifiers(PUBLIC, FINAL);
    factory.addSuperinterface(TypeName.get(JsonAdapter.Factory.class));

    ParameterSpec type = ParameterSpec.builder(Type.class, "type").build();
    WildcardTypeName extendsAnnotation = WildcardTypeName.subtypeOf(Annotation.class);
    ParameterSpec annotations = ParameterSpec
        .builder(ParameterizedTypeName.get(ClassName.get(Set.class), extendsAnnotation), "annotations")
        .build();
    ParameterSpec moshi = ParameterSpec.builder(Moshi.class, "moshi").build();
    ParameterizedTypeName result = ParameterizedTypeName.get(ClassName.get(JsonAdapter.class),
        WildcardTypeName.subtypeOf(TypeName.OBJECT));
    MethodSpec.Builder create = MethodSpec.methodBuilder("create")
        .addModifiers(PUBLIC)
        .addAnnotation(Override.class)
        .addParameters(ImmutableSet.of(type, annotations, moshi))
        .returns(result);

    create.addStatement("Class<?> rawType = com.squareup.moshi.Types.getRawType(type);");
    for (int i = 0; i < elements.size(); i++) {
      Element element = elements.get(i);
      if (i == 0) {
        create.beginControlFlow("if ($T.class.isAssignableFrom($N.getClass()))", element, type);
      } else {
        create.nextControlFlow("else if ($T.class.isAssignableFrom($N.getClass()))", element, type);
      }
      ExecutableElement jsonAdapterMethod = getJsonAdapterMethod(element);
      create.addStatement("return $T." + jsonAdapterMethod.getSimpleName() + "($N)", element, moshi);
    }
    create.nextControlFlow("else");
    create.addStatement("return null");
    create.endControlFlow();

    factory.addMethod(create.build());
    return factory.build();
  }

  private ExecutableElement getJsonAdapterMethod(Element element) {
    ParameterizedTypeName jsonAdapterType = ParameterizedTypeName.get(
        ClassName.get(JsonAdapter.class), TypeName.get(element.asType()));
    for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
      if (method.getModifiers().contains(Modifier.STATIC)
          && method.getModifiers().contains(Modifier.PUBLIC)) {
        TypeMirror rType = method.getReturnType();
        TypeName returnType = TypeName.get(rType);
        if (returnType.equals(jsonAdapterType)) {
          return method;
        }
      }
    }
    return null;
  }

  private static class LimitedContext implements AutoValueExtension.Context {

    private final ProcessingEnvironment processingEnvironment;
    private final TypeElement autoValueClass;

    private LimitedContext(ProcessingEnvironment processingEnvironment, TypeElement autoValueClass) {
      this.processingEnvironment = processingEnvironment;
      this.autoValueClass = autoValueClass;
    }

    @Override public ProcessingEnvironment processingEnvironment() {
      return processingEnvironment;
    }

    @Override public String packageName() {
      String fqn = autoValueClass.getQualifiedName().toString();
      return fqn.substring(0, fqn.lastIndexOf('.'));
    }

    @Override public TypeElement autoValueClass() {
      return autoValueClass;
    }

    @Override public Map<String, ExecutableElement> properties() {
      return null;
    }
  }
}
