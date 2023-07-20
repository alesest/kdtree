package it.alessandrosestito.kdtree.impl;

import it.alessandrosestito.kdtree.KDTree;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@RequiredArgsConstructor
public class KDTreeBuilder<K extends KDTreeKey, V> {
    private final Class<K> keyClass;
    private boolean autoBalance = false;
    private Duration balanceInterval;

    public KDTreeBuilder<K, V> withAutoBalance(Duration interval) {
        autoBalance = true;
        balanceInterval = interval;
        return this;
    }

    public KDTree<K, V> build() {
        return new KDTreeImpl<>(keyClass, autoBalance, balanceInterval);
    }

}
