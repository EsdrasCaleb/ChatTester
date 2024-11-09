package custommodels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ai.djl.ndarray.NDManager;
import ai.djl.Device;
import ai.djl.translate.Batchifier; 
import ai.djl.Application;
import ai.djl.Model;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.util.Utils;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslatorFactory;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.ChatResponse;
import zju.cst.aces.dto.Message;
import zju.cst.aces.util.AskGPT;

import java.nio.file.Paths;
import java.io.IOException;
import java.util.List;

public class AskHuggingFace extends AskGPT {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private String modelName;;
    private Model model;

    public AskHuggingFace(Config config, String modelName) {
        super(config);

        try {
            String authToken = "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";  // Add your token here

            // Set Hugging Face token for authorization
            System.setProperty("huggingface.token", authToken);
            this.modelName = modelName;



        } catch (Exception e) {
            config.getLog().error("Failed to load Hugging Face model: " + e.toString());
            config.getLog().error(Paths.get("/app/models/"+modelName).toString());
        }
    }

    @Override
    public ChatResponse askChatGPT(List<Message> chatMessages) {
        // Concatenate chat messages to form a single prompt
        StringBuilder inputText = new StringBuilder();
        for (Message message : chatMessages) {
            inputText.append(message.getContent()).append(" ");
        }

        try {
            // Generate the response using the model's predictor
            config.getLog().info("Messagem " + inputText.toString());
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
        // Use Criteria to load the model
        Criteria<String, String> criteria = Criteria.builder()
                .optApplication(Application.NLP.TEXT_GENERATION)
                .setTypes(String.class, String.class) // Input and Output types
                .optModelUrls("https://huggingface.co/meta-llama/Llama-3.2-3B-Instruct") // Local model path or URL
                .build();

        ZooModel<String, String> model = criteria.loadModel(); 
        Predictor<String, String> predictor = model.newPredictor();

        // Generate text
        String result = predictor.predict(prompt);
        return result;
    }

}
