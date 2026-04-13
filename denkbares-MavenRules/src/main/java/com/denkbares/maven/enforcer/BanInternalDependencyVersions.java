package com.denkbares.maven.enforcer;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Named;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

@Named("banInternalDependencyVersions")
public class BanInternalDependencyVersions implements EnforcerRule {

	private boolean allowPomPackaging = true;

	@Override
	public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
		MavenProject project = resolveProject(helper);

		Model model = project.getOriginalModel();

		if (isRootPom(model) || isAllowedPomPackaging(model)) {
			return;
		}

		List<String> violations = new ArrayList<>();
		for (Dependency dependency : model.getDependencies()) {
			String version = dependency.getVersion();
			if (version == null || version.isBlank()) {
				continue;
			}
			violations.add(dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + version);
		}

		if (!violations.isEmpty()) {
			throw new EnforcerRuleException(
					"Dependencies must not declare explicit <version> in child POMs. "
							+ "Use <dependencyManagement> in the parent POM instead.\n"
							+ "Violations in " + project.getFile() + ":\n  "
							+ String.join("\n  ", violations));
		}
	}

	private MavenProject resolveProject(EnforcerRuleHelper helper) throws EnforcerRuleException {
		// Try component lookup first (works in most Maven versions)
		try {
			MavenProject project = helper.getComponent(MavenProject.class);
			if (project != null) {
				return project;
			}
		}
		catch (Exception ignored) {
		}

		// Fallback: evaluate the ${project} expression
		try {
			Object evaluated = helper.evaluate("${project}");
			if (evaluated instanceof MavenProject) {
				return (MavenProject) evaluated;
			}
		}
		catch (Exception ignored) {
		}

		throw new EnforcerRuleException("MavenProject was not injected into the enforcer rule.");
	}

	public void setAllowPomPackaging(boolean allowPomPackaging) {
		this.allowPomPackaging = allowPomPackaging;
	}

	private boolean isRootPom(Model model) {
		return model.getParent() == null;
	}

	private boolean isAllowedPomPackaging(Model model) {
		return allowPomPackaging && "pom".equals(model.getPackaging());
	}

	@Override
	public boolean isCacheable() {
		return false;
	}

	@Override
	public boolean isResultValid(EnforcerRule enforcerRule) {
		return false;
	}

	@Override
	public String getCacheId() {
		return null;
	}
}
