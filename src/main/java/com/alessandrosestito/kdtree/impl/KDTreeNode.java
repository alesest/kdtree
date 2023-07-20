package com.alessandrosestito.kdtree.impl;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;


@Data
@Builder
@ToString(exclude = {"left", "right"})
public class KDTreeNode<K, V> {
    private K key;
    private V value;
    private KDTreeNode<K, V> left;
    private KDTreeNode<K, V> right;
    private int axis;
}
