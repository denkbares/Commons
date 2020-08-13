package python.venv;

public enum OperatingSystem {Windows,MacOS,Linux;

	public static OperatingSystem fromString(String osName) {
		if(osName.startsWith("Mac OS")) return OperatingSystem.MacOS;
		else if(osName.startsWith("Windows")) return OperatingSystem.Windows;

		System.out.println(osName);

		throw new IllegalStateException("Currently onyl MacOS supported!");
	}
}
