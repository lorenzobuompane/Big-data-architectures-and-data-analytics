package it.polito.bigdata.spark.example;

import scala.Tuple2;

import org.apache.spark.api.java.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;

public class SparkDriver {

	public static void main(String[] args) {

		// The following two lines are used to switch off some verbose log messages
		Logger.getLogger("org").setLevel(Level.OFF);
		Logger.getLogger("akka").setLevel(Level.OFF);

		String inputPath;
		String inputPath2;
		Double threshold;
		String outputFolder;

		inputPath = args[0];
		inputPath2 = args[1];
		threshold = Double.parseDouble(args[2]);
		outputFolder = args[3];

		// Create a configuration object and set the name of the application
		// SparkConf conf = new SparkConf().setAppName("Spark Lab #7");

		// Use the following command to create the SparkConf object if you want to run
		// your application inside Eclipse.
		SparkConf conf = new SparkConf().setAppName("Spark Lab #7").setMaster("local");

		// Create a Spark Context object
		JavaSparkContext sc = new JavaSparkContext(conf);

		// INPUT
		JavaRDD<String> inputRegisterRDD = sc.textFile(inputPath);
		JavaRDD<String> inputStationsRDD = sc.textFile(inputPath2);

		String headerRegister = inputRegisterRDD.first();
		JavaRDD<String> registerRDD = inputRegisterRDD.filter(x -> !x.equals(headerRegister)).filter(x -> {
			if (Integer.parseInt(x.split("\t")[2]) == 0 && Integer.parseInt(x.split("\t")[3]) == 0)
				return false;
			return true;
		});

		// <KEY> (STATIONID)
		// <VALUE> (LONGITUDE \t LATITUDE)
		String headerStations = inputStationsRDD.first();
		JavaPairRDD<String, String> stationRDD = inputStationsRDD.filter(x -> !x.equals(headerStations))
				.mapToPair(x -> {
					String[] fields = x.split("\t");
					return new Tuple2<String, String>(fields[0], fields[1] + "\t" + fields[2]);
				});

		// <KEY> (STATIONID, DAYOFTHEWEEK - HOUR)
		// <VALUE> (1 if FULL otherwise 0, 1)
		JavaPairRDD<Tuple2<String, String>, Tuple2<Integer, Integer>> criticalListRDD = registerRDD.mapToPair(x -> {
			String[] fields = x.split("\t");
			String[] dateAndHour = fields[1].split(" ");
			String date = DateTool.DayOfTheWeek(dateAndHour[0]);
			String[] hour = dateAndHour[1].split(":");
			Integer isFull = 0;
			if (Integer.parseInt(fields[3]) == 0)
				isFull = 1;
			return new Tuple2<Tuple2<String, String>, Tuple2<Integer, Integer>>(
					new Tuple2<String, String>(fields[0], date + " - " + hour[0]),
					new Tuple2<Integer, Integer>(isFull, 1));
		});
		// criticalListRDD.saveAsTextFile(outputFolder);

		// <KEY> (STATIONID, DAYOFTHEWEEK - HOUR)
		// <VALUE> (#FULL slots, #TOT slots)
		JavaPairRDD<Tuple2<String, String>, Tuple2<Integer, Integer>> reduceStationsRDD = criticalListRDD
				.reduceByKey((x, y) -> {
					Integer full = x._1() + y._1();
					Integer total = x._2() + y._2();
					return new Tuple2<Integer, Integer>(full, total);
				});

		// <KEY> (STATIONID, DAYOFTHEWEEK - HOUR)
		// <VALUE> (CRITICAL value >= threshold)
		JavaPairRDD<Tuple2<String, String>, Double> criticalRDD = reduceStationsRDD.mapToPair(x -> {
			Double critical = (double) (x._2()._1()) / (x._2()._2());
			return new Tuple2<Tuple2<String, String>, Double>(x._1, critical);
		}).filter(x -> x._2() >= threshold);
		// criticalRDD.saveAsTextFile(outputFolder);

		// <KEY> (STATIONID)
		// <VALUE> (DAYOFTHEWEEK - HOUR \t CRITICAL)
		JavaPairRDD<String, String> mostCriticalRDDJavaPairRDD = criticalRDD.mapToPair(x -> {
			return new Tuple2<String, String>(x._1._1, x._1._2 + "\t" + x._2.toString());
		}).reduceByKey((x, y) -> {
			String[] x_fields = x.split("\t");
			String[] y_fields = y.split("\t");

			Double x_crit = Double.parseDouble(x_fields[1]);
			Double y_crit = Double.parseDouble(y_fields[1]);

			Integer x_hour = Integer.parseInt(x_fields[0].split(" ")[2]);
			Integer y_hour = Integer.parseInt(y_fields[0].split(" ")[2]);

			String x_day = x_fields[0].split(" ")[0];
			String y_day = y_fields[0].split(" ")[0];

			// ORDER BY CRITICAL
			if (x_crit > y_crit) {
				return x;
			} else if (x_crit < y_crit) {
				return y;
			} else {
				// ORDER BY HOUR
				if (x_hour < y_hour) {
					return x;
				} else if (x_hour > y_hour) {
					return y;
				} else {
					// ORDER BY DAY
					if (x_day.compareTo(y_day) < 0) {
						return x;
					} else {
						return y;
					}
				}
			}
		});
		// mostCriticalRDDJavaPairRDD.saveAsTextFile(outputFolder);

		// <KEY> (STATIONID)
		// <VALUE> (DAYOFTHEWEEK - HOUR \t CRITICAL, LONGITUDE \t LATITUDE)
		JavaPairRDD<String, Tuple2<String, String>> joinRDD = mostCriticalRDDJavaPairRDD.join(stationRDD);

		// joinRDD.saveAsTextFile(outputFolder);

		// Store in resultKML one String, representing a KML marker, for each station
		// with a critical timeslot
		JavaRDD<String> resultKML = joinRDD.map(x -> {
			String name = x._1();
			String day = x._2()._1().split("\t")[0].split(" ")[0];
			String hour = x._2()._1().split("\t")[0].split(" ")[2];
			String crit = x._2()._1().split("\t")[1];
			String coor = x._2()._2().split("\t")[0] + "," + x._2()._2().split("\t")[1];

			return "<Placemark>" + "<name>" + name + "</name>" + "<ExtendedData>" + "<Data name=\"DayWeek\">"
					+ "<value>" + day + "</value>" + "</Data>" + "<Data name=\"Hour\">" + "<value>" + hour + "</value>"
					+ "</Data>" + "<Data name=\"Criticality\">" + "<value>" + crit + "</value>" + "</Data>"
					+ "</ExtendedData>" + "<Point>" + "<coordinates>" + coor + "</coordinates>" + "</Point>"
					+ "</Placemark>";
		});

		// Invoke coalesce(1) to store all data inside one single partition/i.e., in one
		// single output part file
		resultKML.coalesce(1).saveAsTextFile(outputFolder);

		// Close the Spark context
		sc.close();
	}
}
