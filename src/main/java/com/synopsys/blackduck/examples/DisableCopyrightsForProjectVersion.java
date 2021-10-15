package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.integration.blackduck.api.core.response.UrlMultipleResponses;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionComponentView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.api.manual.temporary.component.VersionBomOriginView;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Disables copyrights for all component origins within a project version's bill of materials.  Used as an example
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2020.6.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.DisableCopyrightsForProjectVersion -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -projectVersionUrl https://52.213.63.29/api/projects/2b8e2496-891a-42b7-abcd-5ae0cd527fd7/versions/32b7d1da-e62d-41a6-845d-29b7add91428
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class DisableCopyrightsForProjectVersion extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(DisableCopyrightsForProjectVersion.class);

    private static final String PROJECT_VERSION_URL_PARAMETER = "projectVersionUrl";
    private static final String ONLY_DISABLE_MULTIPLE_COPYRIGHTS = "ignoreSingleCopyrights";

    /**
     * Calls the BoM entries for a project version.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param projectVersionUrl the project version url.
     * @return List of ProjectView objects.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<ProjectVersionComponentView>> getBoMEntriesForProjectVersion(BlackDuckRestConnector restConnector, String projectVersionUrl) throws IntegrationException {
        BlackDuckApiClient blackDuckApiClient = restConnector.getBlackDuckApiClient();
        // Note this URL will filter the results to show not ignored items.
        HttpUrl url = new HttpUrl(projectVersionUrl + "/components?sort=projectName%20ASC&filter=bomInclusion%3Afalse&filter=bomMatchInclusion%3Afalse");

        List<ProjectVersionComponentView> views = blackDuckApiClient.getAllResponses(new UrlMultipleResponses<>(url, ProjectVersionComponentView.class));
        return (views != null) ? Optional.of(views) : Optional.empty();
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
     * Example command line.
     *
     * @param args command line parameters.
     */
    public static void main(String... args) {
        Options options = createCommandLineOptions();

        addRequiredSingleOptionParameter(options, PROJECT_VERSION_URL_PARAMETER, "The project version url e.g. https://52.213.63.29/api/projects/2b8e2496-891a-42b7-abcd-5ae0cd527fd7/versions/32b7d1da-e62d-41a6-845d-29b7add91428");
        options.addOption(Option.builder(ONLY_DISABLE_MULTIPLE_COPYRIGHTS).argName(ONLY_DISABLE_MULTIPLE_COPYRIGHTS).desc("Whether to only disable multiple copyrights, e.g. if a component has a single copyright it will skip it and leave it active.").build());

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String projectVersionUrl = getRequiredSingleOptionParameterValue(commandLine.get(), PROJECT_VERSION_URL_PARAMETER);
            boolean onlyDisableMultiple = commandLine.get().hasOption(ONLY_DISABLE_MULTIPLE_COPYRIGHTS);

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

            DisableCopyrightsForProjectVersion disableCopyrightsForProjectVersion = new DisableCopyrightsForProjectVersion();
            if (disableCopyrightsForProjectVersion.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Disabling copyrights for [" + projectVersionUrl + "] - onlyDisableMultipleCopyrights [" + onlyDisableMultiple + "]");

                    // Get the list of BoM entries
                    Optional<List<ProjectVersionComponentView>> bomItems = disableCopyrightsForProjectVersion.getBoMEntriesForProjectVersion(restConnector, projectVersionUrl);
                    if (bomItems.isPresent()) {
                        // For each get the list of origins.
                        for (ProjectVersionComponentView bomItem : bomItems.get()) {
                            // For each origin get the copyrights
                            for (VersionBomOriginView origin : bomItem.getOrigins()) {
                                // Get the list of copyrights for the origin.

                                // For each copyright deactivate it.
                            }
                        }
                    } else {
                        log.info("No items in the Bill of Materials for [" + projectVersionUrl + "]");
                    }
                } catch (IntegrationException e) {
                    log.error("Failed to disable copyrights for [" + projectVersionUrl + "] as BoM could not be loaded for the project version due to : " + e.getMessage(), e);
                }
            }
        }
    }
}
