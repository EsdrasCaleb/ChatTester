package custommodels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ai.djl.Model;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.nlp.generate.TextGenerator;
import ai.djl.modality.nlp.generate.TextGenerationConfig;
import ai.djl.modality.nlp.generate.TextGenerationModel;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import ai.djl.training.util.ProgressBar;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.ChatResponse;
import zju.cst.aces.dto.Message;
import zju.cst.aces.util.AskGPT;

import java.nio.file.Paths;
import java.util.List;

public class AskLocalModel extends AskGPT {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private ZooModel<String, String> model;
    private Predictor<String, String> predictor;

    public AskLocalModel(Config config, String modelPath) {
        super(config);
        
        try {
            // Define criteria for loading the local model with text generation capabilities
            Criteria<String, String> criteria = Criteria.builder()
                .setTypes(String.class, String.class)
                .optModelPath(Paths.get(modelPath))       // Set path to the downloaded model directory
                .optProgress(new ProgressBar())
                .build();

            // Load the model and create a predictor for text generation
            model = criteria.loadModel();
            predictor = model.newPredictor();
        } catch (ModelException | IOException e) {
            config.getLog().error("Failed to load local model: " + e);
        }
    }

    @Override
    public ChatResponse askChatGPT(List<Message> chatMessages) {
        // Combine all messages into a single input string
        StringBuilder inputText = new StringBuilder();
        for (Message message : chatMessages) {
            inputText.append(message.getContent()).append(" ");
        }

        try {
            // Generate response using the local model predictor
            String result = predictor.predict(inputText.toString());
            ChatResponse chatResponse = GSON.fromJson(result, ChatResponse.class);
            return chatResponse;
            return response;
        } catch (TranslateException e) {
            config.getLog().error("Text generation failed: " + e);
            return null;
        }
    }

    // Close resources when done
    public void close() {
        if (predictor != null) {
            predictor.close();
        }
        if (model != null) {
            model.close();
        }
    }
}
