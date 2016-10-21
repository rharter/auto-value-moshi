package com.ryanharter.auto.value.moshi;

import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
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
import java.lang.reflect.ParameterizedType;
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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * Created by rharter on 4/27/16.
 */
@AutoService(Processor.class)
public class AutoValueMoshiAdapterFactoryProcessor extends AbstractProcessor {

  private final AutoValueMoshiExtension extension = new AutoValueMoshiExtension();
  private Types typeUtils;
  private Elements elementUtils;

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(AutoValue.class.getName(), MoshiAdapterFactory.class.getName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
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
      Set<? extends Element> adaptorFactories = roundEnv.getElementsAnnotatedWith(MoshiAdapterFactory.class);
      for (Element element : adaptorFactories) {
        if (!element.getModifiers().contains(ABSTRACT)) {
          error(element, "Must be abstract!");
        }
        TypeElement type = (TypeElement) element; // Safe to cast because this is only applicable on types anyway
        if (!implementsJsonAdapterFactory(type)) {
          error(element, "Must implement JsonAdapter.Factory!");
        }
        String adapterName = classNameOf(type);
        String packageName = packageNameOf(type);
        TypeSpec jsonAdapterFactory = createJsonAdapterFactory(elements, packageName, adapterName);
        JavaFile file = JavaFile.builder(packageName, jsonAdapterFactory).build();
        try {
          file.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
          processingEnv.getMessager().printMessage(ERROR, "Failed to write TypeAdapterFactory: " + e.getLocalizedMessage());
        }
      }
    }

    return false;
  }

  private TypeSpec createJsonAdapterFactory(
      List<Element> elements,
      String packageName,
      String adapterName) {
    TypeSpec.Builder factory = TypeSpec.classBuilder(ClassName.get(packageName, "AutoValueMoshi_" + adapterName));
    factory.addModifiers(PUBLIC, FINAL);
    factory.superclass(ClassName.get(packageName, adapterName));

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

    CodeBlock.Builder classes = null;
    CodeBlock.Builder generics = null;

    int numGenerics = 0;
    int numClasses = 0;

    for (int i = 0; i < elements.size(); i++) {
      Element element = elements.get(i);
      TypeName elementTypeName = TypeName.get(element.asType());

      if (elementTypeName instanceof ParameterizedTypeName) {
        if (generics == null) {
          generics = CodeBlock.builder()
              .beginControlFlow("if ($N instanceof $T)", type, ParameterizedType.class)
              .addStatement("$T rawType = (($T) $N).getRawType()", Type.class, ParameterizedType.class, type);
        }

        addControlFlowGenric(generics, elementTypeName, moshi, type, element, numGenerics);
        numGenerics++;
      } else {
        if (classes == null) {
          classes = CodeBlock.builder();
        }

        addControlFlow(classes, CodeBlock.of("$N", type), elementTypeName, numClasses);
        numClasses++;

        ExecutableElement jsonAdapterMethod = getJsonAdapterMethod(element);
        classes.addStatement("return $T.$L($N)", element, jsonAdapterMethod.getSimpleName(), moshi);
      }

    }

    if (generics != null) {
      generics.endControlFlow();
      generics.addStatement("return null");
      generics.endControlFlow();
      create.addCode(generics.build());
    }

    if (classes != null) {
      classes.endControlFlow();
      create.addCode(classes.build());
    }

    create.addStatement("return null");
    factory.addMethod(create.build());
    return factory.build();
  }

  private void addControlFlowGenric(CodeBlock.Builder block, TypeName elementTypeName,
      ParameterSpec moshi, ParameterSpec type, Element element, int numGenerics) {
    TypeName typeName = ((ParameterizedTypeName) elementTypeName).rawType;
    CodeBlock typeBlock = CodeBlock.of("rawType");

    addControlFlow(block, typeBlock, typeName, numGenerics);

    ExecutableElement jsonAdapterMethod = getJsonAdapterMethod(element);
    if (jsonAdapterMethod.getParameters().size() > 1) {
      block.addStatement("return $L.$L($N, (($T) $N).getActualTypeArguments())",
          element.getSimpleName(), jsonAdapterMethod.getSimpleName(), moshi,
          ParameterizedType.class, type);
    }
  }

  private void addControlFlow(CodeBlock.Builder block, CodeBlock typeBlock,
      TypeName elementTypeName, int pos) {
    if (pos == 0) {
      block.beginControlFlow("if ($L.equals($T.class))", typeBlock, elementTypeName);
    } else {
      block.nextControlFlow("else if ($L.equals($T.class))", typeBlock, elementTypeName);
    }
  }

  private ExecutableElement getJsonAdapterMethod(Element element) {
    ParameterizedTypeName jsonAdapterType = ParameterizedTypeName.get(
        ClassName.get(JsonAdapter.class), TypeName.get(element.asType()));
    for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
      if (method.getModifiers().contains(Modifier.STATIC)
          && method.getModifiers().contains(Modifier.PUBLIC)) {
        TypeMirror rType = method.getReturnType();
        TypeName returnType = TypeName.get(rType);
        if (returnType.equals(jsonAdapterType) || returnType instanceof ParameterizedTypeName) {
          return method;
        }
      }
    }
    return null;
  }

  private void error(Element element, String message, Object... args) {
    if (args.length > 0) {
      message = String.format(message, args);
    }
    processingEnv.getMessager().printMessage(ERROR, message, element);
  }

  private boolean implementsJsonAdapterFactory(TypeElement type) {
    TypeMirror jsonAdapterType
        = elementUtils.getTypeElement(JsonAdapter.Factory.class.getCanonicalName()).asType();
    TypeMirror typeMirror = type.asType();
    if (!type.getInterfaces().isEmpty() || typeMirror.getKind() != TypeKind.NONE) {
      while (typeMirror.getKind() != TypeKind.NONE) {
        if (searchInterfacesAncestry(typeMirror, jsonAdapterType)) {
          return true;
        }
        type = (TypeElement) typeUtils.asElement(typeMirror);
        typeMirror = type.getSuperclass();
      }
    }
    return false;
  }

  private boolean searchInterfacesAncestry(TypeMirror rootIface, TypeMirror target) {
    TypeElement rootIfaceElement = (TypeElement) typeUtils.asElement(rootIface);
    // check if it implements valid interfaces
    for (TypeMirror iface : rootIfaceElement.getInterfaces()) {
      TypeElement ifaceElement = (TypeElement) typeUtils.asElement(rootIface);
      while (iface.getKind() != TypeKind.NONE) {
        if (typeUtils.isSameType(iface, target)) {
          return true;
        }
        // go up
        if (searchInterfacesAncestry(iface, target)) {
          return true;
        }
        // then move on
        iface = ifaceElement.getSuperclass();
      }
    }
    return false;
  }

  /**
   * Returns the name of the given type, including any enclosing types but not the package.
   */
  private static String classNameOf(TypeElement type) {
    String name = type.getQualifiedName().toString();
    String pkgName = packageNameOf(type);
    return pkgName.isEmpty() ? name : name.substring(pkgName.length() + 1);
  }

  /**
   * Returns the name of the package that the given type is in. If the type is in the default
   * (unnamed) package then the name is the empty string.
   */
  private static String packageNameOf(TypeElement type) {
    while (true) {
      Element enclosing = type.getEnclosingElement();
      if (enclosing instanceof PackageElement) {
        return ((PackageElement) enclosing).getQualifiedName().toString();
      }
      type = (TypeElement) enclosing;
    }
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

    @Override public Set<ExecutableElement> abstractMethods() {
      return null;
    }
  }
}
