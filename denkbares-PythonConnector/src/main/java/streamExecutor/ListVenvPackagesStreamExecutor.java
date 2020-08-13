package streamExecutor;

import commandLine.CommandLineExecutor;
import commandLine.CommandLineStreamHandler;
import python.venv.PythonDependency;
import python.venv.PythonVirtualEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListVenvPackagesStreamExecutor implements CommandLineStreamHandler {


    private final PythonVirtualEnvironment venv;

    private List<PythonDependency> dependencies = new ArrayList<>();

    public ListVenvPackagesStreamExecutor(PythonVirtualEnvironment venv) {
        this.venv = venv;
    }

    public List<PythonDependency> execute() {

        //clean dependencies
        dependencies.clear();
        CommandLineExecutor commandLineExecutor = new CommandLineExecutor(venv.getExecutablePath(), "-m", "pip", "list");
        commandLineExecutor.streamHandler = this;
        try {
            commandLineExecutor.execute();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return Collections.unmodifiableList(dependencies);
    }

    @Override
    public void onError(String errorLine) {

    }

    @Override
    public void onConsoleOutputChanged(String line) {
        //2 lines header!
        if (line.trim().startsWith("Package") && line.trim().endsWith("Version")) return;
        if (line.startsWith("------")) return;

        String[] split = line.split(" ");

        if (split.length < 2) return;

        dependencies.add(new PythonDependency(split[0], "==", split[split.length - 1]));
    }
}
