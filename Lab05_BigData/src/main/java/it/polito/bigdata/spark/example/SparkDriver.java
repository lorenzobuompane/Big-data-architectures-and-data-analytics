package it.polito.bigdata.spark.example;

import org.apache.spark.api.java.*;
import org.apache.spark.SparkConf;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class SparkDriver {

	public static void main(String[] args) {

		// The following two lines are used to switch off some verbose log messages
		Logger.getLogger("org").setLevel(Level.OFF);
		Logger.getLogger("akka").setLevel(Level.OFF);

		String inputPath;
		String outputPath;
		String prefix;

		inputPath = args[0];
		outputPath = args[1];
		prefix = args[2];

		// Create a configuration object and set the name of the application
		SparkConf conf = new SparkConf().setAppName("Spark Lab #5");

		// Use the following command to create the SparkConf object if you want to run
		// your application inside Eclipse.
		// Remember to remove .setMaster("local") before running your application on the
		// cluster
		// SparkConf conf = new SparkConf().setAppName("Spark Lab
		// #5").setMaster("local");

		// Create a Spark Context object
		JavaSparkContext sc = new JavaSparkContext(conf);

		// Read the content of the input file/folder
		// Each element/string of wordFreqRDD corresponds to one line of the input data
		// (i.e, one pair "word\tfreq")
		JavaRDD<String> wordFreqRDD = sc.textFile(inputPath);

		/*
		 * Task 1 ....... .......
		 */
		JavaRDD<String> prefixRDD = wordFreqRDD.filter(x -> x.startsWith(prefix));
		System.out.print("# lines first filter: " + prefixRDD.count() + "\n");

		JavaRDD<Integer> freqRDD = prefixRDD.map(x -> Integer.parseInt(x.split("\t")[1]));
		Integer max = freqRDD.reduce((e1, e2) -> {
			if (e1 > e2)
				return e1;
			else
				return e2;
		});
		System.out.print("MAX freq: " + max + "\n");

		/*
		 * Task 2 ....... .......
		 */
		Double threshold = 0.8 * max;

		JavaRDD<String> secondFilterRDD = prefixRDD.filter(x -> {
			if (Integer.parseInt(x.split("\t")[1]) >= threshold)
				return true;
			else
				return false;
		});
		System.out.print("# lines second filter: " + secondFilterRDD.count() + "\n");

		JavaRDD<String> finalMapRDD = secondFilterRDD.map(x -> x.split("\t")[0]);
		finalMapRDD.saveAsTextFile(outputPath);

		// Close the Spark context
		sc.close();
	}
}

// spark-submit --class it.polito.bigdata.spark.example.SparkDriver --deploy-mode client --master yarn Lab5-1.0.0.jar /data/students/bigdata-01QYD/Lab2/ HDFSOutputFolder "ho"
