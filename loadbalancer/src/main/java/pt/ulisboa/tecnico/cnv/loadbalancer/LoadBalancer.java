package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoadBalancer {

    private AtomicInteger index = new AtomicInteger(0);
    private static final Path XML_FILE_PATH = Paths.get("/home/vagrant/cnv-shared/cnv24-g28/allVmInstances.xml");
    private HashMap<String, InstanceInfo> vmInstances = new HashMap<>();

    private static String AWS_REGION = "us-east-2";
    private static AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .withRegion(AWS_REGION)
            .build();


    private static ConcurrentHashMap<ClientRequestParameters, Double> scores = new ConcurrentHashMap<>();
    
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    static {
        // Start the periodic task as soon as the class is loaded
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateScores();
            } catch (Exception e) {
                System.err.println("Error during scheduled execution of updateScore: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);

        // Shutdown hook to clean up when the JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownScheduler();
        }));
    }

    public LoadBalancer() {
        readXmlAndUpdateInstances();
        watchXmlFile();
    }

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

    static class InstanceInfo {
        vmInstance instance;
        int requestCount;

        public InstanceInfo(vmInstance instance, int requestCount) {
            this.instance = instance;
            this.requestCount = requestCount;
        }
    }


    static class ClientRequestParameters {
        // "raytracer", "/blurImage", "/enhanceImage"
        String type;
        // used by both raytracing and imageproc. image hash and image size.
        String input;
        String sizeOfInput;
        // used by raytracing
        String texmap;
        int scols;
        int srows;
        int wcols;
        int wrows;
        int coff;
        int roff;
        // used by imageproc. png, bmp etc.
        String format;
    }

    // Helper method to cleanly shutdown the scheduler
    private static void shutdownScheduler() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }

    private static void updateScores() {
        try {
            // Scan raytracer table
            ScanRequest scanRequest = new ScanRequest("raytracer");
            ScanResult scanResult = dynamoDB.scan(scanRequest);
            System.out.println("Result: " + scanResult);
            // https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/AttributeValue.html

            for(Map<String, AttributeValue> record : scanResult.getItems()) {
                String input = record.get("input").getS();
                String texmap = record.get("texmap").getS();
                int scols = Integer.parseInt(record.get("scols").getN());
                int srows = Integer.parseInt(record.get("srows").getN());
                int wcols = Integer.parseInt(record.get("wcols").getN());
                int wrows = Integer.parseInt(record.get("wrows").getN());
                int coff = Integer.parseInt(record.get("coff").getN());
                int roff = Integer.parseInt(record.get("roff").getN());
                int sizeOfInput = Integer.parseInt(record.get("sizeOfInput").getN());
                double score = Double.parseDouble(record.get("score").getN());
                System.out.println("0");
                ClientRequestParameters crp = new ClientRequestParameters();
                crp.type = "raytracer";
                crp.input = input;
                crp.texmap = texmap;
                crp.scols = scols;
                crp.srows = srows;
                crp.wcols = wcols;
                crp.wrows = wrows;
                crp.coff = coff;
                crp.roff = roff;
                crp.sizeOfInput = String.valueOf(sizeOfInput);
                System.out.println("0.5");
                if(!scores.containsKey(crp)) scores.put(crp, score);
                System.out.println("0.7");
            }
            System.out.println("1");
            // Scan imageProc table
            
            scanRequest = new ScanRequest("imageProc");
            System.out.println("2");
            scanResult = dynamoDB.scan(scanRequest);
            System.out.println("3");
            System.out.println("Result: " + scanResult);
            for(Map<String, AttributeValue> record : scanResult.getItems()) {
                String input = record.get("inputEncoded").getS();
                String format = record.get("format").getS();
                String type = record.get("typeOfRequest").getS();
                int sizeOfInput = Integer.parseInt(record.get("sizeOfInput").getN());
                double score = Double.parseDouble(record.get("score").getN());

                ClientRequestParameters crp = new ClientRequestParameters();
                crp.type = type;
                crp.input = input;
                crp.format = format;
                crp.sizeOfInput = String.valueOf(sizeOfInput);
                if(!scores.containsKey(crp)) scores.put(crp, score);
            }
            System.out.println("4");

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    private String getNextServer() {
        vmInstance selectedInstance = null;
        double lowestCpuLoad = Double.MAX_VALUE;
    
        // Find the instance with the lowest CPU load
        for (InstanceInfo info : vmInstances.values()) {
            if (info.instance.cpuLoad < lowestCpuLoad) {
                lowestCpuLoad = info.instance.cpuLoad;
                selectedInstance = info.instance;
            }
        }
    
        if (selectedInstance != null) {
            // Construct the server URL using the DNS of the selected instance
            return "http://" + selectedInstance.instanceDNS + ":8000";
        } //else {
            // Fallback to round-robin if no instances are found
           // return servers.get(index.getAndUpdate(i -> (i + 1) % servers.size()));
        //}
        return "";
    }   

    public synchronized void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String serverUrl = getNextServer();
                if (serverUrl == ""){
                    System.out.println("No servers running yet");
                }
                else{
                    try {
                        URL serverUri = new URL(serverUrl + exchange.getRequestURI());
                        HttpURLConnection connection = (HttpURLConnection) serverUri.openConnection();
                        connection.setRequestMethod(exchange.getRequestMethod());
                        connection.setDoOutput(true);
                        byte[] requestBody = exchange.getRequestBody().readAllBytes();
                        if (requestBody.length > 0) {
                            connection.getOutputStream().write(requestBody);
                        }

                        // Forward the response from the server to the client
                        InputStream serverResponse = connection.getInputStream();
                        exchange.sendResponseHeaders(connection.getResponseCode(), 0);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(serverResponse.readAllBytes());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1); // Server error
                    } finally {
                        exchange.close();
                    }
                }
            }
        });
        server.start();
        System.out.println("Load Balancer is running on port " + port);
    }

    private void readXmlAndUpdateInstances() {
        while (true) {
            try {
                File xmlFile = XML_FILE_PATH.toFile();
                while (!xmlFile.exists()) {
                    System.out.println("XML file does not exist. Waiting for 5 seconds...");
                    Thread.sleep(5000);
                }
                while (xmlFile.length() == 0) {
                    System.out.println("XML file is empty. Waiting for 5 seconds...");
                    Thread.sleep(5000);
                    continue;
                }

                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document document = documentBuilder.parse(xmlFile);
                document.getDocumentElement().normalize();

                NodeList nodeList = document.getElementsByTagName("vmInstance");
                HashMap<String, InstanceInfo> updatedInstances = new HashMap<>();

                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element element = (Element) nodeList.item(i);
                    String instanceID = element.getElementsByTagName("instanceId").item(0).getTextContent();
                    String instanceDNS = element.getElementsByTagName("instanceDns").item(0).getTextContent();
                    double cpuLoad = Double.parseDouble(element.getElementsByTagName("cpuLoad").item(0).getTextContent());
                    if (cpuLoad != -1) {
                        vmInstance instance = new vmInstance(instanceID, instanceDNS, cpuLoad);
                        if (vmInstances.containsKey(instanceID)) {
                            // Update existing instance info without changing the request count
                            updatedInstances.put(instanceID, new InstanceInfo(instance, vmInstances.get(instanceID).requestCount));
                        } else {
                            // Add new instance with request count 0
                            updatedInstances.put(instanceID, new InstanceInfo(instance, 0));
                        }
                    }
                }

                synchronized (vmInstances) {
                    vmInstances.clear();
                    vmInstances.putAll(updatedInstances);
                }

                System.out.println("Updated vmInstances from XML:");
                vmInstances.forEach((id, info) -> System.out.println(info.instance + ", requestCount=" + info.requestCount));


                break;

            } catch (ParserConfigurationException | IOException | InterruptedException | org.xml.sax.SAXException e) {
                e.printStackTrace();
            }
        }
    }

    private void watchXmlFile() {
        Thread thread = new Thread(() -> {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
                XML_FILE_PATH.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

                while (true) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY &&
                                XML_FILE_PATH.getFileName().toString().equals(event.context().toString())) {
                            System.out.println("XML file changed, updating instances...");
                            readXmlAndUpdateInstances();
                        }
                    }
                    key.reset();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        thread.setDaemon(true);
        thread.start();
    }


    public static void main(String[] args) throws IOException {
        new LoadBalancer().start(8000);       
    }
}

