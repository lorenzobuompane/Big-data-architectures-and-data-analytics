package it.polito.bigdata.hadoop.lab;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Lab - Reducer
 */

/* Set the proper data types for the (key,value) pairs */
class ReducerBigData1 extends Reducer<
                Text,           // Input key type
                Text,    // Input value type
                Text,           // Output key type
                DoubleWritable> {  // Output value type
    
    @Override
    protected void reduce(
        Text key, // Input key type
        Iterable<Text> values, // Input value type
        Context context) throws IOException, InterruptedException {

		/* Implement the reduce method */
    	Integer count = 0;
    	Double avg = 0.0;
    	ArrayList<String> tmp = new ArrayList<String>();    	
    	
    	for (Text value : values) {
    		tmp.add(value.toString());
       		String[] fields = value.toString().split(",");
    		Double score = Double.parseDouble(fields[1]);
    		avg += score;
    		count++;
    	}
    	
    	avg = avg/count;
    	
    	for (String value : tmp) {
    		String[] fields = value.split(",");
    		String id = fields[0];
    		Double score = Double.parseDouble(fields[1]);
    		Double normal = score - avg;
    		
    		//System.out.print(id + "---" + normal + "\n");
    		//context.write(new Text(key), new Text( id + ',' + normal ));
    		context.write(new Text(id), new DoubleWritable(normal));
    	}
    }
}
