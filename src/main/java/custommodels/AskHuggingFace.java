package custommodels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ai.djl.Model;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.translate.Batchifier;
import ai.djl.translate.TranslatorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.huggingface.translator.QuestionAnsweringTranslatorFactory;
import ai.djl.translate.TranslatorContext;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.ChatResponse;
import zju.cst.aces.dto.Message;
import zju.cst.aces.util.AskGPT;
import ai.djl.modality.nlp.qa.QAInput;
import ai.djl.training.util.ProgressBar;

import java.io.IOException;
import java.util.List;

public class AskHuggingFace extends AskGPT {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private Predictor<String, String> predictor;
    private ZooModel<String, String> model;

    public AskHuggingFace(Config config, String modelName, String authToken) {
        super(config);
        System.setProperty("DJL_HTTP_AUTH_TOKEN", authToken); // Set the Hugging Face API token

        try {

            // Define criteria for model loading using modelName and a text generation translator
            Criteria<String, String> criteria = Criteria.builder()
            .setTypes(String.class, String.class)
            .optModelUrls(
                    "https://huggingface.co/"+modelName)
            .optEngine("PyTorch")
            .optTranslatorFactory(new QuestionAnsweringTranslatorFactory())
            .optProgress(new ProgressBar())
            .build();

            // Load model using criteria
            model = criteria.loadModel();
            predictor = model.newPredictor();

        } catch (Exception e) {
            config.getLog().error("Failed to load Hugging Face model: " + e);
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
            String result = predictor.predict(inputText.toString());
            config.getLog().info("Generated response: " + result);

            ChatResponse chatResponse = GSON.fromJson(result, ChatResponse.class);
            return chatResponse;

        } catch (TranslateException e) {
            config.getLog().error("Failed to generate response with Hugging Face model: " + e);
            return null;
        }
    }

    // Clean up resources
    public void close() {
        if (predictor != null) {
            predictor.close();
        }
        if (model != null) {
            model.close();
        }
    }
}
