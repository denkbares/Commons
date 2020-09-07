package python.venv;


import commandLine.CommandLineExecutor;
import streamExecutor.CreateVenvStreamExecutor;

import java.io.IOException;
import java.util.List;

public class PythonEnvironmentConnector {

    public final String environmentName;
    public final String environmentPath;
    public final String pythonExecutablePath;
    public final List<PythonDependency> dependencies;


    public PythonEnvironmentConnector(String environmentName, String environmentPath, String pythonExecutablePath, List<PythonDependency> dependencies) {
        this.environmentName = environmentName;
        this.environmentPath = environmentPath;
        this.pythonExecutablePath = pythonExecutablePath;
        this.dependencies = dependencies;
    }

    public PythonVirtualEnvironment initializeEnvironment() {
        //the first thing we do is to check if the environment does already exist! TODO do this gracefully

        //the we create the environment
        PythonVirtualEnvironment vEnv = createVirtualEnv();

        //activate the venv TODO im not so sure if this is required
        vEnv.activate();

        //and then we install the requirements using pip
        dependencies.forEach(dependency -> vEnv.installDependency(dependency));

        return vEnv;
    }


    private PythonVirtualEnvironment createVirtualEnv() {

        //build the vEnvPath from name and the path
        String venvPath = environmentPath + "/" + environmentName;

        //this can execute the command
        CommandLineExecutor executor = new CommandLineExecutor(pythonExecutablePath, "-m", "venv", venvPath);
        try {
            //but we have another layer, which also watches the stream and handles errors specific to the task of creating a venv
            new CreateVenvStreamExecutor(executor).execute();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return new PythonVirtualEnvironment(venvPath);
    }


}
