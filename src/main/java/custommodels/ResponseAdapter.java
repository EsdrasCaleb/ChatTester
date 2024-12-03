package custommodels;

import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ResponseAdapter {
    public static String AdaptHugging(String originalJson) {
        // Parse the JSON
        JSONObject jsonObject = new JSONObject(originalJson);

        // Remove unwanted keys
        jsonObject.remove("system_fingerprint");
        //jsonObject.remove("usage");

        // Also remove the "logprobs" key inside each choice if necessary
        JSONArray choices = jsonObject.getJSONArray("choices");
        for (int i = 0; i < choices.length(); i++) {
            JSONObject choice = choices.getJSONObject(i);
            choice.remove("logprobs");
        }

        // Output the transformed JSON
        return jsonObject.toString();
    }

    public static String AdaptGoogle(String originalJson) {
        try{
            // Parse the original JSON
            JsonObject originalObject = JsonParser.parseString(originalJson).getAsJsonObject();

            // Create the transformed JSON structure
            JsonObject transformedObject = new JsonObject();
            transformedObject.addProperty("object", "chat.completion");
            transformedObject.addProperty("id", "");
            transformedObject.addProperty("created", 1731291407);
            transformedObject.addProperty("model", originalObject.get("modelVersion").getAsString());
            
            // Build the choices array
            JsonArray choicesArray = new JsonArray();
            JsonObject choiceObject = new JsonObject();
            choiceObject.addProperty("index", 0);

            JsonObject messageObject = new JsonObject();
            messageObject.addProperty("role", "assistant");

            // Extract content text
            String contentText = originalObject.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject().getAsJsonObject("content")
                    .getAsJsonArray("parts").get(0).getAsJsonObject()
                    .get("text").getAsString();
            messageObject.addProperty("content", contentText);

            choiceObject.add("message", messageObject);
            choiceObject.addProperty("finish_reason", "stop");
            choicesArray.add(choiceObject);
            transformedObject.add("choices", choicesArray);

            // Create usage object
            JsonObject usageObject = new JsonObject();
            usageObject.addProperty("prompt_tokens", originalObject.getAsJsonObject("usageMetadata").get("promptTokenCount").getAsInt());
            usageObject.addProperty("completion_tokens", originalObject.getAsJsonObject("usageMetadata").get("candidatesTokenCount").getAsInt());
            usageObject.addProperty("total_tokens", originalObject.getAsJsonObject("usageMetadata").get("totalTokenCount").getAsInt());
            transformedObject.add("usage", usageObject);

            return transformedObject.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}
