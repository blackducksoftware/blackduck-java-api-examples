package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.integration.blackduck.api.core.response.UrlMultipleResponses;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionComponentView;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Lists the bill of materials for a project version.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.ListBillOfMaterialsForProjectVersion -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -projectVersionUrl https://52.213.63.29/api/projects/2b8e2496-891a-42b7-abcd-5ae0cd527fd7/versions/32b7d1da-e62d-41a6-845d-29b7add91428
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class ListBillOfMaterialsForProjectVersion extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(ListBillOfMaterialsForProjectVersion.class);

    private static final String PROJECT_VERSION_URL_PARAMETER = "projectVersionUrl";

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

        List<ProjectVersionComponentView> views = blackDuckApiClient.getAllResponses(new UrlMultipleResponses(url, ProjectVersionComponentView.class));
        return (views != null) ? Optional.of(views) : Optional.empty();
    }

    /**
     * Outputs the BoM to the log.
     * @param bomItems List of ProjectVersionComponentView objects.
     */
    public void listBoM(Optional<List<ProjectVersionComponentView>> bomItems) {
        if (bomItems.isPresent()) {
            log.info("-----------------------");
            log.info(bomItems.get().size() + " item(s) in the bill of materials.");

            log.info("-----------------------");
            log.info("component_name");
            log.info("-----------------------");

            for (ProjectVersionComponentView bomItem : bomItems.get()) {
                String output = bomItem.getComponentName();
                log.info(output);
            }
            log.info("-----------------------");
        } else {
            log.info("-----------------------");
            log.error("No items found in the bill of materials.");
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

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String projectVersionUrl = getRequiredSingleOptionParameterValue(commandLine.get(), PROJECT_VERSION_URL_PARAMETER);

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

            ListBillOfMaterialsForProjectVersion listBillOfMaterialsForProjectVersion = new ListBillOfMaterialsForProjectVersion();
            if (listBillOfMaterialsForProjectVersion.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Listing BoM for [" + projectVersionUrl + "]");

                    // Get the list of BoM entries
                    Optional<List<ProjectVersionComponentView>> bomItems = listBillOfMaterialsForProjectVersion.getBoMEntriesForProjectVersion(restConnector, projectVersionUrl);
                    listBillOfMaterialsForProjectVersion.listBoM(bomItems);
                } catch (IntegrationException e) {
                    log.error("Failed to list BoM for [" + projectVersionUrl + "] as BoM could not be loaded for the project version due to : " + e.getMessage(), e);
                }
            }
        }
    }
}
