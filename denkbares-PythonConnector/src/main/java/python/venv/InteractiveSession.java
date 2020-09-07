package python.venv;

import commandLine.CommandLineStreamHandler;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import streamExecutor.ConsoleLogStreamHandler;
import streamExecutor.DefaultStreamHandler;

import java.io.*;

public class InteractiveSession {

    public static final String KEYWORD_CONTINUE = "KEYWORD_CONTINUE";

    private final InputStreamGobbler inputStreamGobbler;
    private final InputStreamGobbler errorStreamGobbler;
    private Process process;

    private final Logger logger;

    public InteractiveSession(Process process) {

        this.process = process;
        this.inputStreamGobbler = new InputStreamGobbler(process.getInputStream(), new DefaultStreamHandler(), "input");
        this.errorStreamGobbler = new InputStreamGobbler(process.getErrorStream(), new DefaultStreamHandler(), "error");

        //start them
        this.inputStreamGobbler.start();
        this.errorStreamGobbler.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger = LoggerFactory.getLogger(getClass());

    }

    public void execute(String script, CommandLineStreamHandler streamHandler) {
        //swap the handler
        this.inputStreamGobbler.handler = streamHandler;
        this.errorStreamGobbler.handler = streamHandler;

        waitUntilLastJobIsCompleted();

        System.out.println("Execute: " + script);
        logger.debug("Execute: " + script);

        writeToProcess(script);
    }

    private void writeToProcess(String script) {
        OutputStream outputStream = process.getOutputStream();
        try {
            this.inputStreamGobbler.isBusy = true;
            outputStream.write((script + System.lineSeparator() + "print('KEYWORD_CONTINUE')" + System.lineSeparator()).getBytes());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void closeSession() {
        waitUntilLastJobIsCompleted();
        execute("exit", new DefaultStreamHandler());
        process.destroy();
        this.errorStreamGobbler.interrupt();
        this.inputStreamGobbler.interrupt();
    }

    private void waitUntilLastJobIsCompleted() {
        while (this.inputStreamGobbler.isBusy) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Takes care that a python script of yours is loaded into the active session
     *
     * @param pathToScript
     * @param alias
     * @param streamHandler
     */
    public void importScript(String pathToScript, String alias, CommandLineStreamHandler streamHandler) {
        //first we import sys
        execute("import sys", streamHandler);
        //then we append the parent folder to sys.path
        String directoryString = new File(pathToScript).getParentFile().getAbsolutePath();
        execute("sys.path.insert(1,r'" + directoryString + "')", streamHandler);
        //then import the script
        String aliasString = alias != null ? " as " + alias : "";
        execute("import " + new File(pathToScript).getName().replaceAll(".py", "") + aliasString, streamHandler);
    }

    public void importScript(InputStream isScript, String scriptName, String alias, CommandLineStreamHandler streamHandler) {
        File tempFile = PythonVirtualEnvironment.readStreamToFile(isScript, scriptName, "py");
        importScript(tempFile.getAbsolutePath(), alias, streamHandler);
    }


    static class InputStreamGobbler extends Thread {
        public CommandLineStreamHandler handler;
        InputStream is;
        String type;

        boolean isBusy = false;

        public InputStreamGobbler(InputStream is, CommandLineStreamHandler handler, String type) {
            this.is = is;
            this.handler = handler;
            this.type = type;
        }

        @Override
        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (type.equals("input")) {
                        if (line.equals(KEYWORD_CONTINUE)) {
                            this.isBusy = false;
                        } else {
                            handler.onConsoleOutputChanged(line);
                        }
                    } else handler.onError(line);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

}
