package streamExecutor;

import commandLine.CommandLineStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultStreamHandler implements CommandLineStreamHandler {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public void onError(String error) {
		logger.error(error);
	}

	@Override
	public void onConsoleOutputChanged(String newLine) {
		logger.info(newLine);
	}
}
