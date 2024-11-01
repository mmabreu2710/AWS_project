#!/bin/bash
# Compile the Java file
javac loadbalancer/src/main/java/pt/ulisboa/tecnico/cnv/loadbalancer/LoadBalancer.java

# Change to the directory containing the root of the package structure
cd loadbalancer/src/main/java

# Run the Java program with the full package name, ensuring the classpath is set correctly
java pt.ulisboa.tecnico.cnv.loadbalancer.LoadBalancer
