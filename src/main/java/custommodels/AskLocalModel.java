package custommodels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ai.onnxruntime.*;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.ChatResponse;
import zju.cst.aces.dto.Message;
import zju.cst.aces.util.AskGPT;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AskLocalModel extends AskGPT {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private OrtEnvironment env;
    private OrtSession session;

    public AskLocalModel(Config config, String modelPath) {
        super(config);
        // Initialize environment and session for ONNX model
        try {
        env = OrtEnvironment.getEnvironment();
        session = env.createSession(modelPath, new OrtSession.SessionOptions());
        } catch (Exception e) {
            config.getLog().error("Onix Creation Failed: " + e);
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
            String result = generateText(inputText.toString());
            ChatResponse chatResponse = GSON.fromJson(result, ChatResponse.class);
            return chatResponse;
        } catch (Exception e) {
            config.getLog().error("Text generation failed: " + e);
            return null;
        }
    }

    private String generateText(String inputText) throws OrtException {
        // Convert input text to ONNX-compatible tensor (this assumes tokenized input)
        Map<String, OnnxTensor> inputMap = new HashMap<>();
        OnnxTensor inputIds = OnnxTensor.createTensor(env, tokenize(inputText));
        inputMap.put("input_ids", inputIds);

        // Run the inference session
        OrtSession.Result result = session.run(inputMap);
        OnnxTensor output = (OnnxTensor) result.get(0);

        // Decode output tokens into a readable string
        return decode(output);
    }

    private long[] tokenize(String inputText) {
        // Implement tokenization based on your model's tokenizer here
        // Example: Convert string to token IDs (replace this with your actual tokenizer logic)
        return new long[] { 101, 2009, 2003, 1037, 3899, 102 };
    }

    private String decode(OnnxTensor output) {
        // Convert output token IDs to a string
        long[] outputIds = output.getLongBuffer().array();
        StringBuilder decoded = new StringBuilder();
        for (long id : outputIds) {
            decoded.append(id).append(" "); // Example; replace with vocab lookup
        }
        return decoded.toString();
    }

    public void close() throws OrtException {
        if (session != null) session.close();
        if (env != null) env.close();
    }
}
