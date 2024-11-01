package pt.ulisboa.tecnico.cnv.db_handler;

import java.util.HashMap;
import java.util.Map;

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

public class ImageProc_DB {

    private static String AWS_REGION = "us-east-2";
    private static AmazonDynamoDB dynamoDB;

    public static void main(String[] args) {
        if (args.length != 14) {
            System.err.println("Usage: java pt.ulisboa.tecnico.cnv.db_handler.Metrics_DB <inputEncoded> <format> <nmethods> <nblocks> <ninsts> <opTime> <cpuLoad> <memoryUsed> <vmHealthy> <ip> <threadId> <requestedURI> <sizeOfInput> <score>");
            System.exit(1);
        }
        
        String inputEncoded = args[0];
        String format = args[1];
        int nmethods = Integer.parseInt(args[2]);
        int nblocks = Integer.parseInt(args[3]);
        int ninsts = Integer.parseInt(args[4]);
        long opTime = Long.parseLong(args[5]);
        double cpuLoad = Double.parseDouble(args[6]);
        double memoryUsed = Double.parseDouble(args[7]);
        boolean vmHealthy = Boolean.parseBoolean(args[8]);
        String ip = args[9];
        long threadId = Long.parseLong(args[10]);
        String requestedURI = args[11];
        int sizeOfInput = Integer.parseInt(args[12]);
        double score = Double.parseDouble(args[13]);

        dynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .withRegion(AWS_REGION)
            .build();

        try {
            String tableName = "imageProc";

            // Create a table with a primary hash key named 'id' which holds a number
            CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
                .withKeySchema(new KeySchemaElement().withAttributeName("id").withKeyType(KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition().withAttributeName("id").withAttributeType(ScalarAttributeType.N))
                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

            // Create table if it does not exist yet
            TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);
            // wait for the table to move into ACTIVE state
            TableUtils.waitUntilActive(dynamoDB, tableName);

            // Describe our new table
            //DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
            //TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
            //System.out.println("Table Description: " + tableDescription);

            // Add the item to the DynamoDB table
            dynamoDB.putItem(new PutItemRequest(tableName, newItem(getNextId(tableName), inputEncoded, format, nmethods, nblocks, ninsts, opTime, cpuLoad, memoryUsed, vmHealthy, ip, threadId, requestedURI, sizeOfInput, score)));

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
        } catch (InterruptedException e) {
            System.out.println("Caught an InterruptedException, which means the thread was interrupted while waiting for the table to become active.");
            System.out.println("Error Message: " + e.getMessage());
        }
    }

    private static long getNextId(String tableName) {
        ScanRequest scanRequest = new ScanRequest().withTableName(tableName);
        ScanResult scanResult = dynamoDB.scan(scanRequest);
        return scanResult.getItems().size() + 1;
    }

    private static Map<String, AttributeValue> newItem(long id, String inputEncoded, String format, int nmethods, int nblocks, int ninsts, long opTime, double cpuLoad, double memoryUsed, boolean vmHealthy, String ip, long threadId, String requestedURI, int sizeOfInput, double score) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("id", new AttributeValue().withN(Long.toString(id)));
        item.put("inputEncoded", new AttributeValue().withS(inputEncoded));
        item.put("format", new AttributeValue().withS(format));
        item.put("nmethods", new AttributeValue().withN(Integer.toString(nmethods)));
        item.put("nblocks", new AttributeValue().withN(Integer.toString(nblocks)));
        item.put("ninsts", new AttributeValue().withN(Integer.toString(ninsts)));
        item.put("opTime", new AttributeValue().withN(Long.toString(opTime)));
        item.put("cpuLoad", new AttributeValue().withN(Double.toString(cpuLoad)));
        item.put("memoryUsed", new AttributeValue().withN(Double.toString(memoryUsed)));
        item.put("vmHealthy", new AttributeValue().withBOOL(vmHealthy));
        item.put("ip", new AttributeValue().withS(ip));
        item.put("threadId", new AttributeValue().withN(Long.toString(threadId)));
        item.put("typeOfRequest", new AttributeValue().withS(requestedURI));
        item.put("sizeOfInput", new AttributeValue().withN(Integer.toString(sizeOfInput)));
        item.put("score", new AttributeValue().withN(Double.toString(score)));
        return item;
    }
    
}

