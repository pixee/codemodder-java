package com.ryandens.delegation;

import static com.google.auto.common.AnnotationMirrors.getAnnotationValue;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import javax.lang.model.util.Types;

/**
 * Annotation processor that generates abstract classes that delegate to an inner composed
 * implementation of an interface.
 */
@AutoService(Processor.class)
public final class AutoDelegateProcessor extends AbstractProcessor {

  private Filer filer;
  private Elements elementUtils;
  private Types typeUtils;

  @Override
  public synchronized void init(final ProcessingEnvironment processingEnv) {
    filer = processingEnv.getFiler();
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
  }

  @Override
  public boolean process(
      final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    final Set<? extends Element> autoDelegateElements =
        roundEnv.getElementsAnnotatedWith(AutoDelegate.class);
    //
    for (final Element element : autoDelegateElements) {
      // First, get an AnnotationMirror off the element annotated with AutoDelegate. This can be
      // thought of as an "instance" of an annotation
      final var annotationMirror =
          MoreElements.getAnnotationMirror(element, AutoDelegate.class)
              .toJavaUtil()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "element should always be annotated with AutoDelegate"));
      // From the AnnotationMirror, get the value of AutoDelegate#value and translate it
      // into an Element
      final var valueToDelegateField = getInterfaceToDelegateAsElement(annotationMirror);
      // From the AnnotationMirror, get the value of AutoDelegate#to and translate it
      // into a Set<Element>
      final var toDelegateSetField = getInterfacesToDelegateAsElementSet(annotationMirror);

      // validate that one and only one of those options was specified to avoid confusing behavior
      if (valueToDelegateField == null && toDelegateSetField.isEmpty()) {
        throw new IllegalArgumentException("A delegation target must be provided");
      } else if (valueToDelegateField != null && !toDelegateSetField.isEmpty()) {
        throw new IllegalArgumentException(
            "Only one mechanism of supplying delegation targets should be used");
      }

      // Since only one of valueToDelegateField/toDelegateSetField is not null or empty, if
      // valueToDelegateField is not null map it to a singleton List. If it is null, then use the
      // toDelegateSetField. Now we can proceed with our annotation processing without considering
      // whether we are delegating to one inner composed instance or multiple.
      final var apisToDelegate =
          valueToDelegateField != null ? List.of(valueToDelegateField) : toDelegateSetField;

      // from the Element annotated with AutoDelegate, get their declared interfaces. Find the
      // interface that is also specified as a delegation target via the AutoDelegate
      // annotation
      final var types =
          (((TypeElement) element)
              .getInterfaces().stream()
                  // also check supertypes
                  .flatMap(
                      declaredType ->
                          Stream.concat(
                              Stream.of(declaredType),
                              typeUtils.directSupertypes(declaredType).stream()
                                  .filter(
                                      typeMirror ->
                                          typeMirror.getKind().equals(TypeKind.DECLARED))))
                  .map(typeMirror -> (DeclaredType) typeMirror)
                  .filter(declaredType -> apisToDelegate.contains(declaredType.asElement()))
                  .collect(Collectors.toList()));

      // Validate that we found a DeclaredType from the declaring element's list of
      // interfaces for each type specified as a delegation target via the AutoDelegate annotation
      if (types.size() != apisToDelegate.size()) {
        throw new IllegalStateException(
            "A mismatch between the number of interfaces found on the declaring class that correspond to the delegation targets specified via the AutoDelegate annotation. Are you sure your type implements all of the delegation targets?");
      }

      // iterate through the List<DeclaredType> and build a new List of DelegationTargetDescriptor.
      // This is deliberately ordinal so that that we can consistently build a constructor with
      // parameters in the same declared order on each build. We also need to forge an association
      // between a DeclaredType and a String fieldName that will be used as the delegation target.
      // This mapping would be an otherwise good candidate for Stream#map, except that we want to
      // increment an index in order to guarantee unique field names for each type. It's generally
      // not recommended to mutate shared state in Stream#map so we opt to do it more idiomatically
      // in a for loop, collecting DelegationTargetDescriptors into a mutable LinkedList in the for
      // loop before wrapping it in an unmodifiableList implementation and de-referencing the
      // directly mutable LinkedList to prevent accidental mutations or usages
      var mutableDelegationTargetDescriptors = new LinkedList<DelegationTargetDescriptor>();
      for (int i = 0; i < types.size(); i++) {
        mutableDelegationTargetDescriptors.add(
            new DelegationTargetDescriptor(types.get(i), "inner" + i));
      }
      final var delegationTargetDescriptors =
          Collections.unmodifiableList(mutableDelegationTargetDescriptors);
      //noinspection UnusedAssignment
      mutableDelegationTargetDescriptors =
          null; // dereference to prevent accidental mutation or usage

      // Get the package of the element annotated with AutoDelegate, as the
      final var destinationPackageName =
          MoreElements.getPackage(element).getQualifiedName().toString();
      final var className = "AutoDelegate_" + element.getSimpleName();
      final var javaFile =
          new AutoDelegateGenerator(
                  elementUtils,
                  typeUtils,
                  destinationPackageName,
                  className,
                  delegationTargetDescriptors)
              .autoDelegate();
      try {
        // Write the JavaFile to the local environment
        javaFile.writeTo(filer);
      } catch (IOException e) {
        throw new UncheckedIOException(
            "Problem writing " + destinationPackageName + "." + className + " class to file", e);
      }
    }
    // always return false because we don't want to forbid other annotation processors from
    // operating on this type.
    return false;
  }

  @Override
  public final SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(AutoDelegate.class.getCanonicalName());
  }

  /** Returns the contents of {@link AutoDelegate#value()} as an {@link Element} */
  private Element getInterfaceToDelegateAsElement(AnnotationMirror annotationMirror) {
    return getAnnotationValue(annotationMirror, "value")
        .accept(
            new SimpleAnnotationValueVisitor8<Element, Void>() {
              @Override
              public Element visitType(TypeMirror typeMirror, Void v) {
                if (typeMirror.getKind().equals(TypeKind.VOID)) {
                  return null;
                } else {
                  return MoreTypes.asDeclared(typeMirror).asElement();
                }
              }
            },
            null);
  }

  /** Returns the contents of {@link AutoDelegate#value()} as an {@link Element} */
  private List<Element> getInterfacesToDelegateAsElementSet(AnnotationMirror annotationMirror) {
    return getAnnotationValue(annotationMirror, "to")
        .accept(
            new SimpleAnnotationValueVisitor8<List<Element>, Void>() {
              @Override
              public List<Element> visitType(TypeMirror typeMirror, Void v) {
                if (typeMirror.getKind().equals(TypeKind.VOID)) {
                  return null;
                } else {
                  return List.of(MoreTypes.asDeclared(typeMirror).asElement());
                }
              }

              @Override
              public List<Element> visitArray(
                  final List<? extends AnnotationValue> values, final Void unused) {
                return values.stream()
                    .flatMap(value -> value.accept(this, null).stream())
                    .collect(Collectors.toList());
              }
            },
            null);
  }
}
