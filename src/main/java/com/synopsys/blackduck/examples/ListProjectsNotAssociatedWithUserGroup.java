package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.blackduck.util.BlackDuckViewUtils;
import com.synopsys.integration.blackduck.api.core.ResourceLink;
import com.synopsys.integration.blackduck.api.core.response.UrlMultipleResponses;
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.api.generated.view.RoleAssignmentView;
import com.synopsys.integration.blackduck.api.generated.view.UserGroupView;
import com.synopsys.integration.blackduck.service.dataservice.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Lists projects that are not referenced in any user group.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.ListProjectsNotAssociatedWithUserGroup -apikey NDAxZTViMzctNjVjNS00YjczLWJhYjAtOWU1OWMzNWZhODA1OjFhZWZlZTNhLWQxZmQtNDVlMy05MGJmLTFjNTA5ZjhjYWQ0OQ== -url https://52.213.63.19 -trusthttps
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class ListProjectsNotAssociatedWithUserGroup extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(ListProjectsNotAssociatedWithUserGroup.class);

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

    public List<UserGroupView> getUserGroups(BlackDuckRestConnector restConnector) throws IntegrationException {
        return restConnector.getBlackDuckApiClient().getAllResponses(restConnector.getBlackDuckServicesFactory().getApiDiscovery().metaUsergroupsLink());
    }

    public List<RoleAssignmentView> getRolesForGroup(BlackDuckRestConnector restConnector, UserGroupView userGroup) {
        ResourceLink link = BlackDuckViewUtils.getResourceLink(userGroup, "roles");
        try {
            if (link != null) {
                return restConnector.getBlackDuckApiClient().getAllResponses(new UrlMultipleResponses<>(link.getHref(), RoleAssignmentView.class));
            } else {
                log.error("Failed to load roles for user group [" + userGroup.getName() + "] as no 'roles' link available");
            }
        } catch (IntegrationException e) {
            log.error("Failed to load roles for user group [" + userGroup.getName() + "] due to : " + e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    public Collection<ProjectView> findProjectsNotReferencedInAnyUserGroup(BlackDuckRestConnector restConnector) throws IntegrationException {
        List<ProjectView> projects = getProjects(restConnector);
        List<UserGroupView> userGroups = getUserGroups(restConnector);

        Map<String, ProjectView> projectsMap = new HashMap<>();
        for (ProjectView project : projects) {
            projectsMap.put(project.getHref().string(), project);
        }

        for (UserGroupView userGroup : userGroups) {
            List<RoleAssignmentView> roles = getRolesForGroup(restConnector, userGroup);
            if (roles != null) {
                for (RoleAssignmentView role : roles) {
                    projectsMap.remove(role.getScope());
                }
            }
        }
        return projectsMap.values();
    }

    public String getApplicationIdForProject(BlackDuckRestConnector restConnector, ProjectView project) {
        if (project != null) {
            ResourceLink link = BlackDuckViewUtils.getResourceLink(project, ProjectView.PROJECT_MAPPINGS_LINK);
            if (link != null) {
                try {
                    List<ListProjectsAndApplicationId.ProjectMapping> mappings = restConnector.getBlackDuckApiClient().getAllResponses(new UrlMultipleResponses<>(link.getHref(), ListProjectsAndApplicationId.ProjectMapping.class));
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
    public void listProjects(BlackDuckRestConnector restConnector, Collection<ProjectView> projects) {
        log.info(projects.size() + " project(s) found.");

        log.info("-----------------------");
        log.info("name,application_id,href,created_by,created_at,project_owner");
        log.info("-----------------------");

        FileWriter fileWriter = null;
        try {
            File file = new File("projects_not_in_any_usergroup.csv");
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

            ListProjectsNotAssociatedWithUserGroup listProjects = new ListProjectsNotAssociatedWithUserGroup();
            if (listProjects.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Listing projects not referenced in any user group on [" + blackDuckInstance.getServerUrl() + "]");

                    Collection<ProjectView> projects = listProjects.findProjectsNotReferencedInAnyUserGroup(restConnector);
                    listProjects.listProjects(restConnector, projects);
                } catch (IntegrationException e) {
                    log.error("Failed to list projects on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                }
            }
        }
    }
}
