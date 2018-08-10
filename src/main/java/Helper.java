import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import scala.Tuple2;

import java.util.*;

public class Helper {

    /**
     * Grouping points with its cluster center
     *
     * @param data list of points
     */
    public static void groupingPoints(JavaRDD<Point> data, Broadcast<List<Point>> preBroadcastCenter,Broadcast<Integer> numOfCluster) {

        System.out.println(preBroadcastCenter.getValue());
        JavaPairRDD<Point, String> pointAndCenter = data.mapToPair(point -> {
            Point key = getNearestClusterCenter(point, preBroadcastCenter,numOfCluster);
            return new Tuple2<>(point, key.toString());
        });
        JavaKMeansExample.setGroupOfPoint(pointAndCenter);

    }

    public static Broadcast<List<Point>> reCalculateCenter(JavaSparkContext sc) {
        JavaPairRDD<String, Integer> countMap = countNumberOfPointInEachCluster();
        Broadcast<Map<String, Integer>> broadcastCountMap = sc.broadcast(countMap.collectAsMap());
        System.out.println("x: " + broadcastCountMap.getValue());
        JavaPairRDD<String, Point> tmpMapCenter = calcTotalCoordinateOfEachCluster();
        System.out.println("y: " + tmpMapCenter.collectAsMap());
        JavaRDD<Point> CenterMap = CalcNewCenterFromTotalCoordinateAndNumOfPointInEachCluster(tmpMapCenter, broadcastCountMap);
        System.out.println("z" + CenterMap.collect());
        Broadcast<List<Point>> broadcastCenterList = sc.broadcast(CenterMap.collect());
        JavaKMeansExample.setClusterCenter(CenterMap.collect());
        return broadcastCenterList;

    }

    /**
     * Create a Map with key=point and value=its center
     *
     * @param data List of Points
     * @return JavaPairRDD has key=Point and value = center
     */
    private static JavaPairRDD<Point, String> createMapOfPointAndCenter(JavaRDD<Point> data, Broadcast<List<Point>> preBroadcastCenter, Broadcast<Integer> numOfCluster) {
        System.out.println(preBroadcastCenter.getValue());
        System.out.println(JavaKMeansExample.getNumOfCluster());
        return data.mapToPair(point -> {
            Point key = getNearestClusterCenter(point, preBroadcastCenter,numOfCluster);
            return new Tuple2<>(point, key.toString());
        });
    }

    /**
     * count number of point in each cluster
     *
     * @return a map which key is cluster center and value is number of point in each cluster
     */
    private static JavaPairRDD<String, Integer> countNumberOfPointInEachCluster() {
        return JavaKMeansExample.getGroupOfPoint()
                .mapToPair(point -> new Tuple2<>(point._2, 1))
                .reduceByKey((a, b) -> a + b);
    }

    /**
     * Create map of sum of all point in each cluster: (x1,y1) + (x2,y2) = (x1+x2, y1+y2)
     *
     * @return JavapairRdd with key = cluster center and value = total coordinate in each cluster
     */
    private static JavaPairRDD<String, Point> calcTotalCoordinateOfEachCluster() {
        return JavaKMeansExample.getGroupOfPoint()
                .mapToPair(point -> new Tuple2<>(point._2, point._1))
                .reduceByKey((Point a, Point b) -> new Point(a.getX() + b.getX(), a.getY() + b.getY()));
    }

    /**
     * Calculate new center from total coordinate and number of points in each cluster which is calculated before
     *
     * @param tmpMapCenter Map of center and total coordinate in each cluster
     * @param countMap     Map of center and number of point in each cluster
     * @return javardd of new centers
     */
    private static JavaRDD<Point>
    CalcNewCenterFromTotalCoordinateAndNumOfPointInEachCluster(JavaPairRDD<String, Point> tmpMapCenter, Broadcast<Map<String, Integer>> countMap) {

        return tmpMapCenter.map(center -> {
            int countPoints = countMap.getValue().get(center._1);
            return new Point(center._2.getX() / countPoints, center._2.getY() / countPoints);
        });
    }

    /**
     * Generate ramdom center for k cluster
     */
    public static void GenerateRandomCenter(JavaRDD<Point> data, Broadcast<Integer> numOfCluster) {
        List<Point> centers = data.takeSample(true, numOfCluster.getValue(), System.nanoTime());
        JavaKMeansExample.setClusterCenter(centers);
    }

    private static Point getCenter(Broadcast<List<Point>> preBroadcastCenter, int i) {
        return preBroadcastCenter.getValue().get(i);
    }

    /**
     * get the Cluster center nearest to the point
     *
     * @param point point that nead to find the center
     * @return Nearest cluster center to the point
     */
    private static Point getNearestClusterCenter(Point point, Broadcast<List<Point>> preBroadcastCenter, Broadcast<Integer> numOfCluster) {
        Point minObject = new Point(0, 0);
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < numOfCluster.getValue(); i++) {
//            System.out.println(preBroadcastCenter + "\t" + getCenter(preBroadcastCenter,i)+"\t" +point.distance(getCenter(preBroadcastCenter, i)));
            if (point.distance(getCenter(preBroadcastCenter, i)) < minDistance) {
                minDistance = point.distance(getCenter(preBroadcastCenter, i));
                minObject = getCenter(preBroadcastCenter, i);
            }
        }
        return minObject;
    }

    public static boolean compareCentersList(List<Point> list1, List<Point> list2, Broadcast<Integer> numOfCluster) {
        System.out.println(list1 + "\n" + list2);
        for (int i = 0; i < numOfCluster.getValue(); i++) {
            if (!list1.get(i).equals(list2.get(i)))
                return false;
        }
        return true;
    }
}
