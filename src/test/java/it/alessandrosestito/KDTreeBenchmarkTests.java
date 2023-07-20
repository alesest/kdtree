package it.alessandrosestito;

import it.alessandrosestito.kdtree.KDTree;
import it.alessandrosestito.kdtree.impl.KDTreeKey;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KDTreeBenchmarkTests {


    @Data
    @Builder
    @EqualsAndHashCode(callSuper = true)
    static class Point3dIndex extends KDTreeKey {
        private final Integer x;
        private final Integer y;
        private final Integer z;
    }

    @Data
    @Builder
    static class Point3d {
        private String id;
    }

    private static final int sampleNo = 1_000_000;
    private static final int V = 50;
    private static final int I = 10;

    @Test
    public void insertBenchmark() {

        int randInterval = 1000;

        List<Map.Entry<Point3dIndex, Point3d>> dataToInsert = IntStream.range(0, sampleNo).boxed().map(integer -> {
            Random random = new Random(integer);
            Point3dIndex key = Point3dIndex.builder()
                    .x(random.nextInt(randInterval))
                    .y(random.nextInt(randInterval))
                    .z(random.nextInt(randInterval))
                    .build();
            Point3d value = Point3d.builder().id(UUID.randomUUID().toString()).build();
            return new AbstractMap.SimpleEntry<>(key, value);
        }).collect(Collectors.toList());

        KDTree<Point3dIndex, Point3d> kdTree = KDTree.of(Point3dIndex.class, Point3d.class).build();
        PerformanceObserver observer = PerformanceObserver.start();
        dataToInsert.forEach(pt -> kdTree.insert(pt.getKey(), pt.getValue()));
        System.out.println("KDTree: " + observer.executionComplete() + " size :" + kdTree.size());

        observer = PerformanceObserver.start();
        List<Map.Entry<Point3dIndex, Point3d>> list = new LinkedList<>(dataToInsert);
        System.out.println("List: " + observer.executionComplete() + " size :" + list.size());


        observer = PerformanceObserver.start();
        List<Map.Entry<Point3dIndex, Point3d>> point3dIndicesKD = kdTree.query()
                .featureBounds("x", (V), (V + (3 * V)))
                .featureBounds("y", (V + I), (V + I + (3 * V)))
                .featureBounds("z", (V + 2 * I), (V + 2 * I + (3 * V)))
                .filter(matchFunction())
                .execute();
        System.out.println("KDTree: " + observer.executionComplete() + " size :" + point3dIndicesKD.size());

        observer = PerformanceObserver.start();
        List<Point3dIndex> point3dIndicesLIST = list.stream()
                .parallel()
                .filter(p -> matchFunction().test(p.getKey(), p.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        System.out.println("List: " + observer.executionComplete() + " size :" + point3dIndicesLIST.size());
    }

    private static BiPredicate<Point3dIndex, Point3d> matchFunction() {
        return (key, value) -> {
            //Simulate load
            IntStream.range(0, 1_000).boxed().forEach(integer -> {
            });
            //Actual filter
            return key.getX() >= V && key.getX() <= V + (3 * V) &&
                    key.getY() >= V + I && key.getY() <= V + I + (3 * V) &&
                    key.getZ() >= V + 2 * I & key.getZ() <= V + 2 * I + (3 * V);
        };
    }

    public static class PerformanceObserver {
        LocalDateTime startTime;

        public static PerformanceObserver start() {
            return new PerformanceObserver();
        }

        private PerformanceObserver() {
            startTime = LocalDateTime.now();
        }

        public long executionComplete() {
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            return duration.toMillis();
        }

    }

}
