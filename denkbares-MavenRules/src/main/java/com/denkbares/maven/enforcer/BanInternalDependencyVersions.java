package com.denkbares.maven.enforcer;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

public class BanInternalDependencyVersions extends AbstractEnforcerRule {

    private MavenProject project;

    private List<String> groupIdPrefixes = List.of();
    private boolean allowPomPackaging = true;

    public BanInternalDependencyVersions() {
    }

    @Override
    public void execute() throws EnforcerRuleException {
        try {
            if (project == null) {
                throw new EnforcerRuleException("MavenProject was not provided to BanInternalDependencyVersions.");
            }
            Model model = project.getOriginalModel();

            if (isRootPom(model) || isInactive() || isAllowedPomPackaging(model)) {
                return;
            }

            List<String> violations = new ArrayList<>();
            for (Dependency dependency : model.getDependencies()) {
                String groupId = dependency.getGroupId();
                String version = dependency.getVersion();
                if (groupId == null || version == null || version.isBlank()) {
                    continue;
                }
                if (!matchesInternalGroup(groupId)) {
                    continue;
                }
                violations.add(groupId + ":" + dependency.getArtifactId() + ":" + version);
            }

            if (!violations.isEmpty()) {
                throw new EnforcerRuleException(
                        "Internal dependencies must not declare explicit <version> in child POMs: "
                                + String.join(", ", violations)
                                + " in "
                                + project.getFile());
            }
        } catch (EnforcerRuleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EnforcerRuleException("Failed to evaluate internal dependency versions.", exception);
        }
    }

    public void setGroupIdPrefixes(List<String> groupIdPrefixes) {
        this.groupIdPrefixes = groupIdPrefixes == null ? List.of() : List.copyOf(groupIdPrefixes);
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public void setAllowPomPackaging(boolean allowPomPackaging) {
        this.allowPomPackaging = allowPomPackaging;
    }

    private boolean isRootPom(Model model) {
        return model.getParent() == null;
    }

    private boolean isInactive() {
        return groupIdPrefixes.isEmpty();
    }

    private boolean isAllowedPomPackaging(Model model) {
        return allowPomPackaging && "pom".equals(model.getPackaging());
    }

    private boolean matchesInternalGroup(String groupId) {
        for (String prefix : groupIdPrefixes) {
            if (groupId.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
