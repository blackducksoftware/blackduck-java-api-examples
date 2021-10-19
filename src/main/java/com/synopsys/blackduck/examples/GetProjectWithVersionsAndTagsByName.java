package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.integration.blackduck.api.core.ResourceLink;
import com.synopsys.integration.blackduck.api.core.BlackDuckView;
import com.synopsys.integration.blackduck.api.core.response.UrlMultipleResponses;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectCloneCategoriesType;
import com.synopsys.integration.blackduck.api.generated.view.*;
import com.synopsys.integration.blackduck.service.dataservice.ProjectService;
import com.synopsys.integration.blackduck.service.dataservice.TagService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Gets a Project by name and version information and outputs the information to the log.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.GetProjectWithVersionsAndTagsByName -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -projectname "Test Project"
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class GetProjectWithVersionsAndTagsByName extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(GetProjectWithVersionsAndTagsByName.class);

    static final String PROJECT_NAME_PARAMETER = "projectname";

    /**
     * Calls the projects REST API to retrieve a project by name.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param projectName the project name.
     * @return Optional ProjectView.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<ProjectView> getProjectByName(BlackDuckRestConnector restConnector, String projectName) throws IntegrationException {
        ProjectService projectService = restConnector.getBlackDuckServicesFactory().createProjectService();

        return projectService.getProjectByName(projectName);
    }

    /**
     * Calls the projects REST API to retrieve a project's versions.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param project the ProjectView.
     * @return Optional List of ProjectVersionView.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<ProjectVersionView>> getProjectVersions(BlackDuckRestConnector restConnector, Optional<ProjectView> project) throws IntegrationException {
        if (project.isPresent()) {
            ProjectService projectService = restConnector.getBlackDuckServicesFactory().createProjectService();

            List<ProjectVersionView> projectVersions = projectService.getAllProjectVersions(project.get());
            return (projectVersions != null) ? Optional.of(projectVersions) : Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * Calls the projects REST API to retrieve a project's tags.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param project the ProjectView.
     * @return Optional List of TagView.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<TagView>> getProjectTags(BlackDuckRestConnector restConnector, Optional<ProjectView> project) throws IntegrationException {
        if (project.isPresent()) {
            TagService tagService = restConnector.getBlackDuckServicesFactory().createTagService();

            List<TagView> tags = tagService.getAllTags(project.get());
            return (tags != null) ? Optional.of(tags) : Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * Load the custom field values for a given Black Duck object, e.g. a project or project version.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param blackDuckView the object to look for the values for.
     * @return Optional List of CustomFieldView.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<CustomFieldView>> getCustomFields(BlackDuckRestConnector restConnector, BlackDuckView blackDuckView) throws IntegrationException {
        if (blackDuckView != null) {
            HttpUrl customFieldsLink = blackDuckView.getFirstLink("custom-fields");
            if (customFieldsLink != null) {
                List<CustomFieldView> customFields = restConnector.getBlackDuckApiClient().getAllResponses(new UrlMultipleResponses<>(new HttpUrl(customFieldsLink.string()), CustomFieldView.class));

                return (customFields != null) ? Optional.of(customFields) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Outputs the project to the log.
     * @param projectName the project name.
     * @param project ProjectView object.
     */
    public void outputProject(String projectName, Optional<ProjectView> project) {
        if (project.isPresent()) {
            log.info("-----------------------");
            log.info("Project Name [" + project.get().getName() + "]");
            log.info("Project Description [" + project.get().getDescription() + "]");
            if (project.get().getHref() != null) {
                log.info("Project Href [" + project.get().getHref().string() + "]");
            }
            log.info("Project Tier [" + project.get().getProjectTier() + "]");
            log.info("Project Owner [" + project.get().getProjectOwner() + "]");
            log.info("Project Created By [" + project.get().getCreatedBy() + "]");
            log.info("Project Created By User [" + project.get().getCreatedByUser() + "]");
            log.info("Project Created At [" + project.get().getCreatedAt() + "]");
            log.info("Project Custom Signature Enabled [" + project.get().getCustomSignatureEnabled() + "]");
            log.info("Project Custom Signature Depth [" + project.get().getCustomSignatureDepth() + "]");
            log.info("Project Level Adjustments [" + project.get().getProjectLevelAdjustments() + "]");
            if (project.get().getCloneCategories() != null) {
                log.info("Project Clone Categories:");
                for (ProjectCloneCategoriesType type : project.get().getCloneCategories()) {
                    log.info(" - " + type.prettyPrint());
                }
            }
            if (project.get().getResourceLinks() != null) {
                log.info("Project Resource Links: (rel : href)");
                for (ResourceLink link : project.get().getResourceLinks()) {
                    log.info(" - " + link.getRel() + " : " + link.getHref().string());
                }
            }
            log.info("-----------------------");
        } else {
            log.info("-----------------------");
            log.error("Project [" + projectName + "] was not found.");
            log.info("-----------------------");
        }
    }

    /**
     * Outputs the project's versions and tags to the log.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param projectView ProjectView object.
     * @param versions the List of ProjectVersionView.
     * @param tags the List of TagView.
     */
    public void outputVersionsAndTags(BlackDuckRestConnector restConnector, Optional<ProjectView> projectView, Optional<List<ProjectVersionView>> versions, Optional<List<TagView>> tags) {
        if (projectView.isPresent() && versions.isPresent()) {
            log.info("-----------------------");
            log.info("Project [" + projectView.get().getName() + "] has [" + versions.get().size() + "] versions.");
            log.info("Project Versions:");
            log.info("-----------------------");
            log.info("name,href,nickname,phase,distribution,license,created_by,created_at,released_on");
            log.info("-----------------------");
            for (ProjectVersionView version : versions.get()) {
                String output = version.getVersionName() + SEPARATOR + version.getHref().string() + " " + SEPARATOR + version.getNickname() + SEPARATOR;
                output = output + version.getPhase() + SEPARATOR + version.getDistribution() + SEPARATOR + version.getLicense().getLicenseDisplay();
                output = output + SEPARATOR + version.getCreatedBy() + SEPARATOR + version.getCreatedAt() + SEPARATOR + version.getReleasedOn();
                log.info(output);

                try {
                    Optional<List<CustomFieldView>> customFieldsForVersion = getCustomFields(restConnector, version);
                    outputCustomFields("Project Version [" + version.getVersionName() + "]", customFieldsForVersion);
                } catch (IntegrationException e) {
                    log.error("Failed to load project version custom fields for [" + version.getVersionName() + "] on [" + restConnector.getServerUrl() + "] due to : " + e.getMessage(), e);
                }
            }
            log.info("-----------------------");
        }
        if (projectView.isPresent() && tags.isPresent()) {
            log.info("-----------------------");
            log.info("Project [" + projectView.get().getName() + "] has [" + tags.get().size() + "] tags.");
            log.info("Project Tags:");
            log.info("-----------------------");
            log.info("name,href");
            log.info("-----------------------");
            for (TagView tag : tags.get()) {
                String output = tag.getName() + SEPARATOR + tag.getHref().string();
                log.info(output);
            }
            log.info("-----------------------");
        }
    }

    /**
     * Outputs the custom field values to the log.
     * @param name Black Duck object name.
     * @param customFields the List of CustomFieldView.
     */
    public void outputCustomFields(String name, Optional<List<CustomFieldView>> customFields) {
        if (name != null && customFields.isPresent()) {
            log.info("-----------------------");
            log.info(name + " has [" + customFields.get().size() + "] custom fields.");
            log.info(" - Custom Fields:");
            log.info(" - -----------------------");
            log.info(" - label,href,active,position,type,description,values...");
            log.info(" - -----------------------");
            for (CustomFieldView customField : customFields.get()) {
                String output = " - " + customField.getLabel() + SEPARATOR + customField.getHref().string() + " " + SEPARATOR + customField.getActive() + SEPARATOR;
                output = output + customField.getPosition() + SEPARATOR + customField.getType() + SEPARATOR + customField.getDescription();
                if (customField.getValues() == null || customField.getValues().size() == 0) {
                    output = output + SEPARATOR + "no_values_set";
                } else {
                    for (String value : customField.getValues()) {
                        output = output + SEPARATOR + value;
                    }
                }
                log.info(output);
            }
            log.info(" - -----------------------");
        }
    }

    /**
     * Example shows how to get a project by name.
     *
     * @param args command line parameters.
     */
    public static void main(String... args) {
        Options options = createCommandLineOptions();

        addRequiredSingleOptionParameter(options, PROJECT_NAME_PARAMETER, "The project name (case sensitive)");

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String projectName = getRequiredSingleOptionParameterValue(commandLine.get(), PROJECT_NAME_PARAMETER);

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

            GetProjectWithVersionsAndTagsByName getProject = new GetProjectWithVersionsAndTagsByName();
            if (getProject.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Loading project [" + projectName + "] on [" + blackDuckInstance.getServerUrl() + "]");

                    Optional<ProjectView> project = getProject.getProjectByName(restConnector, projectName);
                    getProject.outputProject(projectName, project);

                    if (project.isPresent()) {
                        Optional<List<ProjectVersionView>> versions = getProject.getProjectVersions(restConnector, project);
                        Optional<List<TagView>> tags = getProject.getProjectTags(restConnector, project);
                        Optional<List<CustomFieldView>> customFields = getProject.getCustomFields(restConnector, project.get());

                        getProject.outputCustomFields("Project [" + projectName + "]", customFields);

                        getProject.outputVersionsAndTags(restConnector, project, versions, tags);
                    }
                } catch (IntegrationException e) {
                    log.error("Failed to load project [" + projectName + "] on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                }
            }
        }
    }
}
