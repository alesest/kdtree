package com.alessandrosestito.kdtree.impl;

import com.alessandrosestito.kdtree.KDTree;
import com.alessandrosestito.kdtree.exception.KDTreeExceptionType;
import com.alessandrosestito.kdtree.exception.KDTreeRuntimeException;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@RequiredArgsConstructor
public class KDTreeBuilder<K extends KDTreeKey, V> {
    private final Class<K> keyClass;
    private boolean autoBalance = false;
    private double autoBalanceScoreThreshold = 0.90;
    private Duration balanceInterval;

    public KDTreeBuilder<K, V> withAutoBalance(Duration interval) {
        autoBalance = true;
        balanceInterval = interval;
        return this;
    }

    public KDTreeBuilder<K, V> withAutoBalanceScoreThreshold(double threshold) {
        if (threshold < 0 || threshold > 1) {
            throw new KDTreeRuntimeException(KDTreeExceptionType.AUTO_BALANCE_SCORE_THRESHOLD_RANGE, "AutoBalanceScoreThreshold range is [0,1]");
        }
        autoBalanceScoreThreshold = threshold;
        return this;
    }

    public KDTree<K, V> build() {
        return new KDTreeImpl<>(keyClass, autoBalance, autoBalanceScoreThreshold, balanceInterval);
    }

}
