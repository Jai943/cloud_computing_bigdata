import java.io.*;
import java.util.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.util.*;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;

// Custom Writable class to represent a pair of integers
class Pair implements WritableComparable < Pair > {

    int i;
    int j;

    Pair() {}
    Pair(int i, int j) {
        this.i = i;
        this.j = j;
    }
    //read the pair from the input file 
    @Override
    public void readFields(DataInput input) throws IOException {
        i = input.readInt();
        j = input.readInt();
    }
    //write the pair to the output file
    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(i);
        output.writeInt(j);
    }
    //compare two pairs
    @Override
    public int compareTo(Pair o) {

        if (i == o.i)
            return j - o.j;
        else
            return i - o.i;
    }
    //pair to string value to write to the output file
    public String toString() {
        return i + "," + j + ",";
    }
    /*...*/
}

//Similar Writable class but to represent a pair - (pair of integer,double value)

class Pair1 implements Writable {
    int tag;
    int index;
    double value;

    Pair1() {
        tag = 0;
        index = 0;
        value = 0.0;
    }

    Pair1(int tag, int index, double value) {
        this.tag = tag;
        this.index = index;
        this.value = value;
    }

    @Override
    public void readFields(DataInput input) throws IOException {
        tag = input.readInt();
        index = input.readInt();
        value = input.readDouble();
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(tag);
        output.writeInt(index);
        output.writeDouble(value);
    }
}

public class Multiply extends Configured implements Tool {
    /* ... */

    // Mapper-Reducer functions with 3 mappers and 2 reducers for matrix multiplication with 3 classes
    //Job1 performs map reduce for matrix multiplication for individual matrices
    // Mapper for matrix M
    public static class MatrixM_Mapper extends Mapper < Object, Text, IntWritable, Pair1 > {
        @Override
        public void map(Object key, Text value, Context context) throws IOException,
        InterruptedException {

            Scanner s = new Scanner(value.toString()).useDelimiter(",");
            int i = s.nextInt();
            int j = s.nextInt();
            double v = s.nextDouble();
            // key-value pair -- (j, (0, i, v)) 
            context.write(new IntWritable(j), new Pair1(0, i, v));
        }
    }

    // Mapper for matrix N
    public static class MatrixN_Mapper extends Mapper < Object, Text, IntWritable, Pair1 > {
        @Override
        public void map(Object key, Text value, Context context) throws IOException,
        InterruptedException {

            Scanner s = new Scanner(value.toString()).useDelimiter(",");
            int i = s.nextInt();
            int j = s.nextInt();
            double v = s.nextDouble();

            //  key-value pair -- (i, (1, j, v))
            context.write(new IntWritable(i), new Pair1(1, j, v));
        }
    }

    // Reducer_multiply to perform the matrix multiplication
    public static class Reducer_multiply extends Reducer < IntWritable, Pair1, Pair, DoubleWritable > {

        ArrayList < Pair1 > Marray = new ArrayList < Pair1 > ();
        ArrayList < Pair1 > Narray = new ArrayList < Pair1 > ();

        @Override
        public void reduce(IntWritable key, Iterable < Pair1 > values, Context context) throws IOException,
        InterruptedException {

            Marray.clear();
            Narray.clear();

            // separate the values into two ArrayLists, Marray and Narray
            for (Pair1 x: values) {
                if (x.tag == 0) {
                    Pair1 temp = new Pair1();
                    temp.tag = 0;
                    temp.index = x.index;
                    temp.value = x.value;
                    Marray.add(temp);
                } else {
                    Pair1 temp = new Pair1();
                    temp.tag = 1;
                    temp.index = x.index;
                    temp.value = x.value;
                    Narray.add(temp);
                }
            }

            //matrix multiplication operation
            for (Pair1 a: Marray) {
                for (Pair1 b: Narray) {
                    //intermediate output
                    context.write(new Pair(a.index, b.index), new DoubleWritable(a.value * b.value));
                }
            }
        }
    }


    //Job2 performs the the sort and addition of the two matrices
    //sorting the indermediate results
    public static class Sort_Mapper extends Mapper < Object, Text, Pair, DoubleWritable > {
        @Override
        public void map(Object key, Text values, Context context) throws IOException,
        InterruptedException {

            //parsing the values - taking them into line and splitting them with ',' delimiter
            String lines = values.toString();
            String[] str = lines.split(",");
            int i = Integer.parseInt(str[0]);
            int j = Integer.parseInt(str[1]);
            double values1 = Double.parseDouble(str[2]);

            //
            context.write(new Pair(i, j), new DoubleWritable(values1));
        }
    }

    //adding the sorted results - aggregation
    public static class Aggregration_Reducer extends Reducer < Pair, DoubleWritable, Pair, DoubleWritable > {
        @Override
        public void reduce(Pair key, Iterable < DoubleWritable > values, Context context) throws IOException,
        InterruptedException {

            //aggregation using key pair values
            double res = 0.0;
            for (DoubleWritable val: values) {
                res += val.get();
            }
            //adding final output
            context.write(key, new DoubleWritable(res));
        }
    }

    @Override
    public int run(String[] args) throws Exception {
        /* ... */


        Configuration conf = getConf();
        // Create a new MapReduce job instance with a given name
        Job job1 = Job.getInstance(conf, "MR_job1");
        // Set the jar file for this job
        job1.setJarByClass(Multiply.class);
        // Set the output key and value types for the map phase of job1
        job1.setMapOutputKeyClass(IntWritable.class);
        job1.setMapOutputValueClass(Pair1.class);
        // Set the output key and value types for the reduce phase of job1
        job1.setOutputKeyClass(Pair.class);
        job1.setOutputValueClass(DoubleWritable.class);
        job1.setOutputFormatClass(TextOutputFormat.class);
        MultipleInputs.addInputPath(job1, new Path(args[0]), TextInputFormat.class, MatrixM_Mapper.class);
        MultipleInputs.addInputPath(job1, new Path(args[1]), TextInputFormat.class, MatrixN_Mapper.class);
        job1.setReducerClass(Reducer_multiply.class);
        FileOutputFormat.setOutputPath(job1, new Path(args[2]));
        // Submit and wait for the completion of job1
        job1.waitForCompletion(true);

        // Get another Hadoop configuration object
        Configuration conf2 = getConf();
        // Create a new MapReduce job instance with a different name
        Job job2 = Job.getInstance(conf2, "MR_job2");
        job2.setJarByClass(Multiply.class);
        job2.setReducerClass(Aggregration_Reducer.class);
        FileOutputFormat.setOutputPath(job2, new Path(args[3]));
        MultipleInputs.addInputPath(job2, new Path(args[2]), TextInputFormat.class, Sort_Mapper.class);
        job2.setOutputFormatClass(TextOutputFormat.class);
        job2.setMapOutputKeyClass(Pair.class);
        job2.setMapOutputValueClass(DoubleWritable.class);
        job2.setOutputKeyClass(Pair.class);
        job2.setOutputValueClass(DoubleWritable.class);
        job2.waitForCompletion(true);
        return 0;
    }

    public static void main(String[] args) throws Exception {
        // Run job using tool runner
        int exitCode = ToolRunner.run(new Multiply(), args);
        System.exit(exitCode);
    }
}