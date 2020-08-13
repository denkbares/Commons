package streamExecutor;

import commandLine.CommandLineExecutor;
import commandLine.CommandLineStreamHandler;

import java.io.IOException;

public class CreateVenvStreamExecutor implements CommandLineStreamHandler {

    public final CommandLineExecutor executor;

    public CreateVenvStreamExecutor(CommandLineExecutor executor) {
        this.executor = executor;
        executor.streamHandler = this;
    }

    public void execute() throws IOException, InterruptedException {
        executor.execute();
    }

    @Override
    public void onError(String error) {

    }

    @Override
    public void onConsoleOutputChanged(String newLine) {
        if (newLine.trim().startsWith("Requirement already satisfied:")) return;
        System.out.println(newLine);
    }
}
