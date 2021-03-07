package org.dragon.zhang;

import com.github.therapi.runtimejavadoc.ClassJavadoc;
import com.github.therapi.runtimejavadoc.Comment;
import com.github.therapi.runtimejavadoc.CommentFormatter;
import com.github.therapi.runtimejavadoc.MethodJavadoc;
import com.github.therapi.runtimejavadoc.OtherJavadoc;
import com.github.therapi.runtimejavadoc.ParamJavadoc;
import com.github.therapi.runtimejavadoc.RuntimeJavadoc;
import com.github.therapi.runtimejavadoc.ThrowsJavadoc;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.MemberAttributeExtension;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhangzicheng
 * @date 2021/03/07
 */
public abstract class JavadocAnnotator implements BeanFactoryAware {

    protected static final Logger log = LoggerFactory.getLogger(JavadocAnnotator.class);

    protected static final CommentFormatter FORMATTER = new CommentFormatter();

    protected static final String DESCRIPTION_KEY = "description";
    protected static final String RETURN_KEY = "return";
    protected static final String PARAM_KEY_PRE = "param-";
    protected static final String PARAM_NAME_KEY = "paramName";
    protected static final String PARAM_DESCRIPTION_KEY = "paramDescription";

    /**
     * 打了什么注解的类需要修改字节码
     */
    protected Set<Class<? extends Annotation>> typeMarks;

    /**
     * 打了什么注解的方法需要修改字节码
     */
    protected Set<Class<? extends Annotation>> methodMarks;

    /**
     * 匹配的类需要打什么注解
     */
    protected Set<JavadocMapping<? extends Annotation>> tagClass;

    /**
     * 匹配的方法需要打什么注解
     */
    protected Set<JavadocMapping<? extends Annotation>> tagMethod;

    /**
     * 匹配的方法需要打什么注解
     */
    protected Set<JavadocMapping<? extends Annotation>> tagParameter;

    public JavadocAnnotator() {
        this.typeMarks = buildTypeMarks();
        this.methodMarks = buildMethodMarks();
        this.tagClass = buildTagClass();
        this.tagMethod = buildTagMethod();
        this.tagParameter = buildTagParameter();
    }

    protected abstract Set<JavadocMapping<? extends Annotation>> buildTagParameter();

    protected abstract Set<JavadocMapping<? extends Annotation>> buildTagMethod();

    protected abstract Set<JavadocMapping<? extends Annotation>> buildTagClass();

    protected abstract Set<Class<? extends Annotation>> buildMethodMarks();

    protected abstract Set<Class<? extends Annotation>> buildTypeMarks();

    private static String format(Comment c) {
        return FORMATTER.format(c);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (null == typeMarks || typeMarks.isEmpty()) {
            throw new BeanCreationException("illegal types");
        }
        boolean tagClassEmpty = (null == tagClass || tagClass.isEmpty());
        boolean tagMethodEmpty = (null == tagMethod || tagMethod.isEmpty());
        if (tagClassEmpty && tagMethodEmpty) {
            throw new IllegalArgumentException("they cannot be empty at the same time");
        }
        ListableBeanFactory listableBeanFactory = (ListableBeanFactory) beanFactory;
        Set<Object> beans = new HashSet<>();
        //这里已经过滤掉了不需要的类
        for (Class<? extends Annotation> type : typeMarks) {
            beans.addAll(listableBeanFactory.getBeansWithAnnotation(type).values());
        }
        if (beans.isEmpty()) {
            log.warn("there is no bean of the types");
            return;
        }
        //开始修改字节码
        ByteBuddyAgent.install();
        for (Object bean : beans) {
            Class<?> beanClass = bean.getClass();
            ClassJavadoc classDoc = RuntimeJavadoc.getJavadoc(beanClass.getName());
            DynamicType.Builder<?> builder = new ByteBuddy()
                    .redefine(beanClass);
            if (!tagClassEmpty) {
                //初始化注释
                Map<String, String> commentMap = classDoc.getOther()
                        .stream().collect(Collectors.toMap(
                                OtherJavadoc::getName, data -> format(data.getComment())));
                commentMap.put(DESCRIPTION_KEY, format(classDoc.getComment()));
                Set<JavadocMapping<? extends Annotation>> tagClass = getAnnotationsNeedToTag(beanClass, this.tagClass);
                for (JavadocMapping<? extends Annotation> mapping : tagClass) {
                    Class<? extends Annotation> annotationType = mapping.annotationType();
                    AnnotationDescription.Builder annotationBuilder = AnnotationDescription.Builder
                            .ofType(annotationType);
                    for (Method memberMethod : annotationType.getDeclaredMethods()) {
                        String methodName = memberMethod.getName();
                        try {
                            //优先取用户指定的默认值
                            Object value = memberMethod.invoke(mapping.getAnnotation());
                            if (null == value) {
                                //注释的key
                                String tagKey = mapping.getCommentKey(methodName);
                                if (null != tagKey) {
                                    value = commentMap.get(tagKey);
                                }
                            }
                            if (null == value) {
                                //没有映射，取注解定义的默认值
                                value = memberMethod.getDefaultValue();
                            }
                            Class<?> returnType = memberMethod.getReturnType();
                            if (returnType.isArray()) {
                                Class<Annotation> componentType = (Class<Annotation>) returnType.getComponentType();
                                if (value instanceof Annotation[]) {
                                    annotationBuilder = annotationBuilder.defineAnnotationArray(methodName, componentType, (Annotation[]) value);
                                } else if (value instanceof Class<?>[]) {
                                    annotationBuilder = annotationBuilder.defineTypeArray(methodName, (Class<?>[]) value);
                                } else if (value instanceof Enum<?>[]) {
                                    Set<String> enums = new HashSet<>();
                                    for (Enum<?> anEnum : ((Enum<?>[]) value)) {
                                        enums.add(anEnum.name());
                                    }
                                    annotationBuilder = annotationBuilder.defineEnumerationArray(methodName,
                                            TypeDescription.ForLoadedType.of(componentType), enums.toArray(new String[0]));
                                } else if (value instanceof Boolean) {
                                    annotationBuilder = annotationBuilder.defineArray(methodName, (Boolean) value);
                                } else if (value instanceof Byte) {
                                    annotationBuilder = annotationBuilder.defineArray(methodName, (Byte) value);
                                } else if (value instanceof Short) {
                                    annotationBuilder = annotationBuilder.defineArray(methodName, (Short) value);
                                } else if (value instanceof Character) {
                                    annotationBuilder = annotationBuilder.defineArray(methodName, (Character) value);
                                } else if (value instanceof Integer) {
                                    annotationBuilder = annotationBuilder.defineArray(methodName, (Integer) value);
                                } else if (value instanceof Long) {
                                    annotationBuilder = annotationBuilder.defineArray(methodName, (Long) value);
                                } else if (value instanceof Float) {
                                    annotationBuilder = annotationBuilder.defineArray(methodName, (Float) value);
                                } else if (value instanceof Double) {
                                    annotationBuilder = annotationBuilder.defineArray(methodName, (Double) value);
                                } else if (value instanceof String) {
                                    annotationBuilder = annotationBuilder.defineArray(methodName, (String) value);
                                } else {
                                    annotationBuilder = annotationBuilder.define(methodName, AnnotationValue.ForConstant.of(value));
                                }
                            } else if (value instanceof Annotation) {
                                annotationBuilder = annotationBuilder.define(methodName, (Annotation) value);
                            } else if (value instanceof Class<?>) {
                                annotationBuilder = annotationBuilder.define(methodName, (Class<?>) value);
                            } else if (value instanceof Enum<?>) {
                                annotationBuilder = annotationBuilder.define(methodName, (Enum<?>) value);
                            } else {
                                annotationBuilder = annotationBuilder.define(methodName, AnnotationValue.ForConstant.of(value));
                            }
                        } catch (Exception e) {
                            log.error("annotate annotation failed !", e);
                        }
                    }
                    builder = builder.annotateType(annotationBuilder.build());
                }
            }
            if (!tagMethodEmpty) {
                Map<String, Map<String, Object>> commentMap = new HashMap<>(16);
                //初始化注释
                for (MethodJavadoc methodDoc : classDoc.getMethods()) {
                    Map<String, Object> map = new HashMap<>();
                    if (!methodDoc.isConstructor()) {
                        map.put(RETURN_KEY, format(methodDoc.getReturns()));
                    }
                    for (OtherJavadoc other : methodDoc.getOther()) {
                        map.put(other.getName(), format(other.getComment()));
                    }
                    for (ParamJavadoc paramDoc : methodDoc.getParams()) {
                        map.put(PARAM_KEY_PRE + paramDoc.getName(), format(paramDoc.getComment()));
                    }
                    for (ThrowsJavadoc throwsDoc : methodDoc.getThrows()) {
                        map.put(throwsDoc.getName(), format(throwsDoc.getComment()));
                    }
                    map.put(DESCRIPTION_KEY, format(methodDoc.getComment()));
                    commentMap.put(methodDoc.getName(), map);
                }
                for (Method method : beanClass.getDeclaredMethods()) {
                    //只有打了指定注解的方法才需要补注解
                    boolean needAnnotate = needAnnotate(method);
                    if (!needAnnotate) {
                        continue;
                    }
                    String targetMethodName = method.getName();
                    Map<String, Object> map = commentMap.get(targetMethodName);
                    Set<JavadocMapping<? extends Annotation>> tagMethod = getAnnotationsNeedToTag(method, this.tagMethod);
                    for (JavadocMapping<? extends Annotation> mapping : tagMethod) {
                        Class<? extends Annotation> annotationType = mapping.annotationType();
                        AnnotationDescription.Builder annotationBuilder = AnnotationDescription.Builder
                                .ofType(annotationType);
                        for (Method memberMethod : annotationType.getDeclaredMethods()) {
                            String methodName = memberMethod.getName();
                            try {
                                //优先取用户指定的默认值
                                Object value = memberMethod.invoke(mapping.getAnnotation());
                                if (null == value) {
                                    //注释的key
                                    String tagKey = mapping.getCommentKey(methodName);
                                    if (null != tagKey) {
                                        value = map.get(tagKey);
                                    }
                                }
                                if (null == value) {
                                    //没有映射，取注解定义的默认值
                                    value = memberMethod.getDefaultValue();
                                }
                                Class<?> returnType = memberMethod.getReturnType();
                                if (returnType.isArray()) {
                                    Class<Annotation> componentType = (Class<Annotation>) returnType.getComponentType();
                                    if (value instanceof Annotation[]) {
                                        annotationBuilder = annotationBuilder.defineAnnotationArray(methodName, componentType, (Annotation[]) value);
                                    } else if (value instanceof Class<?>[]) {
                                        annotationBuilder = annotationBuilder.defineTypeArray(methodName, (Class<?>[]) value);
                                    } else if (value instanceof Enum<?>[]) {
                                        Set<String> enums = new HashSet<>();
                                        for (Enum<?> anEnum : ((Enum<?>[]) value)) {
                                            enums.add(anEnum.name());
                                        }
                                        annotationBuilder = annotationBuilder.defineEnumerationArray(methodName,
                                                TypeDescription.ForLoadedType.of(componentType), enums.toArray(new String[0]));
                                    } else if (value instanceof Boolean) {
                                        annotationBuilder = annotationBuilder.defineArray(methodName, (Boolean) value);
                                    } else if (value instanceof Byte) {
                                        annotationBuilder = annotationBuilder.defineArray(methodName, (Byte) value);
                                    } else if (value instanceof Short) {
                                        annotationBuilder = annotationBuilder.defineArray(methodName, (Short) value);
                                    } else if (value instanceof Character) {
                                        annotationBuilder = annotationBuilder.defineArray(methodName, (Character) value);
                                    } else if (value instanceof Integer) {
                                        annotationBuilder = annotationBuilder.defineArray(methodName, (Integer) value);
                                    } else if (value instanceof Long) {
                                        annotationBuilder = annotationBuilder.defineArray(methodName, (Long) value);
                                    } else if (value instanceof Float) {
                                        annotationBuilder = annotationBuilder.defineArray(methodName, (Float) value);
                                    } else if (value instanceof Double) {
                                        annotationBuilder = annotationBuilder.defineArray(methodName, (Double) value);
                                    } else if (value instanceof String) {
                                        annotationBuilder = annotationBuilder.defineArray(methodName, (String) value);
                                    } else {
                                        annotationBuilder = annotationBuilder.define(methodName, AnnotationValue.ForConstant.of(value));
                                    }
                                } else if (value instanceof Annotation) {
                                    annotationBuilder = annotationBuilder.define(methodName, (Annotation) value);
                                } else if (value instanceof Class<?>) {
                                    annotationBuilder = annotationBuilder.define(methodName, (Class<?>) value);
                                } else if (value instanceof Enum<?>) {
                                    annotationBuilder = annotationBuilder.define(methodName, (Enum<?>) value);
                                } else {
                                    annotationBuilder = annotationBuilder.define(methodName, AnnotationValue.ForConstant.of(value));
                                }
                            } catch (Exception e) {
                                log.error("annotate annotation failed !", e);
                            }
                        }
                        try {
                            //只加注解，不改变原有的方法体，找了好久...
                            builder = builder.visit(new MemberAttributeExtension.ForMethod()
                                    .annotateMethod(annotationBuilder.build())
                                    .on(ElementMatchers.named(targetMethodName)));
                        } catch (Exception e) {
                            log.error(annotationType.getName(), e);
                        }
                    }

                    //处理参数
                    Map<String, Object> paramMap = new HashMap<>();
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        String key = entry.getKey();
                        if (key.startsWith(PARAM_KEY_PRE)) {
                            paramMap.put(key.replace(PARAM_KEY_PRE, ""), entry.getValue());
                        }
                    }
                    Parameter[] parameters = method.getParameters();
                    for (int i = 0; i < parameters.length; i++) {
                        Parameter parameter = parameters[i];
                        String parameterName = parameter.getName();
                        Set<JavadocMapping<? extends Annotation>> tagParameter = getAnnotationsNeedToTag(parameter, this.tagParameter);
                        for (JavadocMapping<? extends Annotation> mapping : tagParameter) {
                            Class<? extends Annotation> annotationType = mapping.annotationType();
                            AnnotationDescription.Builder annotationBuilder = AnnotationDescription.Builder
                                    .ofType(annotationType);
                            for (Method memberMethod : annotationType.getDeclaredMethods()) {
                                String methodName = memberMethod.getName();
                                try {
                                    //优先取用户指定的默认值
                                    Object value = memberMethod.invoke(mapping.getAnnotation());
                                    if (null == value) {
                                        //注释的key
                                        String tagKey = mapping.getCommentKey(methodName);
                                        if (null != tagKey) {
                                            if (PARAM_NAME_KEY.equals(tagKey)) {
                                                value = parameterName;
                                            } else if (PARAM_DESCRIPTION_KEY.equals(tagKey)) {
                                                value = paramMap.get(parameterName);
                                            }
                                        }
                                    }
                                    if (null == value) {
                                        //没有映射，取注解定义的默认值
                                        value = memberMethod.getDefaultValue();
                                    }
                                    Class<?> returnType = memberMethod.getReturnType();
                                    if (returnType.isArray()) {
                                        Class<Annotation> componentType = (Class<Annotation>) returnType.getComponentType();
                                        if (value instanceof Annotation[]) {
                                            annotationBuilder = annotationBuilder.defineAnnotationArray(methodName, componentType, (Annotation[]) value);
                                        } else if (value instanceof Class<?>[]) {
                                            annotationBuilder = annotationBuilder.defineTypeArray(methodName, (Class<?>[]) value);
                                        } else if (value instanceof Enum<?>[]) {
                                            Set<String> enums = new HashSet<>();
                                            for (Enum<?> anEnum : ((Enum<?>[]) value)) {
                                                enums.add(anEnum.name());
                                            }
                                            annotationBuilder = annotationBuilder.defineEnumerationArray(methodName,
                                                    TypeDescription.ForLoadedType.of(componentType), enums.toArray(new String[0]));
                                        } else if (value instanceof Boolean) {
                                            annotationBuilder = annotationBuilder.defineArray(methodName, (Boolean) value);
                                        } else if (value instanceof Byte) {
                                            annotationBuilder = annotationBuilder.defineArray(methodName, (Byte) value);
                                        } else if (value instanceof Short) {
                                            annotationBuilder = annotationBuilder.defineArray(methodName, (Short) value);
                                        } else if (value instanceof Character) {
                                            annotationBuilder = annotationBuilder.defineArray(methodName, (Character) value);
                                        } else if (value instanceof Integer) {
                                            annotationBuilder = annotationBuilder.defineArray(methodName, (Integer) value);
                                        } else if (value instanceof Long) {
                                            annotationBuilder = annotationBuilder.defineArray(methodName, (Long) value);
                                        } else if (value instanceof Float) {
                                            annotationBuilder = annotationBuilder.defineArray(methodName, (Float) value);
                                        } else if (value instanceof Double) {
                                            annotationBuilder = annotationBuilder.defineArray(methodName, (Double) value);
                                        } else if (value instanceof String) {
                                            annotationBuilder = annotationBuilder.defineArray(methodName, (String) value);
                                        } else {
                                            annotationBuilder = annotationBuilder.define(methodName, AnnotationValue.ForConstant.of(value));
                                        }
                                    } else if (value instanceof Annotation) {
                                        annotationBuilder = annotationBuilder.define(methodName, (Annotation) value);
                                    } else if (value instanceof Class<?>) {
                                        annotationBuilder = annotationBuilder.define(methodName, (Class<?>) value);
                                    } else if (value instanceof Enum<?>) {
                                        annotationBuilder = annotationBuilder.define(methodName, (Enum<?>) value);
                                    } else {
                                        annotationBuilder = annotationBuilder.define(methodName, AnnotationValue.ForConstant.of(value));
                                    }
                                } catch (Exception e) {
                                    log.error("annotate annotation failed !", e);
                                }
                            }
                            try {
                                //只加注解
                                builder = builder.visit(new MemberAttributeExtension.ForMethod()
                                        .annotateParameter(i, annotationBuilder.build())
                                        .on(ElementMatchers.named(targetMethodName)));
                            } catch (Exception e) {
                                log.error(annotationType.getName(), e);
                            }
                        }
                    }
                }
            }
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            DynamicType.Unloaded<?> unloaded = builder.make();
            try {
                String path = classLoader.getResource("/").getPath();
                log.warn("try to save in {}", path);
                unloaded.saveIn(new File(path));
            } catch (Exception e) {
                log.error("save failed !");
            }
            unloaded.load(classLoader, ClassReloadingStrategy.fromInstalledAgent());
        }
    }

    private Set<JavadocMapping<? extends Annotation>> getAnnotationsNeedToTag(AnnotatedElement annotatedElement, Set<JavadocMapping<? extends Annotation>> annotationsNeedToTag) {
        //只需要打没有打过的注解
        List<Class<? extends Annotation>> declared = Arrays.stream(annotatedElement.getDeclaredAnnotations())
                .map(Annotation::annotationType)
                .collect(Collectors.toList());
        return annotationsNeedToTag.stream()
                .filter(annotation -> !declared.contains(annotation.annotationType()))
                .collect(Collectors.toSet());
    }

    private boolean needAnnotate(Method method) {
        Set<Class<? extends Annotation>> marks = new HashSet<>(methodMarks);
        for (Annotation annotation : method.getDeclaredAnnotations()) {
            for (Class<? extends Annotation> mark : marks) {
                if (!AnnotationUtils.isAnnotationMetaPresent(annotation.annotationType(), mark)) {
                    return true;
                }
            }
        }
        return false;
    }


}
