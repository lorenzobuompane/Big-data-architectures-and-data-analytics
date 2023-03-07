package it.polito.bigdata.hadoop.lab;

import java.io.IOException;
import java.util.Vector;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Lab - Reducer
 */

/* Set the proper data types for the (key,value) pairs */
class ReducerBigData2 extends Reducer<IntWritable, // Input key type
		WordCountWritable, // Input value type
		Text, // Output key type
		IntWritable> { // Output value type

	@Override
	protected void reduce(IntWritable key, // Input key type
			Iterable<WordCountWritable> values, // Input value type
			Context context) throws IOException, InterruptedException {

		TopKVector<WordCountWritable> top100 = new TopKVector<WordCountWritable>(100);
		/* Implement the reduce method */
		for (WordCountWritable value : values) {
			top100.updateWithNewElement(new WordCountWritable(value.getWord(), value.getCount()));
		}

		Vector<WordCountWritable> top100Objects = top100.getLocalTopK();

		for (WordCountWritable top : top100Objects) {
			context.write(new Text(top.getWord()), new IntWritable(top.getCount().intValue()));
		}
	}
}
