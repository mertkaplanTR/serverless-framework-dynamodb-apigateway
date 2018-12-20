package com.revolut.service;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.Writer;
import java.util.HashMap;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;
public class PutService implements RequestStreamHandler {
    JSONParser parser = new JSONParser();
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        String name = "";
        String responseCode = "204";
        String birthdate="";
        try {
            JSONObject event = (JSONObject)parser.parse(reader);
            if (event.get("pathParameters") != null) {
                JSONObject pps = (JSONObject)event.get("pathParameters");
                if ( pps.get("name") != null) {
                    name = (String)pps.get("name");
                }
            }
            if (event.get("body") != null) {
                JSONObject body = (JSONObject)parser.parse((String)event.get("body"));
                if ( body.get("dateOfBirth") != null) {
                    birthdate = (String)body.get("dateOfBirth");
                }
            }
            AmazonDynamoDB client = AmazonDynamoDBAsyncClientBuilder.standard()
                    .withRegion(Regions.US_WEST_1)
                    .build();
            String  table_name="revolutuser";
            HashMap<String,AttributeValue> item =
                    new HashMap<String,AttributeValue>();
            item.put("name", new AttributeValue(name));
            item.put("birthdate", new AttributeValue(birthdate));
            client.putItem(table_name, item);
            JSONObject responseBody = new JSONObject();
            JSONObject headerJson = new JSONObject();
            responseJson.put("isBase64Encoded", false);
            responseJson.put("statusCode", responseCode);
            responseJson.put("headers", headerJson);
            responseJson.put("body", responseBody.toString());
        } catch(Exception pex) {
            responseJson.put("statusCode", "400");
            responseJson.put("exception", pex);
        }
        logger.log(responseJson.toJSONString());
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());
        writer.close();
    }
}