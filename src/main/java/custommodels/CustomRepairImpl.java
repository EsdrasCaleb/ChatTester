package custommodels;

import zju.cst.aces.dto.ChatResponse;
import zju.cst.aces.dto.PromptInfo;
import zju.cst.aces.api.impl.PromptConstructorImpl;
import zju.cst.aces.api.impl.RepairImpl;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.runner.MethodRunner;

public class CustomRepairImpl extends RepairImpl{

    Config config;

    PromptConstructorImpl promptConstructorImpl;

    boolean success = false;
    public boolean isSuccess(){
        return this.success;
    }
    public CustomRepairImpl(Config config, PromptConstructorImpl promptConstructorImpl) {
        super(config,promptConstructorImpl);
        this.config = config;
        this.promptConstructorImpl = promptConstructorImpl;
    }
    @Override
    public String LLMBasedRepair(String code, int rounds) {
        PromptInfo promptInfo = promptConstructorImpl.getPromptInfo();
        promptInfo.setUnitTest(code);
        String fullClassName = promptInfo.getClassInfo().getPackageName() + "." + promptInfo.getClassInfo().getClassName();
        if (MethodRunner.runTest(config, promptConstructorImpl.getFullTestName(), promptInfo, rounds)) {
            config.getLog().info("Extraiu código");
            this.success = true;
            return code;
        }
        config.getLog().info("Não extraiu código");
        promptConstructorImpl.generate();
        if (promptConstructorImpl.isExceedMaxTokens()) {
            config.getLog().error("Exceed max prompt tokens: " + promptInfo.methodInfo.methodName + " Skipped.");
            return code;
        }
        ChatResponse response = ModelChatGenerator.chat(config, promptConstructorImpl.getMessages());
        String newcode = ModelChatGenerator.extractCodeByResponse(response);
        if (newcode.isEmpty()) {
            config.getLog().warn("Test for method < " + promptInfo.methodInfo.methodName + " > extract code failed");
            return code;
        } else {
            config.getLog().info("Gerou novo código");
            return newcode;
        }
    }
}
