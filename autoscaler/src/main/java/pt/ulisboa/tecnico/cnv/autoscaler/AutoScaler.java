package pt.ulisboa.tecnico.cnv.autoscaler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.io.File;
import org.w3c.dom.*;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import javax.xml.parsers.ParserConfigurationException;


import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



public class AutoScaler {
    private static final Path ROOT_DIR = Paths.get("/home/vagrant/cnv-shared/cnv24-g28");

    private static String AWS_REGION = "us-east-2";
    private static String AMI_ID = "ami-0dae16999a724ec99";
    private static String KEY_NAME = "mykeypair";
    private static String SEC_GROUP_ID = "sg-00f61cb92be4f700c";

    private static AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion(AWS_REGION).withCredentials(new EnvironmentVariableCredentialsProvider()).build();
    private static AmazonCloudWatch cloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion(AWS_REGION).withCredentials(new EnvironmentVariableCredentialsProvider()).build();

    static class vmInstance{
        String instanceID;
        String instanceDNS;
        double cpuLoad;

        public vmInstance(String id, String dns, double cpu){
            this.instanceID = id;
            this.instanceDNS = dns;
            this.cpuLoad = cpu;
        }

        @Override
        public String toString() {
            return "vmInstance{" +
                    "instanceID='" + instanceID + '\'' +
                    ", instanceDNS='" + instanceDNS + '\'' +
                    ", cpuLoad='" + cpuLoad + '\'' +
                    '}';
        }
    }

    private static List<vmInstance> allVmInstances = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(AutoScaler::launchAndDestroy, 0, 1, TimeUnit.MINUTES);

    }

    public static void launchAndDestroy() {
        boolean launchNewInstance = false;
        String instanceToTerminate = null;
    
        for (vmInstance instance : allVmInstances) {
            if (instance.cpuLoad >= 0 && instance.cpuLoad <= 15) {
                instanceToTerminate = instance.instanceID;
            }

            if (instance.cpuLoad > 50) {
                launchNewInstance = true;
                break;
            }
        }

        
        if (activeVMS() == 0){
            launchNewInstance = true;
        }

        for (vmInstance instance : allVmInstances) {
            if (instance.cpuLoad < 0) {
                launchNewInstance = false;
                break;
            }
        }

        if (launchNewInstance) {
            createVM();
        }
        
    
        if (instanceToTerminate != null) {
            destroyVM(instanceToTerminate);
        }

        getCPUOfInstances();
    }

    public static void printVmInstances() {

        if (allVmInstances == null || allVmInstances.isEmpty()) {
            System.out.println("No instances available.");
            return;
        }

        for (vmInstance instance : allVmInstances) {
            System.out.println(instance);
        }
    }


    public static void createVM(){
        try {
            System.out.println("Starting a new instance.");
            RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
            runInstancesRequest.withImageId(AMI_ID)
                                .withInstanceType("t2.micro")
                                .withMinCount(1)
                                .withMaxCount(1)
                                .withKeyName(KEY_NAME)
                                .withSecurityGroupIds(SEC_GROUP_ID);
            RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);
            String newInstanceId = runInstancesResult.getReservation().getInstances().get(0).getInstanceId();
            String newInstanceDNS = runInstancesResult.getReservation().getInstances().get(0).getPublicDnsName();
             // Wait until the instance is running and has a public DNS name
            DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withInstanceIds(newInstanceId);
            while (newInstanceDNS.isEmpty()) {
                DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);
                Instance instance = describeInstancesResult.getReservations().get(0).getInstances().get(0);
                newInstanceDNS = instance.getPublicDnsName();
                if (instance.getState().getName().equals("running") && !newInstanceDNS.isEmpty()) {
                    break;
                }
                System.out.println("Waiting for instance to be in running state with DNS name...");
                Thread.sleep(5000); // Wait for 5 seconds before checking again
            }
            System.out.println("You have " + ec2.describeInstances().getReservations().size() + " Amazon EC2 instance(s) running.");

        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }catch (Exception e){
            System.out.println("Estudasses");
        }
    }
    

    public static void destroyVM(String InstanceId){
        try {
            System.out.println("Terminating the instance.");
            TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
            termInstanceReq.withInstanceIds(InstanceId);
            ec2.terminateInstances(termInstanceReq); 
        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public static int activeVMS(){
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        DescribeInstancesResult response = ec2.describeInstances(request);

        int runningInstancesCount = 0;

        for (Reservation reservation : response.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                if ("running".equals(instance.getState().getName())) {
                    runningInstancesCount++;
                }
            }
        }

        System.out.println("You have " + runningInstancesCount + " Amazon EC2 instance(s) running.");

        return runningInstancesCount;
    }

    private static Set<Instance> getInstances(AmazonEC2 ec2) throws Exception {
        Set<Instance> instances = new HashSet<Instance>();
        for (Reservation reservation : ec2.describeInstances().getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                if ("running".equals(instance.getState().getName()) && "ami-0dae16999a724ec99".equals(instance.getImageId())) {
                    instances.add(instance);
                }
            }
        }
        return instances;
    }

    public static void getCPUOfInstances() {
        try {
            Set<Instance> instances = getInstances(ec2);
            System.out.println("Total instances = " + instances.size());
    
            List<vmInstance> updatedInstances = new ArrayList<>();
    
            for (Instance instance : instances) {
                String iid = instance.getInstanceId();
                String state = instance.getState().getName();
                String dns = instance.getPublicDnsName();
                double cpuLoad = -1.0; // Default value if CPU load cannot be retrieved
    
                if (state.equals("running")) {
                    System.out.println("Running instance id = " + iid);
    
                    Dimension instanceDimension = new Dimension().withName("InstanceId").withValue(iid);
                    Date endTime = new Date(); // Current time
                    Date startTime = new Date(endTime.getTime() - 3660000); // 1 hour and 1 minute ago
    
                    GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                            .withStartTime(startTime)
                            .withNamespace("AWS/EC2")
                            .withPeriod(60) // Period of 1 minute
                            .withMetricName("CPUUtilization")
                            .withStatistics("Average")
                            .withDimensions(instanceDimension)
                            .withEndTime(endTime);
    
                    List<Datapoint> datapoints = cloudWatch.getMetricStatistics(request).getDatapoints();
                    if (!datapoints.isEmpty()) {
                        Datapoint latestDatapoint = datapoints.get(0);
                        for (Datapoint dp : datapoints) {
                            if (dp.getTimestamp().after(latestDatapoint.getTimestamp())) {
                                latestDatapoint = dp;
                            }
                        }
                        cpuLoad = latestDatapoint.getAverage();
                        System.out.println("CPU utilization for instance " + iid + " = " + cpuLoad);
                    } else {
                        System.out.println("No CPU utilization data for instance " + iid);
                    }
                } else {
                    System.out.println("Instance id = " + iid);
                }
                System.out.println("Instance State : " + state + ".");
    
                updatedInstances.add(new vmInstance(iid, dns, cpuLoad));
            }
    
            // Update the allVmInstances list with the new data
            allVmInstances = updatedInstances;
    
            // Update the XML file with the new data
            updateXmlFileWithInstances(allVmInstances);
    
        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Response Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        } catch (Exception e) {
            System.out.println("Estudasses");
        }
    }

    public static void updateXmlFileWithInstances(List<vmInstance> instances) {
        try {
            File xmlFile = new File("allVmInstances.xml");
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
    
            Element root = document.createElement("allVmInstances");
            document.appendChild(root);
    
            for (vmInstance instance : instances) {
                Element vmInstanceElement = document.createElement("vmInstance");
    
                Element idElement = document.createElement("instanceId");
                idElement.appendChild(document.createTextNode(instance.instanceID));
                vmInstanceElement.appendChild(idElement);
    
                Element dnsElement = document.createElement("instanceDns");
                dnsElement.appendChild(document.createTextNode(instance.instanceDNS));
                vmInstanceElement.appendChild(dnsElement);
    
                Element cpuElement = document.createElement("cpuLoad");
                cpuElement.appendChild(document.createTextNode(String.valueOf(instance.cpuLoad)));
                vmInstanceElement.appendChild(cpuElement);
    
                root.appendChild(vmInstanceElement);
            }
    
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    
            // Transform document to string
            StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            String xmlString = stringWriter.toString();
    
            // Remove redundant newlines and spaces
            xmlString = xmlString.replaceAll("(?m)^[ \t]*\r?\n", "");
    
            // Write the cleaned and indented XML string back to the file
            try (PrintWriter out = new PrintWriter(xmlFile)) {
                out.println(xmlString);
            }
        } catch (ParserConfigurationException | TransformerException | IOException e) {
            e.printStackTrace();
        }
    }
    
    
}
