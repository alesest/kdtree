package com.alessandrosestito;


import com.alessandrosestito.kdtree.KDTree;
import com.alessandrosestito.kdtree.SkipKDTreeIndex;
import com.alessandrosestito.kdtree.exception.KDTreeExceptionType;
import com.alessandrosestito.kdtree.exception.KDTreeRuntimeException;
import com.alessandrosestito.kdtree.impl.KDTreeKey;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class KDTreeTests {

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Builder
    static class Point3dIndex extends KDTreeKey {
        private final Integer x;
        private final Integer y;
        private final Integer z;
        private final String t;
        @SkipKDTreeIndex
        private final String skip;
    }

    @Data
    @Builder
    static class Point3d {
        private int id;
    }

    private final KDTree<Point3dIndex, Point3d> kdTree = KDTree.of(Point3dIndex.class, Point3d.class).build();
    private static long sampleNo = 100_000;

    @Before
    public void before() {

        AtomicInteger atomicInteger = new AtomicInteger();
        (new Random()).doubles(sampleNo).forEach(value -> {

            long seed = (long) (value * Math.pow(10, 10));
            Random random = new Random(seed);

            Point3dIndex index = Point3dIndex.builder()
                    .x(random.nextInt())
                    .y(random.nextInt())
                    .z(random.nextInt())
                    .t(UUID.randomUUID().toString())
                    .build();
            while (kdTree.containsKey(index)) {
                index = Point3dIndex.builder()
                        .x(random.nextInt())
                        .y(random.nextInt())
                        .z(random.nextInt())
                        .t(UUID.randomUUID().toString())
                        .build();
            }

            Point3d point3d = Point3d.builder().id(atomicInteger.incrementAndGet()).build();
            kdTree.insert(index, point3d);
        });
    }

    @Test
    public void sizeTest() {
        Assert.assertEquals(kdTree.size(), fullScanCount());
    }

    @Test
    public void insertTest() {
        Point3dIndex index = Point3dIndex.builder().x(10).y(10).z(null).build();

        try {
            kdTree.insert(null, null);
        } catch (KDTreeRuntimeException e) {
            if (e.getExceptionType() != KDTreeExceptionType.KEY_IS_NULL) {
                throw e;
            }
        }

        try {
            kdTree.insert(index, null);
        } catch (KDTreeRuntimeException e) {
            if (e.getExceptionType() != KDTreeExceptionType.FIELD_CANNOT_BE_NULL) {
                throw e;
            }
        }

        index = fullScan().findFirst().orElseThrow().getKey();
        try {
            kdTree.insert(index, null);
        } catch (KDTreeRuntimeException e) {
            if (e.getExceptionType() != KDTreeExceptionType.KEY_EXISTS) {
                throw e;
            }
        }

        Random random = new Random();
        Point3d point3d = Point3d.builder().build();
        index = Point3dIndex.builder()
                .x(random.nextInt())
                .y(random.nextInt())
                .z(random.nextInt())
                .t(UUID.randomUUID().toString())
                .build();
        while (kdTree.containsKey(index)) {
            index = Point3dIndex.builder()
                    .x(random.nextInt())
                    .y(random.nextInt())
                    .z(random.nextInt())
                    .t(UUID.randomUUID().toString())
                    .build();
        }
        kdTree.insert(index, point3d);

        Assert.assertEquals(kdTree.size(), fullScanCount());
        Assert.assertEquals(kdTree.size(), sampleNo + 1);
        sampleNo = kdTree.size();

        List<Map.Entry<Point3dIndex, Point3d>> res = kdTree.query()
                .featureBounds("x", index.getX(), index.getX())
                .featureBounds("y", index.getY(), index.getY())
                .featureBounds("z", index.getZ(), index.getZ())
                .featureBounds("t", index.getT(), index.getT())
                .execute();

        if (res.size() != 1) {
            res.forEach(System.out::println);
        }
        Assert.assertEquals(res.size(), 1);
        Assert.assertEquals(res.get(0).getKey(), index);
        Assert.assertEquals(res.get(0).getValue(), point3d);
    }

    @Test
    public void deleteTest() {
        List<Point3dIndex> pointsToDelete = fullScan()
                .map(Map.Entry::getKey)
                .filter(point3dIndex -> point3dIndex.getX() < Integer.MIN_VALUE + Integer.MAX_VALUE / 4)
                .collect(Collectors.toList());

        AtomicReference<Point3dIndex> lastDeleted = new AtomicReference<>();

        pointsToDelete.forEach(point3dIndex -> {
            boolean deleted = kdTree.delete(point3dIndex);
            assertTrue(deleted);
            lastDeleted.set(point3dIndex);
        });

        if (lastDeleted.get() != null) {
            assertFalse(kdTree.delete(lastDeleted.get()));
        }
        Assert.assertEquals(kdTree.size(), fullScanCount());
        Assert.assertEquals(kdTree.size(), sampleNo - pointsToDelete.size());
        sampleNo = kdTree.size();

        insertTest();
    }

    @Test
    public void findTest() {
        insertTest();
        deleteTest();

        Map.Entry<Point3dIndex, Point3d> e = fullScan().findFirst().orElseThrow();
        long count = kdTree.query().upperBound(e.getKey()).lowerBound(e.getKey()).execute().size();
        assertEquals(count, 1);

        Set<Point3dIndex> point3dIndicesKD = kdTree.query()
                .featureBounds("x", Integer.MIN_VALUE + Integer.MAX_VALUE / 40, Integer.MAX_VALUE / 40)
                .featureBounds("y", Integer.MIN_VALUE + Integer.MAX_VALUE / 80, Integer.MAX_VALUE / 80)
                .featureBounds("z", Integer.MIN_VALUE + Integer.MAX_VALUE / 120, Integer.MAX_VALUE / 120)
                .execute().stream().map(Map.Entry::getKey).collect(Collectors.toSet());

        Set<Point3dIndex> point3dIndicesFULL = fullScan().map(Map.Entry::getKey)
                .filter(p -> (p.getX() >= (Integer.MIN_VALUE + Integer.MAX_VALUE / 40) && p.getX() <= (Integer.MAX_VALUE / 40) &&
                        p.getY() >= (Integer.MIN_VALUE + Integer.MAX_VALUE / 80) && p.getY() <= (Integer.MAX_VALUE / 80) &&
                        p.getZ() >= (Integer.MIN_VALUE + Integer.MAX_VALUE / 120) && p.getZ() <= (Integer.MAX_VALUE / 120))).collect(Collectors.toSet());

        checkFindResults(point3dIndicesKD, point3dIndicesFULL);

        point3dIndicesKD = kdTree.query()
                .featureBounds("x", (Integer.MIN_VALUE + Integer.MAX_VALUE / 40), Integer.MAX_VALUE)
                .featureBounds("y", (Integer.MIN_VALUE + Integer.MAX_VALUE / 80), (Integer.MAX_VALUE / 80))
                .featureBounds("z", (Integer.MIN_VALUE), (Integer.MAX_VALUE / 120))
                .execute().stream().map(Map.Entry::getKey).collect(Collectors.toSet());

        point3dIndicesFULL = fullScan().map(Map.Entry::getKey)
                .filter(p -> (p.getX() >= (Integer.MIN_VALUE + Integer.MAX_VALUE / 40) &&
                        p.getY() >= (Integer.MIN_VALUE + Integer.MAX_VALUE / 80) && p.getY() <= (Integer.MAX_VALUE / 80) &&
                        p.getZ() <= (Integer.MAX_VALUE / 120))).collect(Collectors.toSet());

        checkFindResults(point3dIndicesKD, point3dIndicesFULL);

        point3dIndicesKD = kdTree.query()
                .featureBounds("x", (Integer.MIN_VALUE + Integer.MAX_VALUE / 40), (Integer.MAX_VALUE / 40))
                .featureBounds("y", (Integer.MIN_VALUE + Integer.MAX_VALUE / 80), (Integer.MAX_VALUE / 80))
                .featureBounds("z", (Integer.MIN_VALUE + Integer.MAX_VALUE / 120), (Integer.MAX_VALUE / 120))
                .filter((point3dIndex, point3d) -> point3d.getId() % 2 == 0)
                .execute().stream().map(Map.Entry::getKey).collect(Collectors.toSet());

        point3dIndicesFULL = fullScan().filter(pt -> pt.getValue().getId() % 2 == 0).map(Map.Entry::getKey)
                .filter(p -> (p.getX() >= (Integer.MIN_VALUE + Integer.MAX_VALUE / 40) && p.getX() <= (Integer.MAX_VALUE / 40) &&
                        p.getY() >= (Integer.MIN_VALUE + Integer.MAX_VALUE / 80) && p.getY() <= (Integer.MAX_VALUE / 80) &&
                        p.getZ() >= (Integer.MIN_VALUE + Integer.MAX_VALUE / 120) && p.getZ() <= (Integer.MAX_VALUE / 120))).collect(Collectors.toSet());

        checkFindResults(point3dIndicesKD, point3dIndicesFULL);

        point3dIndicesKD = kdTree.query()
                .featureBounds("x", Integer.MIN_VALUE + Integer.MAX_VALUE / 40, Integer.MAX_VALUE / 40)
                .featureBounds("y", Integer.MIN_VALUE + Integer.MAX_VALUE / 80, Integer.MAX_VALUE / 80)
                .featureBounds("z", Integer.MIN_VALUE + Integer.MAX_VALUE / 120, Integer.MAX_VALUE / 120)
                .limit(1000L)
                .filter((point3dIndex, point3d) -> point3d.getId() % 2 == 0)
                .execute().stream().map(Map.Entry::getKey).collect(Collectors.toSet());

        Assert.assertEquals(point3dIndicesKD.size(), 1000L);

        point3dIndicesKD = kdTree.query()
                .featureBounds("t", "", "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz")
                .execute().stream().map(Map.Entry::getKey).collect(Collectors.toSet());

        Assert.assertEquals(point3dIndicesKD.size(), kdTree.size());
    }

    @Test
    public void balance() {
        KDTree<Point3dIndex, Point3d> kd = KDTree.of(Point3dIndex.class, Point3d.class).build();

        IntStream.range(0, 10_000).boxed().forEach(i -> kd.insert(Point3dIndex.builder().x(i).y(i).z(i).t(i.toString()).build(), null));

        Set<Point3dIndex> r1 = kd.query().featureBounds("x", 100, 200).execute().stream().map(Map.Entry::getKey).collect(Collectors.toSet());
        double lowScore = kd.score();
        kd.balance();
        double highScore = kd.score();
        System.out.println("balance - low " + lowScore + " high " + highScore);
        Set<Point3dIndex> r2 = kd.query().featureBounds("x", 100, 200).execute().stream().map(Map.Entry::getKey).collect(Collectors.toSet());

        r2.forEach(p -> assertTrue(p.toString(), r1.contains(p)));
        r1.forEach(p -> assertTrue(p.toString(), r2.contains(p)));
        assertTrue(lowScore < highScore);

        System.out.println("score1 " + kdTree.score());
        findTest();
        System.out.println("score2 " + kdTree.score());
        kdTree.balance();
        System.out.println("score3 " + kdTree.score());
        findTest();
    }

    @Test
    public void autoBalance() throws InterruptedException {
        KDTree<Point3dIndex, Point3d> kd = KDTree.of(Point3dIndex.class, Point3d.class).withAutoBalance(Duration.of(3, ChronoUnit.SECONDS)).build();
        IntStream.range(0, 10_000).boxed().forEach(i -> kd.insert(Point3dIndex.builder().x(i).y(i).z(i).t(i.toString()).build(), null));

        Set<Point3dIndex> r1 = kd.query().featureBounds("x", 100, 200).execute().stream().map(Map.Entry::getKey).collect(Collectors.toSet());
        System.out.println("tree ready - waiting auto-balance");
        double lowScore1 = kd.score();

        Thread.sleep(2_900);
        double lowScore2 = kd.score();

        assertEquals(lowScore1, lowScore2, 0.0);

        Thread.sleep(3_200);
        double highScore = kd.score();

        System.out.println("auto balance check - low " + lowScore1 + "high " + highScore);
        Set<Point3dIndex> r2 = kd.query().featureBounds("x", 100, 200).execute().stream().map(Map.Entry::getKey).collect(Collectors.toSet());

        r2.forEach(p -> assertTrue(p.toString(), r1.contains(p)));
        r1.forEach(p -> assertTrue(p.toString(), r2.contains(p)));
        assertTrue(lowScore1 < highScore);
    }

    private <T> void checkFindResults(Set<T> kdRes, Set<T> fullScanRes) {
        Assert.assertEquals(kdRes.size(), fullScanRes.size());
        kdRes.forEach(point3dIndex -> assertTrue(fullScanRes.contains(point3dIndex)));
        fullScanRes.forEach(point3dIndex -> assertTrue(kdRes.contains(point3dIndex)));
    }

    public long fullScanCount() {
        return fullScan().count();
    }

    public Stream<Map.Entry<Point3dIndex, Point3d>> fullScan() {
        return kdTree.query()
                .featureBounds("x", Integer.MIN_VALUE, Integer.MAX_VALUE)
                .featureBounds("y", Integer.MIN_VALUE, Integer.MAX_VALUE)
                .featureBounds("z", Integer.MIN_VALUE, Integer.MAX_VALUE)
                .execute().stream();
    }
}
