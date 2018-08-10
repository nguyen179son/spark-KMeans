
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.Accumulator;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;

import java.io.IOException;
import java.util.*;
import java.util.List;


public class JavaKMeansExample {
    private static List<Point> ClusterCenter = new ArrayList<>();


    private static int NumOfCluster;

    public static JavaPairRDD<Point, String> getGroupOfPoint() {
        return groupOfPoint;
    }

    private static JavaPairRDD<Point, String> groupOfPoint;

    private static List<Point> getClusterCenter() {
        return ClusterCenter;
    }

    public static void setClusterCenter(List<Point> clusterCenter) {
        ClusterCenter = clusterCenter;
    }

    public static int getNumOfCluster() {
        return NumOfCluster;
    }

    private static void setNumOfCluster() {
        NumOfCluster = 3;
    }


    public static void setGroupOfPoint(JavaPairRDD<Point, String> groupOfPoint) {
        JavaKMeansExample.groupOfPoint = groupOfPoint;
    }

    public static void main(String[] args) throws IOException {

        SparkConf sparkConf = new SparkConf().setAppName("Read Text to RDD");
        JavaSparkContext sc = new JavaSparkContext(sparkConf);
        Accumulator<Integer> NumOfPoint = sc.accumulator(0);
        setNumOfCluster();
        // text file location
        String path = "/kmeans_data.txt";

        // read text file with javardd
        JavaRDD<Point> data = sc.textFile(path).map(word -> {
            String[] split = word.split(" ");
            double x = Double.parseDouble(split[0]);
            double y = Double.parseDouble(split[1]);
            NumOfPoint.add(1);
            return new Point(x, y);
        });
        Helper.GenerateRandomCenter(data,sc.broadcast(getNumOfCluster()));
//        Broadcast<List<Point>> preBroadcastCenter
//        sc.broadcast(ClusterCenter);

        // k means loop
        for (int i = 0; i < 20; i++) {
            // Grouping point with nearest Cluster center
            List<Point> centers = getClusterCenter();
            System.out.println("1: "+centers);
            Broadcast<List<Point>> preBroadcastCenter = sc.broadcast(centers);
            System.out.println("2: " + preBroadcastCenter.getValue());
            Helper.groupingPoints(data, preBroadcastCenter, sc.broadcast(getNumOfCluster()));
//            System.out.println(getGroupOfPoint().collectAsMap());
            Broadcast<List<Point>> afterGroupBroadcastCenter = Helper.reCalculateCenter(sc);
            System.out.println("3: " + afterGroupBroadcastCenter.getValue());

            if (Helper.compareCentersList(preBroadcastCenter.getValue(), afterGroupBroadcastCenter.getValue(), sc.broadcast(getNumOfCluster())))
                break;
        }
        Configuration conf = new Configuration();
        Path output = new Path("/save");
        FileSystem hdfs = FileSystem.get(conf);
        // delete existing directory
        if (hdfs.exists(output)) {
            hdfs.delete(output, true);
        }

        groupOfPoint.saveAsTextFile("/save");

        sc.stop();
    }


}