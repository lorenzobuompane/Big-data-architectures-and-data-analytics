package it.polito.bigdata.hadoop.lab;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Lab - Mapper
 */

/* Set the proper data types for the (key,value) pairs */
class MapperBigData2 extends Mapper<Text, // Input key type
		Text, // Input value type
		IntWritable, // Output key type
		WordCountWritable> {// Output value type

	protected void map(Text key, // Input key type
			Text value, // Input value type
			Context context) throws IOException, InterruptedException {

		/* Implement the map method */
		context.write(new IntWritable(0), new WordCountWritable(key.toString(), Integer.parseInt(value.toString())));
	}
}
