package streamExecutor;

import commandLine.CommandLineStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleLogStreamHandler implements CommandLineStreamHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void onError(String error) {
        System.err.println(error);
    }

    @Override
    public void onConsoleOutputChanged(String newLine) {
        System.out.println(newLine);
    }
}
