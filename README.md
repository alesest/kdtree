# SUPER-FAST LIGHTWEIGHT MULTI-DIMENSION BINARY TREE, to index everything! 🚀

Are you looking to optimize the performance of searches on your Java data structures? take a moment to read this guide, it could be for you.

## Introduction

A multidimensional index is a data structure used to organize and access information in a k-dimensional space. It represents a point or a region in k-dimensional space, where each dimension corresponds to a specific attribute or feature. The index allows efficient retrieval of data based on these attributes. By dividing the space into smaller regions and creating a hierarchical structure, searching and querying operations become faster and more optimized. The multi-dimensional index is commonly used in fields like computer graphics, geographic information systems (GIS), and database management systems to efficiently manage and query multidimensional data.

This library provides a set of out-of-the-box features that allow you to easily use the multidimensional binary tree.

## Installation
This library is available on maven central. Add this dependency to your project:
```xml
<dependency>
  <groupId>com.alessandrosestito</groupId>
  <artifactId>kdtree</artifactId>
  <version>0.0.1</version>
</dependency>
```
## Usage Guide
Before creating the structure it is necessary to construct the data key, which will therefore contain the set of features that will be indexed. Suppose we want to index a 3-dimensional space:

- Create Key
```java
    @Data
    @Builder
    @EqualsAndHashCode(callSuper = false)
    static class Point3d extends KDTreeKey {
        private final Integer x;
        private final Integer y;
        private final Integer z;
    }
```
Note that the attributes of the key must be immutable (therefore final) and the key must extend the KDTreeKey object provided with this package. Furthermore, the hashcode method will have to consider only the indexed attributes and not those of the super class.
The values associated with the keys can be of any type, we can create the tree;

- Create KDTree:
```java
KDTree<Point3d, String> kdTree = KDTree.of(Point3d.class, String.class).build();
```
The "KDTree.of" method requires the key and value classes, returns a builder in which it is possible to configure a self-balancing strategy (more details in the API documentation) and finally returns the created data structure.

- Insert data:
```java
Point3d key = Point3d.builder().x(10).y(10).z(10).build();
String value = "Hello!";
kdTree.insert(key, value);
```
As in a map we can insert a pair <key, value>, if the key is already present an exception will be thrown.

- Delete data:
```java
Point3d key1 = Point3d.builder().x(10).y(10).z(10).build();
kdTree.delete(key1);
```
The Delete method deletes a <key, value> pair if the key exists, a boolean value is returned for this condition.

- Perform query with feature bounds:
```java
List<Map.Entry<Point3d, String>> result =  kdTree.query()
                .featureBounds("x", 0, 10)
                .featureBounds("y", 10, 20)
                .featureBounds("z", 20, 30)
                .execute();
```
The "**KDTree.query**" method returns a configurable query builder in which we can decide the bounds for each feature or pass it keys directly as bounds, we can also set a match function and an upper limit of results.
- Perform query with key bounds:
```java
List<Map.Entry<Point3d, String>> result2 = kdTree.query()
                .lowerBound(Point3d.builder().x(0).y(10).z(20).build())
                .upperBound(Point3d.builder().x(10).y(20).z(30).build())
                .execute();
```
In this case the two outputs will be the same and the two ways are equivalent, you can choose the most suitable one.
- Perform query with match and limit. Search a maximum of 1000 points within a 3d sphere with origin at point 0,0,0 and radius 1:
```java
List<Map.Entry<Point3d, String>> result3 = kdTree.query()
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
```
Since we are looking for points in a 3d sphere with origin 0,0,0 and radius 1, we can narrow the search range in space [-1,-1,-1] and [1,1,1] and we do this using key bounds. Through the filter function we only pick those whose distance from the center is less than or equal to 1 and we ask the process to stop if it finds 1000 points.

## API and Main Features
The main APIs are exposed through the KDTree interface
- **of** :
  Allows you to create an instance of KDTree. It needs the key and value classes.
    - it is possible to add the **withAutoBalance(Duration interval)** option which, with the frequency defined in the interval, schedules a job to automatically balance the tree to improve performance.
    - using **withAutoBalanceScoreThreshold(double threshold)** it is possible to define a threshold value for the tree score above which balancing is not done. The range of the value is [0, 1], 1 perfectly balanced, 0 the tree has degenerated into a list. The default the threshold value is 0.75.
```java
KDTree<Key, Value> kdTree = KDTree.of(Key.class, Value.class)
                .withAutoBalance(Duration.of(10, ChronoUnit.SECONDS))
                .withAutoBalanceScoreThreshold(0.95)
                .build();
```
- **insert** :
  Inserts a new <key, value> pair into the structure. All fields indexed in the key must be non-null. If the key is already present, an exception is thrown.
```java
kdTree.insert(key, value);
```
- **delete** :
  Delete a new <key, value> pair in the structure starting from the key. A Boolean is returned indicating whether the item was deleted.
```java
boolean deleted = kdTree.delete(key);
```
- **query** :
  Create a configurable query builder with several options and then run the query on the data structure.
    - delimit the search space using featureBounds or key bounds (are equivalent)
    - apply a filter to the points delimited by bounds
    - limit the number of results
    - launch the query
```java
List<Map.Entry<Point3d, String>> result = kdTree.query()
        .featureBounds("x", 0, 10)
        .featureBounds("y", 10, 20)
        .featureBounds("z", 20, 30)
        .filter((point3d, v) -> true)
        .limit((long) Integer.MAX_VALUE)
        .execute();
```
or
```java
List<Map.Entry<Point3d, String>> result = kdTree.query()
        .lowerBound(Point3d.builder().x(0).y(10).z(20).build())
        .upperBound(Point3d.builder().x(10).y(20).z(30).build())
        .filter((point3d, v) -> true)
        .limit((long) Integer.MAX_VALUE)
        .execute();
```
- **get** :
  Returns the value associated with the key passed as input, if any, otherwise null.
```java
Map.Entry<K, V> node = kdTree.get(key);
```
- **containsKey** :
  Returns true if the value associated with the key passed as input, is present.
```java
boolean exists = kdTree.containsKey(key);
```
- **size** :
  Returns the number of elements in the structure.
```java
long size = kdTree.size();
```
- **score** :
  Returns a value between 0 and 1.
    - 1 means that the tree is perfectly balanced.
    - 0 means that the tree is degenerated into a list. in these cases a rebalancing is necessary.
```java
double score = kdTree.score();
```
- **balance** :
  Rearrange the structure of the tree in such a way that it is balanced as possible.
```java
kdTree.balance();
```
## Performance
The performance is the same as for a k-dimensional binary tree. Let us indicate with **n** the number of nodes and **k** the number of dimensions, we will have the following results:
| Insert    | Delete    | Search a key |
|-----------|-----------|--------------|
|O(k*log(n))|O(k*log(n))|O(k*log(n))   |

## Benchmark
To verify the performance of the structure, I compared the time spent on a search between the KDTree and the Java ArrayList.
- created a KDTree with a million elements and an ArrayList with the same elements.
- created a match function to be applied to both structures that simulates a light load.
- measured the insertion time for both.
- measured query execution time for both

Results:
|                  | KDTree | ArrayList |
|------------------|--------|-----------|
| Insert Time (ms) | 4197ms | 85ms      |
| Query Time (ms)  | 86ms   | 2547ms    |

From the results it is evident that it is necessary to pay computational time to build the structure, but subsequently the queries will be much faster.

You can find the experiment in the file [*src/test/java/com/alessandrosestito/KDTreeBenchmarkTests.java*](https://github.com/alesest/kdtree/blob/main/src/test/java/com/alessandrosestito/KDTreeBenchmarkTests.java)

## Use case
I used this data structure to optimize the performance of the people matching algorithm of a dating app.
Users had many features and had to match their search parameters with other users. The operations were done in cache but often degenerated into a fullscan of the data. This allowed to considerably reduce the computational cost of the matching algorithm.

## References
My Linkedin profile https://www.linkedin.com/in/alessandro-sestito-b61749150/

Enjoy the features ✌️
