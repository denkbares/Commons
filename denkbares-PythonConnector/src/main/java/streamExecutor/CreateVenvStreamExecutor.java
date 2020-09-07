package streamExecutor;

import commandLine.CommandLineExecutor;
import commandLine.CommandLineStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CreateVenvStreamExecutor implements CommandLineStreamHandler {

    public final CommandLineExecutor executor;
    private final Logger logger;

    public CreateVenvStreamExecutor(CommandLineExecutor executor) {
        this.executor = executor;
        executor.streamHandler = this;
        logger = LoggerFactory.getLogger(this.getClass());
    }

    public void execute() throws IOException, InterruptedException {
        executor.execute();
    }

    @Override
    public void onError(String error) {
        logger.error(error);
    }

    @Override
    public void onConsoleOutputChanged(String newLine) {
        if (newLine.trim().startsWith("Requirement already satisfied:")) return;
        logger.info(newLine);
    }
}
