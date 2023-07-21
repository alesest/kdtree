package com.alessandrosestito;

import com.alessandrosestito.kdtree.KDTree;
import com.alessandrosestito.kdtree.impl.KDTreeKey;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.Test;

public class KDTreeBasicTests {

    @Data
    @Builder
    @EqualsAndHashCode(callSuper = true)
    static class Point3d extends KDTreeKey {
        private final Integer x;
        private final Integer y;
        private final Integer z;
    }

    @Test
    public void test() {

        KDTree<Point3d, String> kdTree = KDTree.of(Point3d.class, String.class).withAutoBalanceScoreThreshold(0.95).build();

        Point3d key = Point3d.builder().x(10).y(10).z(10).build();
        String value = "Hello!";
        kdTree.insert(key, value);
        System.out.println(kdTree.size());

        Point3d key1 = Point3d.builder().x(10).y(10).z(10).build();
        kdTree.delete(key1);
        System.out.println(kdTree.size());

        kdTree.query()
                .featureBounds("x", 0, 10)
                .featureBounds("y", 10, 20)
                .featureBounds("z", 20, 30)
                .execute();

        kdTree.query()
                .lowerBound(Point3d.builder().x(0).y(10).z(20).build())
                .upperBound(Point3d.builder().x(10).y(20).z(30).build())
                .execute();

        kdTree.query()
                .lowerBound(Point3d.builder().x(-1).y(-1).z(-1).build())
                .upperBound(Point3d.builder().x(1).y(1).z(1).build())
                .filter((point3d, pointValue) -> {
                    int x1 = 0;
                    int x2 = point3d.getX();
                    int y1 = 0;
                    int y2 = point3d.getY();
                    int z1 = 0;
                    int z2 = point3d.getZ();
                    return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2)) <= 1;
                })
                .limit(1000L)
                .execute();

        kdTree.query()
                .featureBounds("x", 0, 10)
                .featureBounds("y", 10, 20)
                .featureBounds("z", 20, 30)
                .filter((point3d, s) -> true)
                .limit((long) Integer.MAX_VALUE)
                .execute();

        kdTree.query()
                .lowerBound(Point3d.builder().x(0).y(10).z(20).build())
                .upperBound(Point3d.builder().x(10).y(20).z(30).build())
                .filter((point3d, s) -> true)
                .limit((long) Integer.MAX_VALUE)
                .execute();
    }

}
