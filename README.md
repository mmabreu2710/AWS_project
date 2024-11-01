# AWS Virtual Machine Autoscaler and Load Balancer Project

This project implements an autoscaling and load balancing infrastructure on AWS, designed to launch virtual machines dynamically based on traffic and resource demand. It also includes a monitoring setup where metrics for each machine are logged and stored in a database on AWS. See project running here: https://youtu.be/7rdMOYZgqNg

## Overview

The project comprises several components that handle the automation of deploying virtual machines, configuring load balancing, scaling the infrastructure, and monitoring machine metrics. Key features include:

- **Autoscaler**: Dynamically scales the number of virtual machines based on resource utilization and traffic.
- **Load Balancer**: Distributes incoming traffic across the running virtual machines to ensure balanced load and minimize latency.
- **AWS Metrics Storage**: Collects and stores performance metrics for each instance in an AWS-hosted database, which can be accessed for monitoring and analysis.

## Project Structure

### Main Scripts

- **launch-vm.sh**: Deploys a new virtual machine instance on AWS. This script configures the instance with the necessary dependencies and settings required to run application processes.
- **install-vm.sh**: Installs required software and configurations on the launched virtual machines. This script ensures each instance has the appropriate runtime environment and packages.
- **runLoadBalancer.sh**: Starts the load balancer, which monitors active virtual machines and distributes traffic based on instance health and resource availability.
- **run_LBAndAS.sh**: Initializes both the load balancer and the autoscaler, orchestrating them to work together for optimal resource allocation.
- **run_VMState.sh**: Tracks the state of each virtual machine, updating metrics on usage, health, and availability. This is crucial for the autoscaler to make informed scaling decisions.
- **run_imageProc_DB.sh** and **run_rayTracer_DB.sh**: Start specific processes on the virtual machines for image processing and ray tracing. These scripts include commands to interact with AWS storage, uploading and retrieving data as needed.
- **sendBlurImageRequest.sh** and **sendEnhanceImageRequest.sh**: Send HTTP requests to the load balancer to initiate image processing tasks on the virtual machines.

### Additional Utilities

- **create-image.sh**: Creates an AWS AMI (Amazon Machine Image) from a running instance, enabling quick deployment of similar instances.
- **instrument.sh**: Used for performance monitoring by injecting instrumentation into the instances. This helps gather detailed metrics.
- **runWebServer.sh**: Launches a web server on each virtual machine, which serves as an endpoint for the load balancer to direct traffic.
- **test-vm.sh**: Tests the deployed VM instances to ensure they are running and configured correctly.

### Autoscaler

The autoscaler component continuously monitors CPU, memory usage, and traffic load to decide when to scale in or out. It is configured to launch new instances if the load exceeds a threshold and terminate instances when demand is low. The scaling parameters can be customized to meet specific application requirements.

### Load Balancer

The load balancer distributes traffic across active instances, monitoring their health and performance metrics. It ensures traffic is directed only to healthy instances and adjusts distribution based on load metrics, optimizing resource usage.

### Database and Metrics Monitoring

Performance metrics for each instance are stored in an AWS-hosted database. Metrics include CPU usage, memory, instance health status, and traffic details. These metrics are essential for tracking application performance and analyzing infrastructure costs.

## Running the Project

1. **Start the Load Balancer and Autoscaler**:
   ```
   ./run_LBAndAS.sh
   ```
   This command launches both the load balancer and autoscaler, starting the infrastructure management system.

2. **Launch a New Virtual Machine**:
   ```
   ./launch-vm.sh
   ```
   This command deploys a new instance on AWS, configured to integrate with the load balancer.

3. **Send an Image Processing Request**:
   To initiate an image processing task, send an HTTP request to the load balancer using one of the following:
   - Blur an image:
     ```
     ./sendBlurImageRequest.sh
     ```
   - Enhance an image:
     ```
     ./sendEnhanceImageRequest.sh
     ```

---

This setup is ideal for dynamically scaling an application to handle fluctuating traffic while monitoring and analyzing performance metrics through AWS.

## SpecialVFX@Cloud

This project contains three sub-projects:

1. `raytracer` - the Ray Tracing workload
2. `imageproc` - the BlurImage and EnhanceImage workloads
3. `webserver` - the web server exposing the functionality of the workloads

Refer to the `README.md` files of the sub-projects to get more details about each specific sub-project.

### How to build everything

1. Make sure your `JAVA_HOME` environment variable is set to Java 11+ distribution
2. Run `mvn clean package`

This code is only run locally using an app called xlaunch to launch  all the necessary terminals at the same time by using run_LB_and_servers.sh that launches the LB, AS and 3 webservers.

For us to launch 1 webservers with the java assist tool with use instrument.sh and use another terminal to launch requests. 
We can launch request of raytracer by going to raytracer/resources directory and launching each one of the scripts (1 request or multiple), and th same goes for imageproc.
To send blur or enhance scripts we can launch the script directly from the root directory.

For every request we write the metrics gained in metrics.txt file, every worker every 5 seconds writes its state (not incrementaly) to VMState.xml, and AS checks every change in the file and writes if the machine should be deleted or added to ASState.xml

