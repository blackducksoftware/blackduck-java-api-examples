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
 * Lists projects and outputs the information to the log.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.ListProjects -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class ListProjects extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(ListProjects.class);

    /**
     * Calls the projects REST API to retrieve the projects.
     * @param restConnector BlackDuckRestConnector to connect.
     * @return List of ProjectView objects.
     * @throws IntegrationException if the request was not successful.
     */
    public List<ProjectView> getProjects(BlackDuckRestConnector restConnector) throws IntegrationException {
        ProjectService projectService = restConnector.getBlackDuckServicesFactory().createProjectService();

        return projectService.getAllProjects();
    }

    /**
     * Outputs the projects to the log.
     * @param projects List of ProjectView objects.
     */
    public void listProjects(List<ProjectView> projects) {
        log.info(projects.size() + " project(s) found.");

        log.info("-----------------------");
        log.info("name,description,href,created_by,created_at,project_owner");
        log.info("-----------------------");

        for (ProjectView project : projects) {
            String output = project.getName() + SEPARATOR + project.getDescription() + SEPARATOR;
            output += (project.getHref() != null ? project.getHref().string() : "");
            output += SEPARATOR + project.getCreatedBy() + SEPARATOR + project.getCreatedAt() + SEPARATOR + project.getProjectOwner();
            log.info(output);
        }
        log.info("-----------------------");
    }

    /**
     * Example shows how to list projects configured on a Black Duck instance.
     *
     * @param args command line parameters.
     */
    public static void main(String... args) {
        Options options = createCommandLineOptions();

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

            ListProjects listProjects = new ListProjects();
            if (listProjects.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Listing projects on [" + blackDuckInstance.getServerUrl() + "]");

                    List<ProjectView> projects = listProjects.getProjects(restConnector);
                    listProjects.listProjects(projects);
                } catch (IntegrationException e) {
                    log.error("Failed to list projects on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                }
            }
        }
    }
}
