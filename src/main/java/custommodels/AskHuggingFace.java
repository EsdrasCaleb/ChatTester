package custommodels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.ChatResponse;
import zju.cst.aces.dto.Message;
import zju.cst.aces.util.AskGPT;
import java.net.HttpURLConnection;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Paths;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

public class AskHuggingFace extends AskGPT {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private String model;

    public AskHuggingFace(Config config, String modelName) {
        super(config);
        model = modelName;
    }

    @Override
    public ChatResponse askChatGPT(List<Message> chatMessages) {
        // Concatenate chat messages to form a single prompt
        StringBuilder inputText = new StringBuilder();
        for (Message message : chatMessages) {
            inputText.append(message.getContent()).append(" ");
        }

        try {
            
            String prompt = inputText.toString();
            String result = this.generateText(prompt);
            config.getLog().info("Generated response: " + result);

            ChatResponse chatResponse = GSON.fromJson(result, ChatResponse.class);
            return chatResponse;

        } catch (Exception e) {
            config.getLog().error("Failed to generate response with Hugging Face model: " + e.toString());
            return null;
        }
    }

    public String generateText(String prompt) throws Exception {
        URL url = new URL("https://api-inference.huggingface.co/models/"+model);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("TOKEN");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try {
            // JSON payload
            String payload = String.format("{"+
                "\"model\":\""+model+"\","+
                "\"messages\":{\"role\":\"user\",\"content\": \"%s\"}},"+
                "\"max_tokens\": 1000,"+
                "\"stream\": true}", prompt);
                config.getLog().info("Sent body: " + payload.toString());
            conn.getOutputStream().write(payload.getBytes());
        } catch (Exception e) {
            config.getLog().error("Connection Error: " + e.toString());
        }

        // Read the response
        Scanner scanner = new Scanner(conn.getInputStream());
        StringBuilder response = new StringBuilder();
        while (scanner.hasNextLine()) {
            response.append(scanner.nextLine());
        }
        scanner.close();
        conn.disconnect();
        config.getLog().info("Gotten Response: " + response.toString());
        return response.toString();
    }

}
