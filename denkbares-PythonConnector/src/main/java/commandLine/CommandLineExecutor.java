package commandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.stream.Collectors;

/**
 * Just a little wrapper around a Prcoess Builder
 */
public class CommandLineExecutor {

    public final String[] command;

    private ProcessBuilder processBuilder;

    public CommandLineStreamHandler streamHandler;

    public CommandLineExecutor(String[] command) {
        this.command = command;

       processBuilder= new ProcessBuilder();
    }

    //convenience constructor
    public CommandLineExecutor(String arg1, String... args) {
        command = new String[1 + args.length];
        command[0] = arg1;
        for (int i = 0; i < args.length; i++) {
            command[i+1] = args[i];
        }
        processBuilder = new ProcessBuilder();
    }

    public void execute() throws IOException, InterruptedException {

        //we create a temp file to direct any potentiel error output
        File tempFile = File.createTempFile("dummyLogFile", "txt");
        tempFile.deleteOnExit();

        processBuilder.command(command);
        processBuilder.redirectError(tempFile);
        Process process = processBuilder.start();

        // blocked :(
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            streamHandler.onConsoleOutputChanged(line);
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String errorString =Files.readAllLines(tempFile.toPath()).stream().collect(Collectors.joining());
            streamHandler.onError(errorString);

        } else {
            System.out.println("Task finished successfully!");
        }


    }


}
