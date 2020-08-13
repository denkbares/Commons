package python.venv;

import commandLine.CommandLineExecutor;
import commandLine.CommandLineStreamHandler;
import streamExecutor.DefaultStreamHandler;
import streamExecutor.ListVenvPackagesStreamExecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PythonVirtualEnvironment {

	public final String path;

	public final OperatingSystem operatingSystem;

	public PythonVirtualEnvironment(String path) {
		this.path = path;
		operatingSystem = getOperatingSystem();
	}

	public OperatingSystem getOperatingSystem() {
		String osName = System.getProperty("os.name");

		return OperatingSystem.fromString(osName);
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
			System.out.println("Problem!");
			e.printStackTrace();
		}

	}

	public void executeScript(InputStream scriptSteam, String scriptName, CommandLineStreamHandler streamHandler, String... args) {

		System.out.println("Execute: " + scriptName);
		//we need to create a temp file for the inputstream
		File tempFile = readStreamToFile(scriptSteam, scriptName, "py");


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
			System.out.println("Problem!");
			e.printStackTrace();
		}
	}

	public String getExecutablePath() {
		if (operatingSystem == OperatingSystem.Windows) {
			return path + "/Scripts/python.exe";
		}

		else if (operatingSystem == OperatingSystem.MacOS) return path + "/bin/python";

		throw new IllegalStateException("Not supported for linux atm!");
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

	public void installDependency(PythonDependency dependency) {

		ProcessBuilder processBuilder = new ProcessBuilder();

		processBuilder.command(getExecutablePath(), "-m", "pip", "install", getPipInstallerString(dependency));

		try {

			Process process = processBuilder.start();

			// blocked :(
			BufferedReader reader =
					new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				if(line.trim().startsWith("Requirement already satisfied:"))continue;
				System.out.println(line);
			}

			int exitCode = process.waitFor();
			System.out.println("\nExited with error code : " + exitCode);
		}
		catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private File readStreamToFile(InputStream stream, String fileName, String fileExtension) {
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
