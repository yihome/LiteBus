package cn.mmd.houyi.processer;

import com.squareup.javapoet.MethodSpec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import cn.mmd.houyi.bus.annotation.SubscribeStatic;


/**
 * <p>write the description
 *
 * @author houyi
 * @version [版本号]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */

public class BusProcessor extends AbstractProcessor {

    private Filer mFiler;
    private Elements mElementUtils;
    private Types mTypeUtils;
    private Messager messager;

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.isEmpty()) {
            return false;
        }
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(
                SubscribeStatic.class);
        if (elements == null || elements.isEmpty()) {
            return true;
        }
        // 合法的TypeElement集合
        Set<ExecutableElement> methodElements = elements.stream()
                .filter((Predicate<Element>) this::validateElement)
                .map(element -> (ExecutableElement) element)
                .collect(Collectors.toSet());
        generateActionFactory(methodElements);
        generateBusImp();






        return false;
    }

    private void generateActionFactory(Set<ExecutableElement> methodElements) {
        for (ExecutableElement methodElement : methodElements) {
            SubscribeStatic annotation = methodElement.getAnnotation(SubscribeStatic.class);
            String type = annotation.type();
        }
    }

    private void generateBusImp() {
        MethodSpec sendAction = MethodSpec.methodBuilder("sendAction")
                .addModifiers(Modifier.PUBLIC,Modifier.STATIC)
                .returns(Void.class)
                .addParameter(String.class,"type")
                .addParameter(Object[].class,"args")
                .addStatement("cn.mmd.houyi.bus.BusFactory.")
                .build();
    }

    /**
     * Verify the annotated method.
     */
    private boolean validateElement(Element methodElement) {
        TypeElement typeElement = (TypeElement) methodElement.getEnclosingElement();
        Set<Modifier> typeModifiers = typeElement.getModifiers();
        // non-public class.
        if (!typeModifiers.contains(Modifier.PUBLIC)) {
            error(typeElement, "The class %s is not public.", typeElement.getQualifiedName());
            return false;
        }
        // abstract class.
        if (typeModifiers.contains(Modifier.ABSTRACT)) {
            error(typeElement,
                    "The method %s is abstract. You can't annotate abstract method with @%s.",
                    typeElement.getQualifiedName(), SubscribeStatic.class.getCanonicalName());
            return false;
        }

        Set<Modifier> modifiers = methodElement.getModifiers();
        // non-public method.
        if (!modifiers.contains(Modifier.PUBLIC)) {
            error(methodElement, "The method %s->%s is not public.", typeElement.getQualifiedName(),
                    methodElement.getSimpleName());
            return false;
        }
        if (!modifiers.contains(Modifier.STATIC)) {
            error(methodElement, "The method %s->%s is not static.", typeElement.getQualifiedName(),
                    methodElement.getSimpleName());
            return false;
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        LinkedHashSet<String> annotations = new LinkedHashSet<>();
        annotations.add(SubscribeStatic.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFiler = processingEnvironment.getFiler();
        mElementUtils = processingEnvironment.getElementUtils();
        mTypeUtils = processingEnvironment.getTypeUtils();
        messager = processingEnvironment.getMessager();
    }

    private void error(Element element, String message, Object... args) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(message, args), element);
    }

    private void debug(Element element, String message, Object... args) {
        messager.printMessage(Diagnostic.Kind.WARNING, String.format(message, args), element);
    }
}
