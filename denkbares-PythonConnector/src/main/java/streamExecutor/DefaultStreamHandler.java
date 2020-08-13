package streamExecutor;

import java.io.IOException;

import commandLine.CommandLineExecutor;
import commandLine.CommandLineStreamHandler;

public class DefaultStreamHandler implements CommandLineStreamHandler {


	@Override
	public void onError(String error) {
		System.out.println(error);
	}

	@Override
	public void onConsoleOutputChanged(String newLine) {
		System.out.println(newLine);
	}
}
