package org.dragon.zhang;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhangzicheng
 * @date 2021/03/06
 */
public class JavadocMapping<T extends Annotation> implements Annotation {

    /**
     * 注解的默认值
     */
    private final T annotation;

    /**
     * 处于阶段一时(也就是用户注入的状态)：String注解的方法名, String注释key或者注释tag key；
     * 阶段二(注释key会被替换为真正的注释)：String注解的方法名，String注释本身。
     */
    private final Map<String, String> mapping;

    public JavadocMapping(T annotation, Map<String, String> mapping) {
        this.annotation = annotation;
        this.mapping = mapping;
    }

    public T getAnnotation() {
        return annotation;
    }

    public String getCommentKey(String key) {
        return mapping.get(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JavadocMapping)) {
            return false;
        }
        JavadocMapping<?> that = (JavadocMapping<?>) o;
        return Objects.equals(annotation, that.annotation) &&
                Objects.equals(mapping, that.mapping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(annotation, mapping);
    }

    @Override
    public String toString() {
        return "JavadocMapping{" +
                "defaultValue=" + annotation +
                ", mapping=" + mapping +
                '}';
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return annotation.annotationType();
    }
}
