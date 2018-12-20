package com.revolut.service;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

public class GetService implements RequestStreamHandler {
    JSONParser parser = new JSONParser();
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        String name = "";
        String responseCode = "200";
        try {
            JSONObject event = (JSONObject)parser.parse(reader);
            if (event.get("pathParameters") != null) {
                JSONObject pps = (JSONObject)event.get("pathParameters");
                if ( pps.get("name") != null) {
                    name = (String)pps.get("name");  //name is dynamodb column
                }
            }
            if (name.length()<=0) {
                //TODO if you need
            }
            String greeting = "Hello, " + name + "! "; //set
            String birthdate="";
            String table_name = "revolutuser";
            AmazonDynamoDB client = AmazonDynamoDBAsyncClientBuilder.standard()
                    .withRegion(Regions.US_WEST_1)
                    .build();
            HashMap<String, AttributeValue> item = new HashMap<>();
            item.put("name", new AttributeValue(name));
            GetItemRequest request = null;
            request = new GetItemRequest()
                    .withKey(item)
                    .withTableName(table_name);
            Map<String, AttributeValue> resultItem = client.getItem(request).getItem();
            if (resultItem == null) {
                System.out.print("Request not found");
            } else {
                Set<String> keys = resultItem.keySet();
                for (String key : keys) {
                    System.out.format("%s: %s\n",
                            key, resultItem.get(key).getS());
                    if (key == "birthdate") {
                        birthdate = resultItem.get(key).getS();
                        System.out.println("bulunan birthdate: " + birthdate);
                    }
                }

            }
            greeting = greeting + getNextBirthDateDaysLeft(birthdate);
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("message", greeting);
            JSONObject jsonHeaders = new JSONObject();
            jsonHeaders.put("Content-Type", "application/json");
            responseJson.put("statusCode", "200");
            responseJson.put("body", jsonBody.toString());
            responseJson.put("headers", jsonHeaders);
            responseJson.put("isBase64Encoded", false);

        } catch(ParseException pex) {
            responseJson.put("statusCode", "400");
            responseJson.put("exception", pex);
        }
        logger.log(responseJson.toJSONString());
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());
        writer.close();
    }
    public String getNextBirthDateDaysLeft(String birthDate) {
        String currentDate= DateTime.now().toString("yyyy-MM-dd");
        long diff = 0;
        String retMsg = "";
        DateTime currentDateDateTime = DateTime.parse(currentDate,
                DateTimeFormat.forPattern("yyyy-MM-dd"));
        DateTime enteredDateDateTime = DateTime.parse(birthDate,
                DateTimeFormat.forPattern("yyyy-MM-dd"));
        DateTime nextBirthDate = DateTime.parse((currentDateDateTime.getYear()+1)+"-"+enteredDateDateTime.getMonthOfYear()+"-"+enteredDateDateTime.getDayOfMonth(), DateTimeFormat.forPattern("yyyy-MM-dd"));
        DateTime thisYearBirthDate = DateTime.parse((currentDateDateTime.getYear())+"-"+enteredDateDateTime.getMonthOfYear()+"-"+enteredDateDateTime.getDayOfMonth(), DateTimeFormat.forPattern("yyyy-MM-dd"));
        if(currentDateDateTime.getMonthOfYear()>enteredDateDateTime.getMonthOfYear())
        {
            diff = thisYearBirthDate.getMillis() - currentDateDateTime.getMillis();
            retMsg = "Your birthday is in "+String.valueOf( (diff/(1000*60*60*24)) )+" days";
        }
        if(currentDateDateTime.getMonthOfYear() == enteredDateDateTime.getMonthOfYear())
        {
            if(currentDateDateTime.getDayOfMonth()>enteredDateDateTime.getDayOfMonth())
            {
                diff = thisYearBirthDate.getMillis() - currentDateDateTime.getMillis();
                retMsg = "Your birthday is in "+String.valueOf( (diff/(1000*60*60*24)) )+" days";
            }
        }
        if(currentDateDateTime.getDayOfMonth()==enteredDateDateTime.getDayOfMonth() && currentDateDateTime.getMonthOfYear() == enteredDateDateTime.getMonthOfYear())
        {
            retMsg = " Happy birthday!";
        } else
        {
            diff = nextBirthDate.getMillis() - currentDateDateTime.getMillis();
            retMsg = "Your birthday is in "+String.valueOf( (diff/(1000*60*60*24)) )+" days";
        }
        return retMsg;
    }
}