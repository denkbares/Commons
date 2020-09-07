package python.venv;

import commandLine.CommandLineExecutor;
import commandLine.CommandLineStreamHandler;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import streamExecutor.DefaultStreamHandler;
import streamExecutor.ListVenvPackagesStreamExecutor;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PythonVirtualEnvironment {

	public final String path;

	public final OperatingSystem operatingSystem;
	private final Logger logger;

	public PythonVirtualEnvironment(String path) {
		this.path = path;
		operatingSystem = getOperatingSystem();
		logger = LoggerFactory.getLogger(getClass());
	}

	public OperatingSystem getOperatingSystem() {
		String osName = System.getProperty("os.name");
		return OperatingSystem.fromString(osName);
	}

	public InteractiveSession interactiveSession() throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command(getExecutablePath(),"-i","-c","print('')");
		Process process = processBuilder.start();



		return new InteractiveSession(process);
	}

	public void executePythonCommand(String... args){
		//then build the command arguments
		List<String> command = new ArrayList<>();
		command.add(getExecutablePath());
		Arrays.stream(args).forEach(it -> command.add(it));

		//create the executor
		CommandLineExecutor commandLineExecutor = new CommandLineExecutor(command.toArray(new String[0]));
		commandLineExecutor.streamHandler = new DefaultStreamHandler();

		//and execute
		try {
			commandLineExecutor.execute();
		}
		catch (IOException | InterruptedException e) {
			logger.error("Problem!");
			e.printStackTrace();
		}

	}

	/**
	 * Execute a Python script.
	 * <br/>
	 * <b>Note:</b> This method can only deal with scripts that do not require other script files. If your plugin
	 * contains other script files that are needed for the execution of the given script, use
	 * {@link #executeScript(String, String, String, CommandLineStreamHandler, String...)} instead.
	 * @param scriptStream the script to be executed as an InputStream
	 * @param scriptName the name of the script file (the contents of the given InputStream are written to a temporary
	 *                   File with this name)
	 * @param streamHandler the CommandLineStreamHandler which receives the output of the script
	 * @param args the arguments that are passed to the script
	 */
	public void executeScript(InputStream scriptStream, String scriptName, CommandLineStreamHandler streamHandler,
							  String... args) {
		logger.info("Execute: " + scriptName);
		//we need to create a temp file for the inputstream
		File tempFile = readStreamToFile(scriptStream, scriptName, "py");

		//then build the command arguments
		List<String> command = new ArrayList<>();
		command.add(getExecutablePath());
		command.add(tempFile.getAbsolutePath());
		Arrays.stream(args).forEach(it -> command.add(it));

		//create the executor
		CommandLineExecutor commandLineExecutor = new CommandLineExecutor(command.toArray(new String[0]));
		commandLineExecutor.streamHandler = streamHandler;

		//and execute
		try {
			commandLineExecutor.execute();
		}
		catch (IOException | InterruptedException e) {
			logger.error("Unable to execute the script!");
			e.printStackTrace();
		}
	}

	/**
	 * Execute a Python script, which may require one or more other script files. All script files need to be located
	 * in a folder in the jar file (scriptsFolderInJar).
	 * <br/>
	 * Note: If the script you want to execute does <b>not</b> require other script files you can also call
	 * {@link #executeScript(InputStream, String, CommandLineStreamHandler, String...)}
	 * @param scriptName the name of the script file to be executed, including its ending (usually ".py")
	 * @param scriptsFolderInJar the name of the folder (within the jar file) which contains the script file and all
	 *                           other script files that are required
	 * @param pathToJar The location of the jar file that contains the scripts. Can be retrieved by
	 *                     calling {@link #getPathToJar()} if the plugin's jar contains the PythonConnector (see the
	 *                     method's javadoc for details and what to do if this is not the case).
	 * @param streamHandler the CommandLineStreamHandler which receives the output of the script
	 * @param args the arguments that are passed to the script
	 */
	public void executeScript(String scriptName, String scriptsFolderInJar, String pathToJar,
							  CommandLineStreamHandler streamHandler, String... args) {
		// extract the scripts from the jar file
		File scriptsFolder = extractScripts(pathToJar, scriptsFolderInJar);

		// get the script as a File object
		logger.info("Execute: " + scriptName);
		File tempFile = new File(scriptsFolder, scriptName);

		//then build the command arguments
		List<String> command = new ArrayList<>();
		command.add(getExecutablePath());
		command.add(tempFile.getAbsolutePath());
		command.addAll(Arrays.asList(args));

		//create the executor
		CommandLineExecutor commandLineExecutor = new CommandLineExecutor(command.toArray(new String[0]));
		commandLineExecutor.streamHandler = streamHandler;

		//and execute
		try {
			commandLineExecutor.execute();
		}
		catch (IOException | InterruptedException e) {
			logger.error("Unable to execute the script!");
			e.printStackTrace();
		}
	}

	/**
	 * Extract the the folder with the given name (and its contents) from the jar file that is located at the given
	 * path and return the extracted folder. The folder is created as a temporary directory.
	 * @param pathToJar The location of the jar file which contains the folder to be extracted. Can be retrieved by
	 *                     calling {@link #getPathToJar()} if the plugin's jar contains the PythonConnector (see the
	 *                     method's javadoc for details and what to do if this is not the case).
	 * @param scriptsFolderInJar The folder to be extracted from the jar file
	 * @return The extracted folder as a File object
	 */
	public File extractScripts(String pathToJar, String scriptsFolderInJar) {
		try {
			if (pathToJar == null) {
				pathToJar = getPathToJar();
			}
			// use the jar's name as the name of the temporary directory
			String tempName = pathToJar.substring(
					Math.max(pathToJar.lastIndexOf("/"), pathToJar.lastIndexOf("\\")) + 1);
			File scriptDirectory = Files.createTempDirectory(tempName).toFile();
			scriptDirectory.deleteOnExit();
			ProcessBuilder processBuilder = new ProcessBuilder();
			processBuilder.directory(scriptDirectory);
			processBuilder.command("jar", "xf", pathToJar, scriptsFolderInJar);
			Process process = processBuilder.start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				logger.error("Encountered an error while extracting scripts from " + pathToJar);
				return null;
			}
			return new File(scriptDirectory, scriptsFolderInJar);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Can be used to get the path to the Jar in which this file is located.
	 * <br/>
	 * <b>Important:</b> Only call this from a Plugin if that Plugin includes the PythonConnector project
	 * ({@code <scope>compile</scope>} in the pom)! If you use the PluginConnector provided by the Pipeline
	 * ({@code <scope>provided</scope>}) you need to call the following from your Plugin's code instead:
	 * <pre>
	 *     getClass().getProtectionDomain().getCodeSource().getLocation().getPath()
	 * </pre>
	 */
	public String getPathToJar() {
		return getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
	}

	public String getExecutablePath() {
		if (operatingSystem == OperatingSystem.Windows) {
			return path + "/Scripts/python.exe";
		}

		else if (operatingSystem == OperatingSystem.MacOS) return path + "/bin/python";

		else if (operatingSystem == OperatingSystem.Linux) return path + "/bin/python3";

		throw new IllegalStateException("OS not supported");
	}

	public void activate() {
		//does nothing, since i have no idea if i need it
	}

	public List<PythonDependency> listInstalledPackages() {
		return new ListVenvPackagesStreamExecutor(this).execute();
	}

	public String getPipInstallerString(PythonDependency dependency) {
		if (dependency.comparator == null || dependency.version == null) return dependency.dependencyName;

		return dependency.dependencyName + dependency.comparator + dependency.version;
	}

	public void installDependenciesFromRequirementsTxt(String pathToReqTxt){
		try {
			String content = FileUtils.readFileToString(new File(pathToReqTxt), "UTF-8");

			for(String req : content.split("\n")){
				String name = req.split("==")[0];
				String version = req.split("==")[1];
				installDependency(new PythonDependency(name,"==",version));
				logger.info("Install Dependency: "+ name+"=="+version);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void installDependency(PythonDependency dependency) {
		try {
			//we create a temp file to direct any potential error output
			File tempFile = File.createTempFile("dummyLogFile", "txt");
			tempFile.deleteOnExit();

			ProcessBuilder processBuilder = new ProcessBuilder();
			processBuilder.command(getExecutablePath(), "-m", "pip", "install", getPipInstallerString(dependency));
			processBuilder.redirectError(tempFile);

			Process process = processBuilder.start();

			// blocked :(
			BufferedReader reader =
					new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().startsWith("Requirement already satisfied:")) continue;
				logger.error(line);
			}

			int exitCode = process.waitFor();
			if (exitCode != 0) {
				String errorString = String.join("\n\t", Files.readAllLines(tempFile.toPath()));
				logger.info(errorString);
			}
		}
		catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static File readStreamToFile(InputStream stream, String fileName, String fileExtension) {
		File tempFile = null;
		try {
			tempFile = File.createTempFile(fileName, "." + fileExtension);
			OutputStream out = null;
			try {
				out = new FileOutputStream(tempFile);
			}
			catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int read;
			byte[] bytes = new byte[1024];

			try {
				while ((read = stream.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tempFile.deleteOnExit();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return tempFile;
	}
}
