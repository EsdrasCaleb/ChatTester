package custommodels;

import zju.cst.aces.api.Generator;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.Message;
import zju.cst.aces.dto.ChatResponse;
import zju.cst.aces.runner.AbstractRunner;
import zju.cst.aces.util.AskGPT;
import zju.cst.aces.util.CodeExtractor;
import zju.cst.aces.api.impl.ChatGenerator;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.List;

public class ModelChatGenerator extends ChatGenerator {

    public ModelChatGenerator(Config config) {
        super(config);
    }
   
    public static ChatResponse chat(Config config, List<Message> chatMessages) {
        Dotenv dotenv = Dotenv.load();
        String choice = dotenv.get("GENERATE");
        ChatResponse response;
        if (choice.equals("hugging")) {
            String model = dotenv.get("MODEL");
            config.getLog().info("Calling model "+model);
            response = new AskHuggingFace(config,model).askChatGPT(chatMessages);
        }
        else{
            config.getLog().info("Calling google");
            response = new AskGoogle(config).askChatGPT(chatMessages);
        }
        if (response == null) {
            throw new RuntimeException("Response is null, failed to get response.");
        }
        return response;
    }
}
