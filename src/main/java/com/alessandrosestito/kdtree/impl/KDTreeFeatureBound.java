package com.alessandrosestito.kdtree.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
class KDTreeFeatureBound<S> {
    private final String name;
    private Comparable<S> lowerBound;
    private Comparable<S> upperBound;
}
