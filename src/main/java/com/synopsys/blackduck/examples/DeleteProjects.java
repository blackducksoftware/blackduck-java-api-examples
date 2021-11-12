package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.blackduck.util.BlackDuckViewUtils;
import com.synopsys.integration.blackduck.api.core.ResourceLink;
import com.synopsys.integration.blackduck.api.core.response.UrlMultipleResponses;
import com.synopsys.integration.blackduck.api.generated.view.CodeLocationView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.service.dataservice.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Deletes projects along with all project versions and mapped scans.  The projects that match the regex will be deleted.  The regex can apply to either project name, application ID or a custom field value.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.DeleteProjectsCommand -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -regex "^MOCK .*" -matchType "customField_Business Unit" -dryRun
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class DeleteProjects extends ValidateBlackDuckConnection {
    private static final Logger log = LoggerFactory.getLogger(DeleteProjects.class);

    static final String DRY_RUN_PARAMETER = "dryRun";
    static final String REGEX_PARAMETER = "regex";
    static final String MATCH_TYPE_PARAMETER = "matchType";
    static final String MATCH_OPTION_NAME = "PROJECT_NAME";
    static final String MATCH_OPTION_APPLICATION_ID = "APPLICATION_ID";

    public List<ProjectView> findMatchingProjects(BlackDuckRestConnector restConnector, String regex, String matchType) throws IntegrationException {
        ProjectService projectService = restConnector.getBlackDuckServicesFactory().createProjectService();

        List<ProjectView> allProjects = projectService.getAllProjects();
        List<ProjectView> results = new ArrayList<>();
        for (ProjectView project : allProjects) {
            if (matchType.equals(MATCH_OPTION_NAME)) {
                if (project.getName().matches(regex)) {
                    results.add(project);
                }
            } else if (matchType.equals(MATCH_OPTION_APPLICATION_ID)) {
                // Load the application ID.
                String applicationID = getApplicationIdForProject(restConnector, project);
                if (!StringUtils.isEmpty(applicationID)) {
                    if (applicationID.matches(regex)) {
                        results.add(project);
                    }
                } else {
                    log.warn("Project has no application ID - ignoring [" + project.getName() + "]");
                }
            } else {
                log.error("Unknown match type [" + matchType + "] - ignoring.");
            }
        }

        return results;
    }

    public void deleteProjects(BlackDuckRestConnector restConnector, List<ProjectView> projects) throws IntegrationException {
        for (ProjectView project : projects) {
            // Load all versions for the project.
            Optional<List<ProjectVersionView>> projectVersions = getProjectVersions(restConnector, project);
            if (projectVersions.isPresent()) {
                for (ProjectVersionView projectVersion : projectVersions.get()) {
                    // Load the mapped code locations for the project version.
                    Optional<List<CodeLocationView>> mappedCodeLocations = getMappedCodeLocations(restConnector, project, projectVersion);
                    if (mappedCodeLocations.isPresent()) {
                        for (CodeLocationView codeLocation : mappedCodeLocations.get()) {
                            // Delete the code location.
                            log.info("Deleting code location [" + codeLocation.getName() + "] from project [" + project.getName() + "] - version [" + projectVersion.getVersionName() + "]");
                            try {
                                restConnector.getBlackDuckApiClient().delete(codeLocation);
                            } catch (IntegrationException e) {
                                log.error("Failed to delete code location [" + codeLocation.getName() + "] from project [" + project.getName() + "] - version [" + projectVersion.getVersionName() + "] due to : " + e.getMessage(), e);
                            }
                        }
                    }
                }

                if (projectVersions.get().size() > 1) {
                    // Delete all versions except the last one.
                    for (int i = 1; i < projectVersions.get().size(); i++) {
                        // Delete the project version.
                        log.info("Deleting project version from project [" + project.getName() + "] - version [" + projectVersions.get().get(i).getVersionName() + "]");
                        try {
                            restConnector.getBlackDuckApiClient().delete(projectVersions.get().get(i));
                        } catch (IntegrationException e) {
                            log.error("Failed to delete project version from project [" + project.getName() + "] - version [" + projectVersions.get().get(i).getVersionName() + "] due to : " + e.getMessage(), e);
                        }
                    }
                }
            }

            // Delete the project and final version.
            log.info("Deleting project [" + project.getName() + "] and final version.");
            try {
                restConnector.getBlackDuckApiClient().delete(project);
            } catch (IntegrationException e) {
                log.error("Failed to delete project [" + project.getName() + "] and final version due to : " + e.getMessage(), e);
            }
        }
    }

    /**
     * Calls the projects REST API to retrieve a project's versions.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param project the ProjectView.
     * @return Optional List of ProjectVersionView.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<ProjectVersionView>> getProjectVersions(BlackDuckRestConnector restConnector, ProjectView project) throws IntegrationException {
        if (project != null) {
            ProjectService projectService = restConnector.getBlackDuckServicesFactory().createProjectService();

            List<ProjectVersionView> projectVersions = projectService.getAllProjectVersions(project);
            return (projectVersions != null) ? Optional.of(projectVersions) : Optional.empty();
        }
        return Optional.empty();
    }

    public Optional<List<CodeLocationView>> getMappedCodeLocations(BlackDuckRestConnector restConnector, ProjectView project, ProjectVersionView projectVersion) {
        if (project != null && projectVersion != null) {
            ResourceLink link = BlackDuckViewUtils.getResourceLink(projectVersion, ProjectVersionView.CODELOCATIONS_LINK);
            if (link != null) {
                try {
                    return Optional.of(restConnector.getBlackDuckApiClient().getAllResponses(new UrlMultipleResponses<>(link.getHref(), CodeLocationView.class)));
                } catch (IntegrationException e) {
                    log.error("Failed to load mapped code locations for for project [" + project.getName() + "] - version [" + projectVersion.getVersionName() + "] due to : " + e.getMessage(), e);
                }
            }
        }
        return Optional.empty();
    }


    public void listProjectsForDryRun(List<ProjectView> projects, String matchType, String regex) {
        log.info(projects.size() + " project(s) found.");

        log.info("-----------------------");
        log.info("DRY RUN - The following projects match [" + regex + "] for match type [" + matchType + "]");
        log.info("-----------------------");

        for (ProjectView project : projects) {
            log.info(project.getName());
        }
        log.info("-----------------------");
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
     * Command line interface.
     *
     * @param args command line parameters.
     */
    public static void main(String... args) {
        Options options = createCommandLineOptions();

        addRequiredSingleOptionParameter(options, REGEX_PARAMETER, "The regular expression to match for projects to delete.");
        addRequiredSingleOptionParameter(options, MATCH_TYPE_PARAMETER, "The match type, whether to match against project name, application id or custom field value.  Possible values on field to match - PROJECT_NAME or APPLICATION_ID");
        options.addOption(Option.builder(DRY_RUN_PARAMETER).argName(DRY_RUN_PARAMETER).desc("Whether to perform a dry run and just list matching projects.").build());

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String regex = getRequiredSingleOptionParameterValue(commandLine.get(), REGEX_PARAMETER);
            String matchType = getRequiredSingleOptionParameterValue(commandLine.get(), MATCH_TYPE_PARAMETER);
            boolean dryRun = commandLine.get().hasOption(DRY_RUN_PARAMETER);

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

            DeleteProjects deleteProjectsCommand = new DeleteProjects();
            if (deleteProjectsCommand.validateBlackDuckConnection(restConnector)) {
                try {
                    if (dryRun) {
                        log.info("DRY RUN - Listing projects matching [" + regex + "], matchType [" + matchType + "] on [" + blackDuckInstance.getServerUrl() + "]");
                    } else {
                        log.info("Deleting projects matching [" + regex + "], matchType [" + matchType + "] on [" + blackDuckInstance.getServerUrl() + "]");
                    }

                    List<ProjectView> projects = deleteProjectsCommand.findMatchingProjects(restConnector, regex, matchType);
                    if (dryRun) {
                        deleteProjectsCommand.listProjectsForDryRun(projects, matchType, regex);
                    } else {
                        deleteProjectsCommand.deleteProjects(restConnector, projects);
                    }
                } catch (IntegrationException e) {
                    log.error("Failed to delete projects matching regex [" + regex + "], matchType [" + matchType + "] on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                }
            }
        }
    }
}
