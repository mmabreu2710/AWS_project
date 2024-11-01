#!/bin/bash

# Compile the Java file for the load balancer
source config.sh

java -cp aws-java-sdk-1.12.729/lib/aws-java-sdk-1.12.729.jar:aws-java-sdk-1.12.729/third-party/lib/*:aws-java-sdk-1.12.729/lib/commons-logging-1.2.jar:. loadbalancer/src/main/java/pt/ulisboa/tecnico/cnv/loadbalancer/LoadBalancer.java 

# Compile the Java file for the AS
#java -cp aws-java-sdk-1.12.729/lib/aws-java-sdk-1.12.729.jar:aws-java-sdk-1.12.729/third-party/lib/*:aws-java-sdk-1.12.729/lib/commons-logging-1.2.jar:. autoscaler/src/main/java/pt/ulisboa/tecnico/cnv/autoscaler/AutoScaler.java 
