package custommodels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.Message;
import zju.cst.aces.dto.ChatResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import zju.cst.aces.util.AskGPT;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

public class AskGoogle extends AskGPT {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();


    private static final String GEMINI_API_URL = "https://api.gemini.com/v1/query"; // Placeholder URL
    private static final String API_KEY = "YOUR_GEMINI_API_KEY"; // Set your API key

    public AskGoogle(Config config) {
        super(config);
    }

    @Override
    public ChatResponse askChatGPT(List<Message> chatMessages) {
        StringBuilder inputText = new StringBuilder();
        for (Message message : chatMessages) {
            inputText.append(message.getContent()).append(" ");
        }

        try {
            String response = queryGeminiAPI(inputText.toString());
            config.getLog().info(response);
            ChatResponse chatResponse = GSON.fromJson(response, ChatResponse.class);
            return chatResponse;
        } catch (IOException e) {
            config.getLog().error("Failed to query Gemini API: " + e);
            return null;
        }
    }

    private String queryGeminiAPI(String inputText) throws IOException {
        URL url = new URL(GEMINI_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // JSON payload
        String payload = String.format("{\"prompt\": \"%s\"}", inputText);
        conn.getOutputStream().write(payload.getBytes());

        // Read the response
        Scanner scanner = new Scanner(conn.getInputStream());
        StringBuilder response = new StringBuilder();
        while (scanner.hasNextLine()) {
            response.append(scanner.nextLine());
        }
        scanner.close();
        conn.disconnect();

        return response.toString();
    }
}
