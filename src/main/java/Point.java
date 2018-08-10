import scala.Serializable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class Point implements Serializable {
    private double x;
    private double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }


    public double distance(Point M) {
        return Math.sqrt(Math.pow(M.x - this.x, 2) + Math.pow(M.y - this.y, 2));
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    public static Point toPoint(String s) {
        String[] toArray = s.substring(1, s.length() - 1).split(",");
        double x = Double.parseDouble(toArray[0]);
        double y = Double.parseDouble(toArray[1]);
        return new Point(x, y);
    }

    @Override
    public boolean equals(Object o) {
        Point p = (Point) o;
        double delta = 0.01;
        return Math.abs(p.getX() - this.getX()) < delta && Math.abs(p.getY() - this.getY()) < delta;
    }

    public static void main(String[] args) throws IOException {
        Set<Point> pointSet = new HashSet<>();
        Random rand = new Random();

        for (int i = 0; i < 10000; i++) {
            double x = rand.nextInt(10000) / 10;
            double y = rand.nextInt(10000) / 10;
            Point point = new Point(x, y);
            if (pointSet.contains(point)) {
                i--;
            } else {
                pointSet.add(point);
            }
        }
        String fileName = "kmeans_data.txt";
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        for (Point p : pointSet
                ) {
            writer.write(p.getX() + " " + p.getY());
            writer.write("\n");
        }
        writer.close();
    }

}
