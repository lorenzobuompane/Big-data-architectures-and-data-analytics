package it.polito.bigdata.spark.example;

import org.apache.spark.api.java.*;

import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.SparkConf;

public class SparkDriver {

	public static void main(String[] args) {

		String inputPath;
		String outputPath;

		inputPath = args[0];
		outputPath = args[1];

		// Create a configuration object and set the name of the application
		//SparkConf conf=new SparkConf().setAppName("Spark Lab #6");

		// Use the following command to create the SparkConf object if you want to run
		// your application inside Eclipse.
		SparkConf conf = new SparkConf().setAppName("Spark Lab #6").setMaster("local");
		// Remember to remove .setMaster("local") before running your application on the
		// cluster

		// Create a Spark Context object
		JavaSparkContext sc = new JavaSparkContext(conf);

		// Read the content of the input file
		JavaRDD<String> readRDD = sc.textFile(inputPath);

		// ----- TASK 1

		String header = readRDD.first();

		JavaRDD<String> inputRDD = readRDD.filter(x -> !x.equals(header));

		JavaPairRDD<String, String> userProductRDD = inputRDD.mapToPair(review -> {
			String[] fields = review.split(",");
			return new Tuple2<String, String>(fields[2], fields[1]);
		});

		JavaPairRDD<String, Iterable<String>> reviewListRDD = userProductRDD.distinct().groupByKey();

		// reviewListRDD.saveAsTextFile(outputPath);

		// ----- TASK 2

		JavaRDD<Iterable<String>> onlyProductsRDD = reviewListRDD.values();
		JavaPairRDD<String, Integer> coupleRDD = onlyProductsRDD.flatMapToPair(x -> {
			ArrayList<Tuple2<String, Integer>> toReturn = new ArrayList<Tuple2<String, Integer>>();
			for (String extern : x) {
				for (String intern : x) {
					if (extern.compareTo(intern) < 0) {
						toReturn.add(new Tuple2<String, Integer>(extern.concat(",").concat(intern), 1));
					}
				}
			}
			return toReturn.iterator();
		});

		JavaPairRDD<String, Integer> reduceCoupleRDD = coupleRDD.reduceByKey((x, y) -> x + y);

		// ----- TASK 3

		JavaPairRDD<String, Integer> resultRDD = reduceCoupleRDD.filter(x -> x._2() > 1)
				.mapToPair(x -> new Tuple2<Integer, String>(x._2(), x._1())).sortByKey(false)
				.mapToPair(x -> new Tuple2<String, Integer>(x._2(), x._1()));

		// Store the result in the output folder
		resultRDD.saveAsTextFile(outputPath);

		// ----- BONUS TASK

		List<String> top10 = resultRDD.take(10).stream().map(x -> x._1() + " " + x._2()).collect(Collectors.toList());

		top10.forEach(x -> System.out.print(x + "\n"));

		// Close the Spark context
		sc.close();
	}
}
