# Java Connector to create a Python Virtual Environment

The purpose of this project is to manage Python Virtual Environments from Java.


## Preliminaries about Python projects and Dependency Management in Python

Python does not build on the philosophy to configure a project directly, instead it allows you to define so-called virtual environments. A virtual environment can be considered as a separate python, that you got running on your pc.
However there is a major difference between the two. You can now either choose to install a package (from now on dependency) into either your system python or into any virtual environment.
This comes in handy, since now you can simply create a separate virtual environment for every project, and install the dependencies into the specific virtual environment. In general, this means that as long as you provide a separate environment per project, you have a similar dependency mechanism as in Java.
Since every virtual environment has its own interpreter executable, you just need to make sure, to use the correct executable and you are good to go. The power of this connector is to provide a lightweight wrapper around a virtual environment.
E.g. you can create it, install dependencies and other preliminaries and execute command or python scripts using the interpreter of the environment.

--------
What you need to have in order to use this connector is the path to a Python interpreter.
If this requirement is satisfied, you can continue to create a PythonEnvironmentConnector, which is by itself a very lightweight operation, so you can store this object without an issue.
```java
PythonEnvironmentConnector connector = new PythonEnvironmentConnector("ENVIRONMENT_NAME", "ENVIRONMENT_PATH", "EXECUTABLE_PATH")
```

You can subsequently command the Connector to create a Virtual Environment:

```java
PythonVirtualEnvironment pythonVirtualEnvironment = connector.initializeEnvironment();
```

This call will create a new Python virtual environment and return an object that you can use to manage that environment.

### Install Dependencies

Probably the most common use case is to install dependencies into your fresh environment. You can do this using : `public void installDependency(PythonDependency dependency) {`

A PythonDependency in this situation is just a tripel consistings of a `(<DEPENDENCY_NAME>,<COMPARATOR>,<VERSION>)`. If no comparator (=,>=) is specified, it is installing the latest dependency from `pip`.

In order to install `numpy` you can therefore call:

```java
 pythonVirtualEnvironment.installDependency(new PythonDependency("numpy"));
```

### Execute a Python script

Aside from installing a virtual environment, you can use this connector to execute python scripts using the interpreter of your virtual environment. This is done using the function:

```java
public void executeScript(InputStream scriptSteam, String scriptName, CommandLineStreamHandler streamHandler, String... args) {
```

The stream is the stream pointing to the python file you want to execute. Being a stream it does enable you to execute scripts from a bundled jar. The next argument is the scriptname (excluding any trailing .py).
The third argument is a `CommandLineStreamHandler`, which allows you to get callbacks from the console of your python script. The arguments `args` are passed in order you present them.

### Execute a custom command on your Virtual Environment

If just installing dependencies into your environment is not good enough, then you can also execute a custom command on your environment. E.g. if you want to install spacy, you also need to download the models (`python -m spacy download en_core_web_sm`).

This could be done using:

```java
pythonVirtualEnvironment.executePythonCommand("-m", "spacy", "download", "en_core_web_sm");
```
This executes the command and directs the default stream and error stream to the console. 


## Writing a custom CommandLineStreamHandler

This handler allows you to specify callbacks and deal with the results of your programm (e.g. annotate any results)

```java
public interface CommandLineStreamHandler {

    void onError(String errorLine);

    void onConsoleOutputChanged(String newLine);

}
```


