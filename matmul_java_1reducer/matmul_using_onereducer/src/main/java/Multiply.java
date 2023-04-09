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

    // Mapper-Reducer functions with 2 mappers and 1 reducers for matrix multiplication with 5 classes(pair,pair1,m_mapper,n_mapper,reducer)
    //Job1 performs map reduce for matrix multiplication for individual matrices
    // Mapper for matrix M

    // but in this code it returns pair and pair1 as output
    public static class MatrixM_Mapper extends Mapper < Object, Text, Pair, Pair1 > {
         // number of columns in M

        @Override
        public void map(Object key, Text value, Context context) throws IOException,
        InterruptedException {

            //getting the hardcoded values from job 
            Configuration conf = context.getConfiguration();
            int n = conf.getInt("NC",-1);

            Scanner s = new Scanner(value.toString()).useDelimiter(",");
            int i = s.nextInt();
            int j = s.nextInt();
            double v = s.nextDouble();
            // key-value pair -- ((i,k), (0, j, v)) 
            for (int k = 0; k < n; k++) {
                System.out.print("0"+","+j+","+v+'\n');
                context.write(new Pair(i, k), new Pair1(0, j, v));
            }

        }
    }

    // Mapper for matrix N
    public static class MatrixN_Mapper extends Mapper < Object, Text, Pair, Pair1 > {
         // number of columns in M


        @Override
        public void map(Object key, Text value, Context context) throws IOException,
        InterruptedException {

            Configuration conf = context.getConfiguration();
            int p = conf.getInt("MR",-1);

            Scanner s = new Scanner(value.toString()).useDelimiter(",");
            int i = s.nextInt();
            int j = s.nextInt();
            double v = s.nextDouble();

            // key-value pair -- ((k,j), (1, i, v))
            for (int k = 0; k < p; k++) {
                System.out.print("1"+","+i+","+v+'\n');
                context.write(new Pair(k,j), new Pair1(1, i, v));
            }

        }
    }

    // Reducer_multiply to perform the matrix multiplication
    public static class Reducer_multiply extends Reducer < Pair, Pair1, Pair, DoubleWritable > {

        List < Pair1 > Marray = new ArrayList < Pair1 > ();
        List < Pair1 > Narray = new ArrayList < Pair1 > ();


        @Override
        public void reduce(Pair key, Iterable < Pair1 > values, Context context) throws IOException,
        InterruptedException {

            Configuration conf = context.getConfiguration();
            int nc = conf.getInt("NC",-1);

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
            
            //sorting both array list
            Collections.sort(Marray,Comparator.comparing(e->e.index));
            Collections.sort(Narray,Comparator.comparing(e->e.index));

            double sum = 0.0;

            //Multiply and add elements of Marray and Narray
            for(Pair1 m : Marray) {
                for(Pair1 n : Narray) {
                    if (m.index == n.index) {
                        sum += m.value * n.value;
                        System.out.println(sum);
                    
                    }
                }
            }

            
            int i = key.i;
            int j = key.j;

            //Formatting output for showcase 3*3 matirx in output file

            if (key.i <= nc && key.j < nc) {
                context.write(key, new DoubleWritable(sum));
            }

        }   
    }
   
    @Override
    public int run(String[] args) throws Exception {
        /* ... */

        Configuration conf = getConf();

        // hard coding the input matrices values 
        
        conf.set("NC", "3");
        conf.set("MR", "11");

        // Create a new MapReduce job instance with a given name
        Job job1 = Job.getInstance(conf, "matrix_matrixmultiply");
        // Set the jar file for this job
        job1.setJarByClass(Multiply.class);
        // Set the output key and value types for the map phase of job1
        job1.setMapOutputKeyClass(Pair.class);
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

        return 0;
    }

    public static void main(String[] args) throws Exception {
        // Run job using tool runner
        int exitCode = ToolRunner.run(new Multiply(), args);
        System.exit(exitCode);
    }
}