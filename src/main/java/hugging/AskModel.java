package hugging;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import zju.cst.aces.api.Config;
import zju.cst.aces.api.AskGPT;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import ai.djl.Application;
import ai.djl.Model;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.nlp.NlpModel;
import ai.djl.modality.nlp.TextEmbedding;
import ai.djl.modality.nlp.translator.TextTranslator;
import ai.djl.translate.TranslateException;
import zju.cst.aces.dto.ChatMessage;
import zju.cst.aces.dto.ChatResponse;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

public class AskModel extends AskGPT {
    private static final Logger logger = LogManager.getLogger(AskModel.class);
    private final String modelPath;
    private ZooModel<String, String> model;
    private HuggingFaceTokenizer tokenizer;
    private Predictor<String, String> predictor;

    public AskModel(Config config, String modelPath) {
        super(config);
        this.modelPath = modelPath;
        try {
            initializeModel();
        } catch (IOException e) {
            logger.error("Failed to initialize model: " + e.getMessage());
            throw new RuntimeException("Model initialization failed", e);
        }
    }

    private void initializeModel() throws IOException {
        Path path = Paths.get(modelPath);
        
        // Initialize the model
        model = ModelZoo.loadModel(path);
        predictor = model.newPredictor();
        
        // Initialize tokenizer
        tokenizer = HuggingFaceTokenizer.newInstance(path);
    }
    
    public ChatResponse askChatModel1(List<ChatMessage> chatMessages) {
        int maxTry = 5;
        while (maxTry > 0) {
            try {
                // Convert chat messages to a single prompt string
                String prompt = convertMessagesToPrompt(chatMessages);
                
                // Generate response using the local model
                String generatedText = predictor.predict(prompt);
                
                // Create and return ChatResponse object
                ChatResponse response = new ChatResponse();
                ChatChoice choice = new ChatChoice();
                Message message = new Message();
                message.setRole("assistant");
                message.setContent(generatedText);
                choice.setMessage(message);
                
                List<ChatChoice> choices = new ArrayList<>();
                choices.add(choice);
                response.setChoices(choices);
                
                return response;
                
            } catch (Exception e) {
                logger.error("Error in local model inference: " + e.getMessage());
                maxTry--;
                if (maxTry > 0) {
                    try {
                        Thread.sleep(1000); // Wait before retry
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry wait", ie);
                    }
                }
            }
        }
        
        logger.debug("AskModel: Failed to get response after maximum retries\n");
        return null;
    }

    public ChatResponse askModel2(List<ChatMessage> chatMessages, String model) {
        try (Model djlModel = Model.newInstance(model, Application.NLP.TEXT_GENERATION)) {
            TextTranslator translator = TextTranslator.builder()
                    .setSourceLanguage("en")
                    .setTargetLanguage("en")
                    .build();
            Predictor<String, String> predictor = djlModel.newPredictor(translator);
    
            // Combine chat messages into a prompt
            String prompt = chatMessages.stream()
                    .map(message -> message.getSender() + ": " + message.getContent())
                    .collect(Collectors.joining("\n")) + "\nGenerate automated test cases:";
    
            // Use the model to generate text
            String generatedText = predictor.predict(prompt);
    
            // Return the result in ChatResponse format
            ChatResponse response = new ChatResponse();
            response.setContent(generatedText);
            return response;
        } catch (ModelException | TranslateException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String convertMessagesToPrompt(List<ChatMessage> chatMessages) {
        StringBuilder prompt = new StringBuilder();
        for (ChatMessage message : chatMessages) {
            prompt.append(message.getRole())
                  .append(": ")
                  .append(message.getContent())
                  .append("\n");
        }
        return prompt.toString();
    }

    // New method to call Gemini API
    public @Nullable ChatResponse askGemini(List<ChatMessage> chatMessages) {
        String apiKey = config.getRandomKey();
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("messages", chatMessages);
            payload.put("model", "gemini");
            payload.put("temperature", config.getTemperature());

            String jsonPayload = GSON.toJson(payload);
            RequestBody body = RequestBody.create(MEDIA_TYPE, jsonPayload);
            Request request = new Request.Builder()
                    .url("https://api.gemini.com/v1/completions") // Replace with Gemini API endpoint
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            try (Response response = config.getClient().newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                if (response.body() == null) throw new IOException("Response body is null.");
                return GSON.fromJson(response.body().string(), ChatResponse.class);
            }
        } catch (IOException e) {
            config.getLogger().error("In AskGPT.askGemini: " + e);
            return null;
        }
    }

    @Override
    public void close() {
        try {
            if (predictor != null) {
                predictor.close();
            }
            if (model != null) {
                model.close();
            }
            if (tokenizer != null) {
                tokenizer.close();
            }
        } catch (Exception e) {
            logger.error("Error closing resources: " + e.getMessage());
        }
    }
}