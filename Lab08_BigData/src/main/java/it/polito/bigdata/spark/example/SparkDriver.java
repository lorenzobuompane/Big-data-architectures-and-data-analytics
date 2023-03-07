package it.polito.bigdata.spark.example;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import static org.apache.spark.sql.functions.*;

import java.sql.Timestamp;

import org.apache.spark.sql.types.DataTypes;

public class SparkDriver {

	public static void main(String[] args) {

		String inputPath;
		String inputPath2;
		Double threshold;
		String outputFolder;

		inputPath = args[0];
		inputPath2 = args[1];
		threshold = Double.parseDouble(args[2]);
		outputFolder = args[3];

		// Create a Spark Session object and set the name of the application
		// SparkSession ss = SparkSession.builder().appName("Spark Lab #8 -
		// Template").getOrCreate();

		// Invoke .master("local") to execute tha application locally inside Eclipse
		SparkSession ss = SparkSession.builder().master("local").appName("Spark Lab #8 - Template").getOrCreate();

		Dataset<Row> inputRegisterDF = ss.read().format("csv").option("delimiter", "\\t")
				.option("timestampFormat", "yyyy-MM-dd HH:mm:ss").option("header", true).option("inferSchema", true)
				.load(inputPath);

		Dataset<Row> inputStationsDF = ss.read().format("csv").option("delimiter", "\\t").option("header", true)
				.option("inferSchema", true).load(inputPath2).select("id", "longitude", "latitude");

		Dataset<Row> filterDF = inputRegisterDF.filter("used_slots!=0 or free_slots!=0");

		ss.udf().register("hourUDF", (Timestamp ts) -> {
			return DateTool.hour(ts);
		}, DataTypes.IntegerType);

		ss.udf().register("dayUDF", (Timestamp ts) -> {
			return DateTool.DayOfTheWeek(ts);
		}, DataTypes.StringType);

		ss.udf().register("isFullUDF", (Integer fs) -> {
			if (fs == 0)
				return 1;
			return 0;
		}, DataTypes.IntegerType);

		ss.udf().register("criticalUDF", (Long sumFull, Long tot) -> {
			return (double) sumFull / (double) tot;
		}, DataTypes.DoubleType);

		Dataset<Row> fullListDF = filterDF
				.selectExpr("station", "dayUDF(timestamp) AS day", "hourUDF(timestamp) AS hour",
						"isFullUDF(free_slots) AS isFull")
				.groupBy("station", "day", "hour").agg(sum("isFull").as("sumFull"), count("*").as("tot"));

		Dataset<Row> criticalDF = fullListDF.selectExpr("station", "day", "hour",
				"criticalUDF(sumFull, tot) AS criticality");

		Dataset<Row> thresholdDF = criticalDF.filter("criticality >" + String.valueOf(threshold));

		Dataset<Row> joinDF = thresholdDF
				.join(inputStationsDF, thresholdDF.col("station").equalTo(inputStationsDF.col("id")))
				.select("station", "day", "hour", "criticality", "longitude", "latitude")
				.sort(col("criticality").desc(), col("station").asc(), col("day").asc(), col("hour").asc());
		
		joinDF.coalesce(1).write().format("csv").option("header", true).save(outputFolder);

		// Close the Spark session
		ss.stop();
	}
}
