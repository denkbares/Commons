package python.venv;

public enum OperatingSystem {Windows,MacOS,Linux;

	public static OperatingSystem fromString(String osName) {
		if(osName.startsWith("Mac OS")) return OperatingSystem.MacOS;
		else if(osName.startsWith("Windows")) return OperatingSystem.Windows;
		else if(osName.startsWith("Linux")) return OperatingSystem.Linux;

		throw new IllegalStateException("OS not supported!");
	}
}
