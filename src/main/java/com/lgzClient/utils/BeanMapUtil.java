package com.lgzClient.utils;

import org.springframework.cglib.beans.BeanMap;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class BeanMapUtil {
    public static <T> BeanMap beanToBeanMap(T bean) {
        return BeanMap.create(bean);
    }

    public static <T> HashMap beanToHashMap(T bean) {
        BeanMap beanMap = BeanMap.create(bean);
        HashMap map = new HashMap(beanMap);
        return map;
    }

    public static <T> T mapToBean(Map map, Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        T bean = clazz.getConstructor().newInstance();
        BeanMap beanMap = BeanMap.create(bean);
        beanMap.putAll(map);
        return bean;
    }

}
