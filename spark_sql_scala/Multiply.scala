// Databricks notebook source
import org.apache.spark.sql.functions._

// Define the input matrices A and B
val A = Seq((1, 1, -2.0), (0, 0, 5.0), (2, 2, 6.0), (0, 1, -3.0), (3, 2, 7.0), (0, 2, -1.0), (1, 0, 3.0), (1, 2, 4.0), (2, 0, 1.0), (3, 0, -4.0), (3, 1, 2.0))
val B = Seq((1, 0, 3.0), (0, 0, 5.0), (1, 2, -2.0), (2, 0, 9.0), (0, 1, -3.0), (0, 2, -1.0), (1, 1, 8.0), (2, 1, 4.0))

// Convert the input matrices to DataFrames
val A_df = A.toDF("i", "j", "value")
val B_df = B.toDF("i", "j", "value")

// Register the DataFrames as temporary views and compute the matrix product C using SQL
A_df.createOrReplaceTempView("A")
B_df.createOrReplaceTempView("B")
val C_df = spark.sql("""
  SELECT A.i AS i, B.j AS j, SUM(A.value * B.value) AS value
  FROM A JOIN B ON A.j = B.i
  GROUP BY A.i, B.j
  ORDER BY A.i, B.j
""")

C_df.show()
