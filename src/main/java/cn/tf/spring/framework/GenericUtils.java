package cn.tf.spring.framework;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class GenericUtils {

    public static Class<?> getGenericClass(Class<?> clazz) {
        return getGenericClass(clazz, 0);
    }

    public static Class<?> getGenericClass(Class<?> clazz, int index) {
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (!(genericSuperclass instanceof ParameterizedType)) {
            System.out.println("无法获取泛型的class");
            return Object.class;
        }
        Type[] types = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
        if (types == null) {
            System.out.println("无法获取泛型的class");
            return Object.class;
        }
        if (index < 0 || index > types.length) {
            System.out.println("index is error");
            return Object.class;
        }
        Type type = types[index];

        if (type instanceof Class) {
            return (Class<?>) type;
        }
        return Object.class;
    }
}
