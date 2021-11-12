package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.blackduck.util.BlackDuckViewUtils;
import com.synopsys.integration.blackduck.api.core.BlackDuckView;
import com.synopsys.integration.blackduck.api.core.ResourceLink;
import com.synopsys.integration.blackduck.api.core.response.UrlMultipleResponses;
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.service.dataservice.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Lists projects and the projects application ID to a CSV file.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.ListProjectsAndApplicationId -apikey NDAxZTViMzctNjVjNS00YjczLWJhYjAtOWU1OWMzNWZhODA1OjFhZWZlZTNhLWQxZmQtNDVlMy05MGJmLTFjNTA5ZjhjYWQ0OQ== -url https://52.213.63.19 -trusthttps
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class ListProjectsAndApplicationId extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(ListProjectsAndApplicationId.class);

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

    public String getApplicationIdForProject(BlackDuckRestConnector restConnector, ProjectView project) {
        if (project != null) {
            ResourceLink link = BlackDuckViewUtils.getResourceLink(project, ProjectView.PROJECT_MAPPINGS_LINK);
            if (link != null) {
                try {
                    List<ProjectMapping> mappings = restConnector.getBlackDuckApiClient().getAllResponses(new UrlMultipleResponses<>(link.getHref(), ProjectMapping.class));
                    if (mappings != null && mappings.size() > 0) {
                        return mappings.get(0).getApplicationId();
                    }
                } catch (IntegrationException e) {
                    log.error("Failed to load application id for project [" + project.getName() + "] due to : " + e.getMessage(), e);
                }
            }
        }
        return null;
    }

    /**
     * Outputs the projects to the log and an output file.
     * @param projects List of ProjectView objects.
     */
    public void listProjectsAndApplicationIds(BlackDuckRestConnector restConnector, List<ProjectView> projects) {
        log.info(projects.size() + " project(s) found.");

        log.info("-----------------------");
        log.info("name,application_id,href,created_by,created_at,project_owner");
        log.info("-----------------------");

        FileWriter fileWriter = null;
        try {
            File file = new File("project_applicationids.csv");
            fileWriter = new FileWriter(file);
            fileWriter.append("name,application_id,href,created_by,created_at,project_owner\n");

            for (ProjectView project : projects) {
                String applicationId = getApplicationIdForProject(restConnector, project);
                String output = normaliseValue(project.getName()) + SEPARATOR + normaliseValue(applicationId) + SEPARATOR;
                output += (project.getHref() != null ? project.getHref().string() : "");
                output += SEPARATOR + project.getCreatedBy() + SEPARATOR + project.getCreatedAt() + SEPARATOR + normaliseValue(project.getProjectOwner());

                fileWriter.write(output + "\n");
                log.info(output);
            }
            log.info("-----------------------");
            log.info("Written projects to file [" + file.getAbsolutePath() + "]");
        } catch (IOException e) {
            log.error("Failed to write file due to : " + e.getMessage(), e);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    // Do nothing.
                }
            }
        }
    }

    private String normaliseValue(String val) {
        if (val == null) {
            return "";
        }
        if (val != null) {
            if (val.contains(SEPARATOR)) {
                return "\"" + val + "\"";
            }
        }
        return val;
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

            ListProjectsAndApplicationId listProjects = new ListProjectsAndApplicationId();
            if (listProjects.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Listing projects and application ID on [" + blackDuckInstance.getServerUrl() + "]");

                    List<ProjectView> projects = listProjects.getProjects(restConnector);
                    listProjects.listProjectsAndApplicationIds(restConnector, projects);
                } catch (IntegrationException e) {
                    log.error("Failed to list projects on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                }
            }
        }
    }

    public static class ProjectMapping extends BlackDuckView {
        private String applicationId;

        public String getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(String applicationId) {
            this.applicationId = applicationId;
        }
    }
}
