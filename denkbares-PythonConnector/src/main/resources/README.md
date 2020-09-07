# Java Connector to create a Python Virtual Environment

The purpose of this project is to manage Python Virtual Environments from Java.
What you need to have in order to use this connector is the path to a Python interpreter.
If this requirement is satisfied, you can continue to create a PythonEnvironmentConnector, which is by itself a very lightweight operation, so you can store this object without an issue.

## Preliminaries about Python projects and Dependency Management in Python

Python does not build on the philosophy to configure a project directly, instead it allows you to define so-called virtual environments. A virtual environment can be considered as a separate python, that you got running on your pc.
However there is a major difference between the two. You can now either choose to install a package (from now on dependency) into either your system python or into any virtual environment.
This comes in handy, since now you can simply create a separate virtual environment for every project, and install the dependencies into the specific virtual environment. In general, this means that as long as you provide a separate environment per project, you have a similar dependency mechanism as in Java.
Since every virtual environment has its own interpreter executable, you just need to make sure, to use the correct executable and you are good to go. The power of this connector is to provide a lightweight wrapper around a virtual environment.
E.g. you can create it, install dependencies and other preliminaries and execute command or python scripts using the interpreter of the environment.


```java
PythonEnvironmentConnector connector = PythonEnvironmentConnector("ENVIRONMENT_NAME", "ENVIRONMENT_PATH", "EXECUTABLE_PATH")
```

You can subsequently command the Connecttor to create a Virtual Environment:

```java
PythonVirtualEnvironment pythonVirtualEnvironment = connector.initializeEnvironment();
```

This call will create a new Python virtual environment and return an object that you can use to manage that environment. If you want to reuse an existing virtual environment you can just use the primary constructor of `PythonVirtualEnvironment`, which allows you to create a venv with just the path to it (to the folder with the name of your venv)

### Install Dependencies

Probably the most common use case is to install dependencies into your fresh environment. You can do this using : `public void installDependency(PythonDependency dependency)`

A PythonDependency in this situation is just a tripel consistings of a `(<DEPENDENCY_NAME>,<COMPARATOR>,<VERSION>)`. If no comparator (=,>=) is specified, it is installing the latest dependency from `pip`.

In order to install `numpy` you can therefore call:

```java
 pythonVirtualEnvironment.installDependency(new PythonDependency("numpy"));
```

If you have a `requirements.txt`, you can also use this to install dependencies via:

```java
 pythonVirtualEnvironment.installDependenciesFromRequirementsTxt("<PathToReqTxt>");
```

### Execute a Python script

Aside from installing a virtual environment, you can use this connector to execute python scripts using the interpreter of your virtual environment. There are two function for doing this, the first one is:

```java
public void executeScript(InputStream scriptSteam, String scriptName, CommandLineStreamHandler streamHandler, String... args) {
```

The stream is the stream pointing to the python file you want to execute. Being a stream it does enable you to execute scripts from a bundled jar. The next argument is the scriptname (excluding any trailing .py).
The third argument is a `CommandLineStreamHandler`, which allows you to get callbacks from the console of your python script. The arguments `args` are passed in order you present them.

The main drawback of this function is the fact that it can only execute scripts that do not require other files in the jar file (i.e. the complete script is contained in a single file). In case your script is split into several files you need to use the second method provided: 

```java
public void executeScript(String scriptName, String scriptsFolderInJar, String pathToJar, CommandLineStreamHandler streamHandler, String... args) {
```

`scriptName` is the name of the main script file you want to execute. `scriptsFolderInJar` is the folder within the jar that contains all your script files (script files outside of this folder will not be extracted and can therefore not be found by Python). `pathToJar` is the location of the jar file that contains the script file. See below for how to find this path. The remaining arguments (`streamHandler` and `args`) are the same as in the other executeScript function.

The location of the jar file can be found with the following function:
```java
public String getPathToJar()
```
Note though, that this only works if the PythonConnector project is bundled in your Plugin's jar file (by specifying `<scope>compile</scope>` in the Plugin's `pom.xml`). If this is not the case (e.g. `<scope>provided</scope>`) you need to call the following directly in your Plugin's code:
```java
String pathToJar = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
```

### Execute a custom command on your Virtual Environment

If just installing dependencies into your environment is not good enough, then you can also execute a custom command on your environment. E.g. if you want to install spacy, you also need to download the models (`python -m spacy download en_core_web_sm`).

This could be done using:

```java
pythonVirtualEnvironment.executePythonCommand("-m", "spacy", "download", "en_core_web_sm");
```
This executes the command and directs the default stream and error stream to the console. 

## Interactive Session

If executing a script or executing your project is not enough, you can also start an interactive session. Usig the interctive session you can execute alot of commands without your old ones to be forgotten, so you could for instance load a model once, and reuse it for the entire lifetime. An interactive sessions can be thought of having a python console in front of you, with which you communicate.

The first step is to create an interactive session:

```java
 InteractiveSession interactiveSession = pythonVirtualEnvironment.interactiveSession();
 ```
 This can be obtained from any previously created venv. Once you got this, you can send your commands to the session:

```java
 interactiveSession.execute("a = [0.5*x for x in range(0,10000000)]",new ConsoleLogStreamHandler());
 interactiveSession.execute("a",new ConsoleLogStreamHandler());
```
This example creates a rather huge list and then prints it to stdout. Note that even thought you seem to execute the commands immediately after another, the session takes care of all the waiting until python finished the last command you sent it. You can yet again trace all error/input by passing a StreamHandler (see the according section). At this point, i can not guarantee that you can simply pass any of your command which require more than a single line, however the following will work:

```java
        String script ="for i in range(3):\n" +
                "    print(\"Python is cool\")\n" +
                "";
        interactiveSession.execute(script,new ConsoleLogStreamHandler());  
```
And you will receive 3 lines with `Python is cool`.

Since what you probably want it to call some functions from your script, so you have to somehow load your script into the console. For this, there is a dedicated function `importScript`, which you can invoke as follows:

```java
 interactiveSession.importScript("src\\main\\resources\\webathen.py","wa",new ConsoleLogStreamHandler());
 ```

 What this does behind the scenes is to first append to the search path, and then `import webathen as wa`, so you could then use all functions of that script. If you need to import some files in a specific order, then you need to get that right on your end, as currently there is no support for such a feature. If you have your stuff from inside a `.jar`, you can also call this function with an `InputStream` in order to load your module.

When you are done with your work, do not forget to close the interactive session:

```java
interactiveSession.closeSession();

```
    

## Writing a custom CommandLineStreamHandler

This handler allows you to specify callbacks and deal with the results of your programm (e.g. annotate any results)

```java
public interface CommandLineStreamHandler {

    void onError(String errorLine);

    void onConsoleOutputChanged(String newLine);

}
```
