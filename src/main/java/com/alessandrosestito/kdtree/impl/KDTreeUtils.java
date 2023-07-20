package com.alessandrosestito.kdtree.impl;

import com.alessandrosestito.kdtree.SkipKDTreeIndex;
import com.alessandrosestito.kdtree.exception.KDTreeExceptionType;
import com.alessandrosestito.kdtree.exception.KDTreeRuntimeException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class KDTreeUtils {
    public static List<Field> findIndexedFieldScan(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .parallel()
                .map(field -> {
                    boolean skipIndex = field.isAnnotationPresent(SkipKDTreeIndex.class);
                    if (skipIndex) {
                        return null;
                    }
                    if (!Comparable.class.isAssignableFrom(field.getType())) {
                        throw new KDTreeRuntimeException(KDTreeExceptionType.FIELD_NOT_COMPARABLE, "field " + field.getName() + " is not comparable and not implement Comparable.class");
                    }
                    if (!Modifier.isFinal(field.getModifiers())) {
                        throw new KDTreeRuntimeException(KDTreeExceptionType.FIELD_NOT_IMMUTABLE, "field " + field.getName() + " is not final");
                    }
                    field.setAccessible(true);
                    return field;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
