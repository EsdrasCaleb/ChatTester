package custommodels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.api.config.ModelConfig;
import zju.cst.aces.dto.ChatResponse;
import zju.cst.aces.util.AskGPT;
import zju.cst.aces.dto.Message;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.github.cdimascio.dotenv.Dotenv;

public class AskHuggingFace extends AskGPT {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private String model;

    public AskHuggingFace(Config config, String modelName) {
        super(config);
        model = modelName;
    }

    @Override
    public ChatResponse askChatGPT(List<Message> chatMessages) {
        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get("TOKEN");
        int maxTry = 1;
        while (maxTry > 0) {
            Response response = null;
            try {
                Map<String, Object> payload = new HashMap<>();

                ModelConfig modelConfig = config.getModel().getDefaultConfig();

                payload.put("messages", chatMessages);
                payload.put("model", model);
                /*
                payload.put("temperature", config.getTemperature());
                payload.put("frequency_penalty", config.getFrequencyPenalty());
                payload.put("presence_penalty", config.getPresencePenalty());
                 */
                payload.put("max_tokens", config.getMaxResponseTokens());
                config.getLog().info("Tokens:"+config.getMaxResponseTokens());
                payload.put("stream", false);
                String jsonPayload = GSON.toJson(payload);
                
                RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonPayload);
                
                Request request = new Request.Builder().url("https://api-inference.huggingface.co/models/"+model+"/v1/chat/completions").post(body).addHeader("Content-Type", "application/json").addHeader("Authorization", "Bearer " + apiKey).build();
                
                response = config.getClient().newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                try {
                    Thread.sleep(config.sleepTime);
                } catch (InterruptedException ie) {
                    throw new RuntimeException("In AskGPT.askChatGPT: " + ie);
                }
                if (response.body() == null) throw new IOException("Response body is null.");
                String responseString = ResponseAdapter.AdaptHugging(response.body().string());
                config.getLog().info(responseString);
                ChatResponse chatResponse = GSON.fromJson(responseString, ChatResponse.class);
                config.getLog().info("Reponse: " + chatResponse.toString());
                response.close();
                return chatResponse;
            } catch (IOException e) {
                config.getLog().error("In AskGPT.askChatGPT: " + e);
                if (response != null) {
                    response.close();
                }
                config.getLog().error("In AskGPT.askChatGPT: " + e);
                maxTry--;
            }
            catch (Exception e) {
                config.getLog().error("Error in conversion: " + e);
            }
        }
        config.getLog().debug("AskGPT: Failed to get response\n");
        return null;
    }

}
