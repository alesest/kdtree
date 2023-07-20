package it.alessandrosestito.kdtree;

import it.alessandrosestito.kdtree.impl.KDTreeBuilder;
import it.alessandrosestito.kdtree.impl.KDTreeKey;
import it.alessandrosestito.kdtree.impl.KDTreeQuery;
import it.alessandrosestito.kdtree.impl.KDTreeQueryBuilder;

import java.util.List;
import java.util.Map;

public interface KDTree<K extends KDTreeKey, V> {

    static <K extends KDTreeKey, V> KDTreeBuilder<K, V> of(Class<K> keyClass, Class<V> ignoredValueClass) {
        return new KDTreeBuilder<>(keyClass);
    }

    void insert(K key, V value);

    void delete(K key);

    KDTreeQueryBuilder<K, V> query();

    List<Map.Entry<K, V>> find(KDTreeQuery<K, V> query);

    Map.Entry<K, V> get(K key);

    boolean containsKey(K key);

    long size();

    void balance();

    double score();
}
