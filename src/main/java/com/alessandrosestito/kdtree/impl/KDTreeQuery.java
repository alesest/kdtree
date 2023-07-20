package com.alessandrosestito.kdtree.impl;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.function.BiPredicate;

@Builder
@Getter
public class KDTreeQuery<K, V> {
    private BiPredicate<K, V> matchFunction;
    private Long limit;
    private Map<String, KDTreeFeatureBound<? extends Comparable<?>>> boundsMap;

    public boolean hasFeatureBounds() {
        return boundsMap != null && boundsMap.size() != 0;
    }
}
