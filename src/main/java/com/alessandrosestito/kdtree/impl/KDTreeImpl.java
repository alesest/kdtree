package com.alessandrosestito.kdtree.impl;

import com.alessandrosestito.kdtree.KDTree;
import com.alessandrosestito.kdtree.exception.KDTreeExceptionType;
import com.alessandrosestito.kdtree.exception.KDTreeRuntimeException;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class KDTreeImpl<K extends KDTreeKey, V> implements KDTree<K, V> {

    private final Map<Integer, String> features;
    private final boolean withAutoBalance;
    private final double autoBalanceScoreThreshold;
    private final Duration interval;
    private KDTreeNode<K, V> root;
    private long size;
    private ReentrantLock autoBalanceLock;


    public KDTreeImpl(Class<K> clazz, boolean withAutoBalance, double autoBalanceScoreThreshold, Duration interval) {
        AtomicInteger ai = new AtomicInteger();
        features = KDTreeUtils.findIndexedFieldScan(clazz).stream()
                .map(field -> new AbstractMap.SimpleEntry<>(ai.getAndIncrement(), field.getName()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        this.withAutoBalance = withAutoBalance;
        this.interval = interval;
        this.autoBalanceScoreThreshold = autoBalanceScoreThreshold;
        root = null;
        size = 0;
        manageAutoBalance();
    }

    private void manageAutoBalance() {
        if (!withAutoBalance) {
            return;
        }
        autoBalanceLock = new ReentrantLock();
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::balance, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void insert(K key, V value) {
        if (key == null) {
            throw new KDTreeRuntimeException(KDTreeExceptionType.KEY_IS_NULL, "cannot insert data with null key");
        }
        Runnable unlock = autoBalanceLock();

        KDTreeNode<K, V> node = KDTreeNode.<K, V>builder().key(key).value(value).build();
        insertRec(root, node, 0);
        if (root == null) {
            root = node;
        }
        size++;

        unlock.run();
    }


    @Override
    public boolean delete(K key) {
        if (root == null) {
            throw new KDTreeRuntimeException(KDTreeExceptionType.KEY_NOT_EXISTS, "cannot delete node");
        }
        if (key == null) {
            throw new KDTreeRuntimeException(KDTreeExceptionType.KEY_IS_NULL, "key cannot be null");
        }
        Runnable unlock = autoBalanceLock();

        AtomicBoolean isDeleted = new AtomicBoolean(false);
        root = deleteNodeRec(root, key, null, isDeleted);
        if (isDeleted.get()) {
            size--;
        }

        unlock.run();
        return isDeleted.get();
    }

    @Override
    public KDTreeQueryBuilder<K, V> query() {
        return new KDTreeQueryBuilder<>(this);
    }


    @Override
    public List<Map.Entry<K, V>> find(KDTreeQuery<K, V> query) {
        Runnable unlock = autoBalanceLock();

        List<Map.Entry<K, V>> results = new ArrayList<>();
        findRec(root, query, results);

        unlock.run();
        return results;
    }


    @Override
    public Map.Entry<K, V> get(K key) {
        List<Map.Entry<K, V>> res = query().lowerBound(key).upperBound(key).limit(1L).execute();
        if (res.size() > 1) {
            throw new KDTreeRuntimeException(KDTreeExceptionType.DUPLICATE_KEY, "this is a bug, there a 2 or more results with same key: " + res);
        }
        if (res.size() == 0) {
            return null;
        }
        return res.get(0);
    }


    @Override
    public boolean containsKey(K key) {
        return Optional.ofNullable(get(key)).isPresent();
    }


    @Override
    public long size() {
        return size;
    }


    @Override
    public void balance() {
        if (withAutoBalance && score() >= autoBalanceScoreThreshold) {
            return;
        }

        Runnable unlock = autoBalanceLock();

        List<Map.Entry<K, V>> nodes = query().execute();
        root = balanceRec(nodes, 0);

        unlock.run();
    }

    @Override
    public double score() {
        Runnable unlock = autoBalanceLock();
        double score = Math.log(minDept(root)) / Math.log(maxDept(root));
        unlock.run();
        return score;
    }

    public int maxDept(KDTreeNode<K, V> node) {
        if (node == null) {
            return 0;
        }
        return 1 + Math.max(maxDept(node.getLeft()), maxDept(node.getRight()));
    }

    public int minDept(KDTreeNode<K, V> node) {
        if (node == null) {
            return 0;
        }
        return 1 + Math.min(minDept(node.getLeft()), minDept(node.getRight()));
    }

    private Runnable autoBalanceLock() {
        if (withAutoBalance) {
            autoBalanceLock.lock();
            return () -> autoBalanceLock.unlock();
        } else {
            return () -> {
            };
        }
    }

    private KDTreeNode<K, V> balanceRec(List<Map.Entry<K, V>> nodes, int depth) {

        if (nodes.size() == 0) {
            return null;
        }

        int axis = depth % features.size();
        if (nodes.size() == 1) {
            return KDTreeNode.<K, V>builder().key(nodes.get(0).getKey()).value(nodes.get(0).getValue()).axis(axis).build();
        }

        List<Map.Entry<K, V>> sorted = nodes.stream().parallel().sorted((o1, o2) -> compareNodeFeature(o1.getKey(), o2.getKey(), axis)).collect(Collectors.toList());
        int pivotIndex = (int) Math.floor(sorted.size() / 2.0);

        while ((pivotIndex + 1) < sorted.size() && sorted.get(pivotIndex).getKey().equals(sorted.get(pivotIndex + 1).getKey())) {
            pivotIndex++;
        }

        List<Map.Entry<K, V>> left;
        List<Map.Entry<K, V>> right;

        if (sorted.size() == 2) {
            left = List.of(sorted.get(0));
            right = List.of();
        } else {
            left = sorted.subList(0, pivotIndex);
            right = sorted.subList(pivotIndex + 1, sorted.size());
        }

        KDTreeNode<K, V> nl = balanceRec(left, depth + 1);
        KDTreeNode<K, V> nr = balanceRec(right, depth + 1);

        return KDTreeNode.<K, V>builder()
                .key(sorted.get(pivotIndex).getKey())
                .value(sorted.get(pivotIndex).getValue())
                .axis(axis)
                .left(nl)
                .right(nr)
                .build();
    }

    private void findRec(KDTreeNode<K, V> node, KDTreeQuery<K, V> query, List<Map.Entry<K, V>> results) {
        if (node == null) {
            return;
        }

        if (query.getLimit() != null && results.size() >= query.getLimit()) {
            return;
        }

        boolean inBounds = true;

        if (query.hasFeatureBounds()) {
            inBounds = isNodeWithinFeatureBounds(query.getBoundsMap(), node.getKey());
        }

        if (inBounds && (query.getMatchFunction() == null || query.getMatchFunction().test(node.getKey(), node.getValue()))) {
            results.add(new AbstractMap.SimpleImmutableEntry<>(node.getKey(), node.getValue()));
        }

        KDTreeFeatureBound<?> bound = findFeatureBounds(query, node);
        if (bound == null) {
            findRec(node.getLeft(), query, results);
            findRec(node.getRight(), query, results);
            return;
        }

        boolean lbLowerThenNode = compareNodeFeatureWithBound(bound.getLowerBound(), node.getKey(), bound.getName()) < 0;
        boolean ubGreaterThenNode = compareNodeFeatureWithBound(bound.getUpperBound(), node.getKey(), bound.getName()) >= 0;

        if (lbLowerThenNode && ubGreaterThenNode) {
            findRec(node.getLeft(), query, results);
            findRec(node.getRight(), query, results);
        } else if (lbLowerThenNode) {
            findRec(node.getLeft(), query, results);
        } else if (ubGreaterThenNode) {
            findRec(node.getRight(), query, results);
        }
    }

    private void insertRec(KDTreeNode<K, V> visitedNode, KDTreeNode<K, V> nodeToInsert, int depth) {
        int k = features.size();
        int axis = depth % k;

        if (visitedNode == null) {
            nodeToInsert.setAxis(axis);
            return;
        }

        if (nodeToInsert.getKey().equals(visitedNode.getKey())) {
            throw new KDTreeRuntimeException(KDTreeExceptionType.KEY_EXISTS, "key already exists into the tree");
        }

        if (compareNodeFeature(visitedNode.getKey(), nodeToInsert.getKey(), axis) > 0) {
            if (visitedNode.getLeft() == null) {
                nodeToInsert.setAxis((axis + 1) % k);
                visitedNode.setLeft(nodeToInsert);
            } else {
                insertRec(visitedNode.getLeft(), nodeToInsert, depth + 1);
            }
        } else {
            if (visitedNode.getRight() == null) {
                nodeToInsert.setAxis((axis + 1) % k);
                visitedNode.setRight(nodeToInsert);
            } else {
                insertRec(visitedNode.getRight(), nodeToInsert, depth + 1);
            }
        }
    }

    private KDTreeNode<K, V> deleteNodeRec(KDTreeNode<K, V> node, K nodeToDelete, KDTreeNode<K, V> parent, AtomicBoolean isDeleted) {
        if (node == null) {
            return null;
        }

        int axis = node.getAxis();
        if (node.getKey().equals(nodeToDelete)) {
            isDeleted.set(true);

            if (node.getRight() != null) {
                KDTreeNode<K, V> minR = findMinRec(node.getRight(), axis);
                node.setKey(minR.getKey());
                node.setValue(minR.getValue());
                node.setRight(deleteNodeRec(node.getRight(), minR.getKey(), node, isDeleted));

            } else if (node.getLeft() != null) {
                KDTreeNode<K, V> minL = findMinRec(node.getLeft(), axis);
                node.setKey(minL.getKey());
                node.setValue(minL.getValue());
                node.setRight(deleteNodeRec(node.getLeft(), minL.getKey(), node, isDeleted));
                node.setLeft(null);

            } else {
                if (parent.getLeft() != null && parent.getLeft().getKey().equals(node.getKey())) {
                    parent.setLeft(null);
                    return null;
                } else if (parent.getRight() != null && parent.getRight().getKey().equals(node.getKey())) {
                    parent.setRight(null);
                    return null;
                }
            }
            return node;
        }

        if (compareNodeFeature(nodeToDelete, node.getKey(), axis) < 0) {
            node.setLeft(deleteNodeRec(node.getLeft(), nodeToDelete, node, isDeleted));
        } else {
            node.setRight(deleteNodeRec(node.getRight(), nodeToDelete, node, isDeleted));
        }

        return node;
    }

    private KDTreeNode<K, V> findMinRec(KDTreeNode<K, V> node, int axis) {
        if (node == null) {
            return null;
        }

        if (node.getAxis() == axis) {
            if (node.getLeft() == null) {
                return node;
            } else {
                KDTreeNode<K, V> nl = findMinRec(node.getLeft(), axis);
                if (nl == null) {
                    return node;
                } else {
                    if (compareNodeFeature(nl.getKey(), node.getKey(), axis) < 0) {
                        return nl;
                    } else {
                        return node;
                    }
                }
            }
        }

        KDTreeNode<K, V> lNode = findMinRec(node.getLeft(), axis);
        KDTreeNode<K, V> rNode = findMinRec(node.getRight(), axis);

        List<KDTreeNode<K, V>> minimumValues = Arrays.asList(lNode, rNode, node);

        KDTreeNode<K, V> min = minimumValues.get(0);
        for (KDTreeNode<K, V> v : minimumValues) {
            if (min == null) {
                min = v;
            } else if (v != null) {
                if (compareNodeFeature(v.getKey(), min.getKey(), axis) < 0) {
                    min = v;
                }
            }
        }

        return min;
    }

    private boolean isNodeWithinFeatureBounds(Map<String, KDTreeFeatureBound<? extends Comparable<?>>> featureBoundMap, K keyNode) {
        return featureBoundMap.entrySet().stream().parallel().map(e -> {
            int compare1 = compareNodeFeatureWithBound(e.getValue().getLowerBound(), keyNode, e.getKey());
            int compare2 = compareNodeFeatureWithBound(e.getValue().getUpperBound(), keyNode, e.getKey());
            return compare1 <= 0 && compare2 >= 0;
        }).reduce((bool1, bool2) -> bool1 && bool2).orElseThrow();
    }

    @SuppressWarnings({"unchecked"})
    private int compareNodeFeature(K key1, K key2, int axis) {
        Comparable<Object> c1 = (Comparable<Object>) key1.getIndexedKeyValueByIndex(axis);
        Comparable<Object> c2 = (Comparable<Object>) key2.getIndexedKeyValueByIndex(axis);
        if (c1 == null) {
            throw new KDTreeRuntimeException(KDTreeExceptionType.FIELD_CANNOT_BE_NULL, "index " + axis + " of " + key1 + " cannot be null");
        }
        if (c2 == null) {
            throw new KDTreeRuntimeException(KDTreeExceptionType.FIELD_CANNOT_BE_NULL, "index " + axis + " of " + key2 + " cannot be null");
        }
        return c1.compareTo(c2);
    }

    @SuppressWarnings({"unchecked"})
    private int compareNodeFeatureWithBound(Comparable<?> bound, K key, String name) {
        Comparable<Object> c1 = (Comparable<Object>) bound;
        Comparable<Object> c2 = (Comparable<Object>) key.getIndexedKeyValueByName(name);
        return c1.compareTo(c2);
    }

    private KDTreeFeatureBound<? extends Comparable<?>> findFeatureBounds(KDTreeQuery<K, V> query, KDTreeNode<K, V> node) {
        return query.getBoundsMap().get(features.get(node.getAxis()));
    }

}
