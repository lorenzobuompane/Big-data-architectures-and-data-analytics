package it.polito.bigdata.spark.sparkmllib;

import org.apache.spark.api.java.*;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.LogisticRegression;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.ml.feature.HashingTF;
import org.apache.spark.ml.feature.IDF;
import org.apache.spark.ml.feature.StopWordsRemover;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.mllib.linalg.Matrix;

public class SparkDriver {

	public static void main(String[] args) {

		String inputPath;

		inputPath = args[0];

		// Create a Spark Session object and set the name of the application
		// We use some Spark SQL transformation in this program
		SparkSession ss = SparkSession.builder().master("local").appName("Spark Lab9").getOrCreate();

		// Create a Java Spark Context from the Spark Session
		// When a Spark Session has already been defined this method
		// is used to create the Java Spark Context
		JavaSparkContext sc = new JavaSparkContext(ss.sparkContext());

		// *************************
		// Training step
		// *************************

		// Read training data from a textual file
		// Each lines has the format: class-label,list of words
		// E.g., 1,hadoop mapreduce
		JavaRDD<String> data = sc.textFile(inputPath);

		// Map each element (each line of the input file) to a LabeledDocument
		// LabeledDocument is a class defined in this application. Each instance
		// of LabeledDocument is characterized by two attributes:
		// - private double label
		// - private String text
		// Each instance of LabeledDocument is a "document" and the related class label.

		String header = data.first();

		JavaRDD<String> labeledData = data.filter(x -> {
			if (x.equals(header))
				return false;
			else if (Integer.parseInt(x.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)")[5]) == 0)
				return false;
			else
				return true;
		});

		JavaRDD<LabeledDocument> dataRDD = labeledData.map(record -> {
			String[] fields = record.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

			double useful = Double.parseDouble(fields[4]) / Double.parseDouble(fields[5]);

			if (useful > 0.9)
				useful = 1.0;
			else
				useful = 0.0;

			// The content of the document is after the comma
			String text = fields[9];

			// Return a new LabeledDocument
			return new LabeledDocument(useful, text);
		}).cache();

		// Prepare training data.
		// We use LabeledDocument, which is a JavaBean.
		// We use Spark SQL to convert RDDs of JavaBeans
		// into Dataset<Row>. The columns of the Dataset are label
		// and features
		Dataset<Row> schemaReviews = ss.createDataFrame(dataRDD, LabeledDocument.class).cache();

		Dataset<Row>[] splits = schemaReviews.randomSplit(new double[] { 0.7, 0.3 });
		Dataset<Row> trainingData = splits[0];
		Dataset<Row> testData = splits[1];

		// Configure an ML pipeline, which consists of five stages:
		// tokenizer -> split sentences in set of words
		// remover -> remove stopwords
		// hashingTF -> map set of words to a fixed-length feature vectors
		// (each word becomes a feature and the value of the feature
		// is the frequency of the word in the sentence)
		// idf -> compute the idf component of the TF-IDF measure
		// lr -> logistic regression classification algorithm

		// The Tokenizer splits each sentence in a set of words.
		// It analyzes the content of column "text" and adds the
		// new column "words" in the returned DataFrame
		Tokenizer tokenizer = new Tokenizer().setInputCol("text").setOutputCol("words");

		// Remove stopwords.
		// the StopWordsRemover component returns a new DataFrame with
		// new column called "filteredWords". "filteredWords" is generated
		// by removing the stopwords from the content of column "words"
		StopWordsRemover remover = new StopWordsRemover().setInputCol("words").setOutputCol("filteredWords");

		// Map words to a features
		// Each word in filteredWords must become a feature in a Vector object
		// The HashingTF Transformer performs this operation.
		// This operations is based on a hash function and can potentially
		// map two different word to the same "feature". The number of conflicts
		// in influenced by the value of the numFeatures parameter.
		// The "feature" version of the words is stored in Column "rawFeatures".
		// Each feature, for a document, contains the number of occurrences
		// of that feature in the document (TF component of the TF-IDF measure)
		HashingTF hashingTF = new HashingTF().setNumFeatures(1000).setInputCol("filteredWords")
				.setOutputCol("rawFeatures");

		// Apply the IDF transformation.
		// Update the weight associated with each feature by considering also the
		// inverse document frequency component. The returned new column is called
		// "features", that is the standard name for the column that contains the
		// predictive features used to create a classification model
		IDF idf = new IDF().setInputCol("rawFeatures").setOutputCol("features");

		// Create a classification model based on the logistic regression algorithm
		// We can set the values of the parameters of the
		// Logistic Regression algorithm using the setter methods.
		LogisticRegression lr = new LogisticRegression().setMaxIter(10).setRegParam(0.01);

		// Define the pipeline that is used to create the logistic regression
		// model on the training data.
		// In this case the pipeline is composed of five steps
		// - text tokenizer
		// - stopword removal
		// - TF-IDF computation (performed in two steps)
		// - Logistic regression model generation
		Pipeline pipeline = new Pipeline().setStages(new PipelineStage[] { tokenizer, remover, hashingTF, idf, lr });

		// Execute the pipeline on the training data to build the
		// classification model
		PipelineModel model = pipeline.fit(trainingData);

		// Now, the classification model can be used to predict the class label
		// of new unlabeled data

		/* ==== EVALUATION ==== */

		// Make predictions for the test set.
		Dataset<Row> predictions = model.transform(testData);

		// Select example rows to display.
		predictions.show(5);

		// Retrieve the quality metrics.
		MulticlassMetrics metrics = new MulticlassMetrics(predictions.select("prediction", "label"));

		// Confusion matrix
		Matrix confusion = metrics.confusionMatrix();
		System.out.println("Confusion matrix: \n" + confusion);

		double accuracy = metrics.accuracy();
		System.out.println("Accuracy = " + accuracy);

		// Close the Spark context
		sc.close();
	}
}
