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

public class VMState_DB {

    private static String AWS_REGION = "us-east-2";
    private static AmazonDynamoDB dynamoDB;

    public static void main(String[] args) {
        if (args.length != 5) {
            System.err.println("Usage: java pt.ulisboa.tecnico.cnv.db_handler.VMState_DB <cpuLoad> <memoryUsed> <vmHealthy> <ip> <threadId>");
            System.exit(1);
        }

        double cpuLoad = Double.parseDouble(args[0]);
        double memoryUsed = Double.parseDouble(args[1]);
        boolean vmHealthy = Boolean.parseBoolean(args[2]);
        String ip = args[3];
        long threadId = Long.parseLong(args[4]);

        dynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .withRegion(AWS_REGION)
            .build();

        try {
            String tableName = "vm_state";

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
            dynamoDB.putItem(new PutItemRequest(tableName, newItem(getNextId(tableName), cpuLoad, memoryUsed, vmHealthy, ip, threadId)));

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

    private static Map<String, AttributeValue> newItem(long id, double cpuLoad, double memoryUsed, boolean vmHealthy, String ip, long threadId) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("id", new AttributeValue().withN(Long.toString(id)));
        item.put("cpuLoad", new AttributeValue().withN(Double.toString(cpuLoad)));
        item.put("memoryUsed", new AttributeValue().withN(Double.toString(memoryUsed)));
        item.put("vmHealthy", new AttributeValue().withBOOL(vmHealthy));
        item.put("ip", new AttributeValue().withS(ip));
        item.put("threadId", new AttributeValue().withN(Long.toString(threadId)));
        return item;
    }
}
