package commandLine;

public interface CommandLineStreamHandler {

    void onError(String errorLine);

    void onConsoleOutputChanged(String newLine);

}
