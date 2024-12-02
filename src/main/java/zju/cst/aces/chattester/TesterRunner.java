package zju.cst.aces.chattester;

import zju.cst.aces.api.Runner;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.MethodInfo;
import zju.cst.aces.runner.ClassRunner;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;

public class TesterRunner implements Runner {

    Config config;

    public TesterRunner(Config config) {
        this.config = config;
    }

    public void runClass(String fullClassName) {
        config.getLog().info("\nRUNCLASS \n");
        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get("GPT_KEY");
        config.setApiKeys( new String[]{apiKey});
        try {
            //TODO: use TesterMethodRunner in ClassRunner.
            new ClassRunner(config, fullClassName).start(); 
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void runMethod(String fullClassName, MethodInfo methodInfo) {
        config.getLog().info("\nRUNMETHOD \n");
        try {
            new TesterMethodRunner(config, fullClassName, methodInfo).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
