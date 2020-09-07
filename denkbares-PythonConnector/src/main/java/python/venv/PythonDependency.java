package python.venv;

public class PythonDependency {

    public final String dependencyName;
    public final String comparator;
    public final String version;

    public PythonDependency(String dependencyName, String comparator, String version) {
        this.dependencyName = dependencyName;
        this.comparator = comparator;
        this.version = version;
    }

    public PythonDependency(String dependencyName) {
        this.dependencyName = dependencyName;
        this.comparator = null;
        this.version = null;
    }
}
