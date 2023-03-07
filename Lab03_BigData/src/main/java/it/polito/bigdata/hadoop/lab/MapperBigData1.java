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
class MapperBigData1 extends Mapper<LongWritable, // Input key type
		Text, // Input value type
		Text, // Output key type
		IntWritable> {// Output value type

	protected void map(LongWritable key, // Input key type
			Text value, // Input value type
			Context context) throws IOException, InterruptedException {

		/* Implement the map method */
		String[] split = value.toString().split(",");

		if (split.length > 2) {
			for (int i = 1; i < split.length; i++) {
//				for (int j = i + 1; j < split.length - 1; j++) {
//					// (p1,p2 1)
//					context.write(new Text(new String(split[i] + "," + split[j])), new IntWritable(1));
//				}
				
				// CON QUESTO FOR CONSIDERO LE COPPIE ANCHE AL CONTRARIO
				for (int j = 1; j < split.length; j++) {
					if (split[i].compareTo(split[j]) < 0) {
						// (p1,p2 1)
						context.write(new Text(new String(split[i] + "," + split[j])), new IntWritable(1));
					}
				}
			}
		}
	}
}
