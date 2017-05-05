package cn.mmd.houyi.processer;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

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
        Set<ExecutableElement> methodElements = new HashSet<>();
        for (Element element : elements) {
            TypeElement typeElement = (TypeElement) element.getEnclosingElement();
            if (validateElement(element)) {
                methodElements.add((ExecutableElement) element);
            }
        }
        return false;
    }

    /**
     * Verify the annotated class.
     */
    private boolean validateElement(Element methodElement) {
        Set<Modifier> modifiers = methodElement.getModifiers();
        // non-public class.
        if (!modifiers.contains(Modifier.PUBLIC)) {
            error(methodElement, "The method %s is not public.", methodElement.getSimpleName());
            return false;
        }
        if (!modifiers.contains(Modifier.STATIC)) {
            error(methodElement, "The method %s is not static.", methodElement.getSimpleName());
            return false;
        }
        // abstract class.
        if (modifiers.contains(Modifier.ABSTRACT)) {
            error(methodElement,
                    "The method %s is abstract. You can't annotate abstract method with @%s.",
                    methodElement.getSimpleName(), SubscribeStatic.class.getSimpleName());
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
