package cn.mmd.houyi.processer;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
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
        //TODO 方法参数合法性校验
        generateActionFactory(methodElements);
//        generateBusImp();


        return false;
    }

    private void generateActionFactory(Set<ExecutableElement> methodElements) {
        HashMap<String, MethodSpec.Builder> methodSpecMap = generateChildMethod(methodElements);
        MethodSpec sendAction = generateActionSendMethod(methodSpecMap);
        TypeSpec.Builder builder = TypeSpec.classBuilder("ActionFactory")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(sendAction);
        for (String type : methodSpecMap.keySet()) {
            builder.addMethod(methodSpecMap.get(type).build());
        }
        TypeSpec actionFactory = builder.build();
        JavaFile javaFile = JavaFile.builder("cn.mmd.houyi.bus", actionFactory).build();
        try {
            javaFile.writeTo(mFiler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MethodSpec generateActionSendMethod(HashMap<String, MethodSpec.Builder> methodSpecMap) {
        Iterator<String> iterator = methodSpecMap.keySet().iterator();
        MethodSpec.Builder builder = MethodSpec.methodBuilder("sendAction")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addParameter(String.class, "type")
                .addParameter(Object[].class, "args");
        if (iterator.hasNext()) {
            String next = iterator.next();
            builder.beginControlFlow("if (type.equals($S))", next);
            builder.addStatement(next + "Action(args)");
        }
        while (iterator.hasNext()) {
            String next = iterator.next();
            builder.nextControlFlow("else if(type.equals($S))", next);
            builder.addStatement(next + "Action(args)");
        }
        return builder.endControlFlow().build();
    }

    private HashMap<String, MethodSpec.Builder> generateChildMethod(
            Set<ExecutableElement> methodElements) {
        HashMap<String, MethodSpec.Builder> methodSpecMap = new HashMap<>();
        for (ExecutableElement methodElement : methodElements) {
            TypeElement typeElement = (TypeElement) methodElement.getEnclosingElement();
            SubscribeStatic annotation = methodElement.getAnnotation(SubscribeStatic.class);
            String type = annotation.type();
            MethodSpec.Builder typeMethod;
            if ((typeMethod = methodSpecMap.get(type)) == null) {
                typeMethod = MethodSpec.methodBuilder(String.format("%sAction", type))
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .returns(void.class)
                        .addParameter(Object[].class, "args");
                methodSpecMap.put(type, typeMethod);
            }
            String params = generateMethodParams(methodElement);
            if ("".equals(params)) {
                typeMethod.addStatement("$N.$N()", typeElement.getQualifiedName(),
                        methodElement.getSimpleName());
            }else{
                typeMethod.addStatement("$N.$N("+params+")", typeElement.getQualifiedName(),
                        methodElement.getSimpleName());
            }
        }
        return methodSpecMap;
    }

    private String generateMethodParams(ExecutableElement methodElement) {
        List<? extends VariableElement> parameters = methodElement.getParameters();
        //生成方法参数String
        String params = "";
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement variableElement = parameters.get(i);
            TypeMirror typeMirror = variableElement.asType();
            if (typeMirror instanceof TypeVariable) {
                TypeVariable typeVariable = (TypeVariable) typeMirror;
                typeMirror = typeVariable.getUpperBound();
            }
            if (i == 0) {
                params = "(" + typeMirror.toString() + ")args[0]";
            } else {
                params += ", (" + typeMirror.toString() + ")args[" + i + "]";
            }
        }
        return params;
    }

    private void generateBusImp() {
        MethodSpec sendAction = MethodSpec.methodBuilder("sendAction")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addParameter(String.class, "type")
                .addParameter(Object[].class, "args")
                .addStatement("cn.mmd.houyi.bus.BusFactory.sendAction(type, args)")
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
        return SourceVersion.RELEASE_7;
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
