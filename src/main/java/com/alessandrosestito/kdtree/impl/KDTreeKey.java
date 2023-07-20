package com.alessandrosestito.kdtree.impl;

import lombok.Getter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public abstract class KDTreeKey {

    private static final Map<Class<?>, List<Field>> subClassesFieldIndex = new HashMap<>();
    private static final Map<Class<?>, Map<String, Field>> subClassesFieldName = new HashMap<>();
    private final Map<Integer, Comparable<?>> subKeyValuesByIndex;
    private final Map<String, Comparable<?>> subKeyValuesByName;

    public KDTreeKey() {
        subClassesFieldIndex.computeIfAbsent(getClass(), KDTreeUtils::findIndexedFieldScan);
        subKeyValuesByIndex = new ConcurrentHashMap<>();
        subClassesFieldName.computeIfAbsent(getClass(), clazz -> KDTreeUtils.findIndexedFieldScan(clazz).stream().collect(Collectors.toMap(Field::getName, Function.identity())));
        subKeyValuesByName = new ConcurrentHashMap<>();
    }

    public Comparable<?> getIndexedKeyValueByIndex(int index) {
        return subKeyValuesByIndex.computeIfAbsent(index, integer -> {
            try {
                return (Comparable<?>) subClassesFieldIndex.get(getClass()).get(index).get(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Comparable<?> getIndexedKeyValueByName(String name) {
        return subKeyValuesByName.computeIfAbsent(name, s -> {
            try {
                return (Comparable<?>) subClassesFieldName.get(getClass()).get(s).get(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public List<String> getAllFieldNamesNotNull() {
        return subClassesFieldIndex.get(getClass()).stream().filter(field -> {
            try {
                return field.get(this) != null;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }).map(Field::getName).collect(Collectors.toList());
    }
}
