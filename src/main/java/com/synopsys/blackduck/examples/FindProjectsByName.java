package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.service.dataservice.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Finds projects by name..
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.FindProjectsByName -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -projectname "Test Project"
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class FindProjectsByName extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(FindProjectsByName.class);

    /**
     * Calls the projects REST API to retrieve the projects that match by name.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param projectName the project name to match.
     * @return List of ProjectView objects.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<ProjectView>> findProjects(BlackDuckRestConnector restConnector, String projectName) throws IntegrationException {
        ProjectService projectService = restConnector.getBlackDuckServicesFactory().createProjectService();

        List<ProjectView> projects = projectService.getProjectMatches(projectName, 100);

        return (projects != null) ? Optional.of(projects) : Optional.empty();
    }

    /**
     * Outputs the projects to the log.
     * @param projectName the project name to match.
     * @param projects List of ProjectView objects.
     */
    public void listProjects(String projectName, Optional<List<ProjectView>> projects) {
        if (projects.isPresent()) {
            log.info("-----------------------");
            log.info(projects.get().size() + " project(s) matching [" + projectName + "] found.");

            log.info("-----------------------");
            log.info("name,description,href,created_by,created_at,project_owner");
            log.info("-----------------------");

            for (ProjectView project : projects.get()) {
                String output = project.getName() + SEPARATOR + project.getDescription() + SEPARATOR;
                output += (project.getHref() != null ? project.getHref().string() : "");
                output += SEPARATOR + project.getCreatedBy() + SEPARATOR + project.getCreatedAt() + SEPARATOR + project.getProjectOwner();
                log.info(output);
            }
            log.info("-----------------------");
        } else {
            log.info("-----------------------");
            log.error("No Projects found matching name [" + projectName + "] - not found.");
            log.info("-----------------------");
        }
    }

    /**
     * Example shows how to list projects configured on a Black Duck instance.
     *
     * @param args command line parameters.
     */
    public static void main(String... args) {
        Options options = createCommandLineOptions();

        addRequiredSingleOptionParameter(options, GetProjectWithVersionsAndTagsByName.PROJECT_NAME_PARAMETER, "The project name (case sensitive)");

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String projectName = getRequiredSingleOptionParameterValue(commandLine.get(), GetProjectWithVersionsAndTagsByName.PROJECT_NAME_PARAMETER);

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

            FindProjectsByName listProjects = new FindProjectsByName();
            if (listProjects.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Finding projects matching name [" + projectName + "] on [" + blackDuckInstance.getServerUrl() + "]");

                    Optional<List<ProjectView>> projects = listProjects.findProjects(restConnector, projectName);
                    listProjects.listProjects(projectName, projects);
                } catch (IntegrationException e) {
                    log.error("Failed to find projects matching name [" + projectName + "] on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                }
            }
        }
    }
}
