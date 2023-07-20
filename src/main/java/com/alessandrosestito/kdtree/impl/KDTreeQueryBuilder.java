package com.alessandrosestito.kdtree.impl;

import com.alessandrosestito.kdtree.KDTree;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

@RequiredArgsConstructor(access = AccessLevel.MODULE)
public class KDTreeQueryBuilder<K extends KDTreeKey, V> {

    private final KDTree<K, V> kdTree;
    private final Map<String, KDTreeFeatureBound<? extends Comparable<?>>> boundMap = new HashMap<>();
    private BiPredicate<K, V> matchFunction;
    private Long limit;

    public <S extends Comparable<S>> KDTreeQueryBuilder<K, V> featureBounds(String name, S lowerBound, S upperBound) {
        KDTreeFeatureBound<S> bound = new KDTreeFeatureBound<>(name, lowerBound, upperBound);
        boundMap.put(name, bound);
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public KDTreeQueryBuilder<K, V> lowerBound(K lowerBoundPoint) {
        List<String> names = lowerBoundPoint.getAllFieldNamesNotNull();

        names.forEach(name -> boundMap.compute(name, (n, bound) -> {
            Comparable lb = lowerBoundPoint.getIndexedKeyValueByName(n);
            if (bound == null) {
                bound = new KDTreeFeatureBound<>(name, lb, null);
            }
            bound.setLowerBound(lb);
            return bound;
        }));

        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public KDTreeQueryBuilder<K, V> upperBound(K upperBoundPoint) {
        List<String> names = upperBoundPoint.getAllFieldNamesNotNull();

        names.forEach(name -> boundMap.compute(name, (n, bound) -> {
            Comparable ub = upperBoundPoint.getIndexedKeyValueByName(n);
            if (bound == null) {
                bound = new KDTreeFeatureBound<>(name, null, ub);
            }
            bound.setUpperBound(ub);
            return bound;
        }));

        return this;
    }

    public KDTreeQueryBuilder<K, V> filter(BiPredicate<K, V> matchFunction) {
        this.matchFunction = matchFunction;
        return this;
    }

    public KDTreeQueryBuilder<K, V> limit(Long limit) {
        this.limit = limit;
        return this;
    }

    public List<Map.Entry<K, V>> execute() {
        KDTreeQuery<K, V> query = KDTreeQuery.<K, V>builder()
                .boundsMap(boundMap)
                .matchFunction(matchFunction)
                .limit(limit)
                .build();
        return kdTree.find(query);
    }

}
