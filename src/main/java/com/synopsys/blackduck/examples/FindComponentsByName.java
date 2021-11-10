package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.blackduck.util.UrlUtils;
import com.synopsys.integration.blackduck.api.core.response.UrlMultipleResponses;
import com.synopsys.integration.blackduck.api.generated.view.ComponentView;
import com.synopsys.integration.blackduck.http.BlackDuckRequestBuilder;
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
 * Finds components by name.  This will find components that have been added to your Black Duck server and not KnowledgeBase components that have not been added.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.FindComponentsByName -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -componentname "apache"
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class FindComponentsByName extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(FindComponentsByName.class);

    static final String COMPONENT_NAME_PARAMETER = "componentname";

    /**
     * Calls the components REST API to retrieve the components on the Black Duck instance that match by name.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param componentName the component name to match.
     * @return List of ComponentView objects.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<ComponentView>> findComponents(BlackDuckRestConnector restConnector, String componentName) throws IntegrationException {
        BlackDuckApiClient blackDuckApiClient = restConnector.getBlackDuckApiClient();
        BlackDuckRequestBuilder requestBuilder = new BlackDuckRequestBuilder();
        requestBuilder.addHeader(INTERNAL_API_HEADER_NAME, INTERNAL_API_HEADER_VALUE);
        HttpUrl url = new HttpUrl(restConnector.getBlackDuckServicesFactory().getApiDiscovery().metaComponentsLink().getUrl().toString() + "?q=name:" + UrlUtils.encode(componentName));

        List<ComponentView> matchingComponents = blackDuckApiClient.getAllResponses(requestBuilder.buildBlackDuckRequest(new UrlMultipleResponses<>(url, ComponentView.class)));

        return (matchingComponents != null) ? Optional.of(matchingComponents) : Optional.empty();
    }

    /**
     * Outputs the components to the log.
     * @param componentName the component name to match.
     * @param components List of ComponentView objects.
     */
    public void listComponents(String componentName, Optional<List<ComponentView>> components) {
        if (components.isPresent()) {
            log.info("-----------------------");
            log.info(components.get().size() + " component(s) matching [" + componentName + "] found.");

            log.info("-----------------------");
            log.info("name,description,href,url,notes,primary_language");
            log.info("-----------------------");

            for (ComponentView component : components.get()) {
                String output = component.getName() + SEPARATOR + component.getDescription() + SEPARATOR;
                output += (component.getHref() != null ? component.getHref().string() : "");
                output += SEPARATOR + component.getUrl() + SEPARATOR + component.getNotes() + SEPARATOR + component.getPrimaryLanguage();
                log.info(output);
            }
            log.info("-----------------------");
        } else {
            log.info("-----------------------");
            log.error("No components found matching name [" + componentName + "] - not found.");
            log.info("-----------------------");
        }
    }

    /**
     * Example shows how to find components by name.
     *
     * @param args command line parameters.
     */
    public static void main(String... args) {
        Options options = createCommandLineOptions();

        addRequiredSingleOptionParameter(options, COMPONENT_NAME_PARAMETER, "The component name");

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String componentName = getRequiredSingleOptionParameterValue(commandLine.get(), COMPONENT_NAME_PARAMETER);

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

            FindComponentsByName findComponents = new FindComponentsByName();
            if (findComponents.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Finding components matching name [" + componentName + "] on [" + blackDuckInstance.getServerUrl() + "]");

                    Optional<List<ComponentView>> components = findComponents.findComponents(restConnector, componentName);
                    findComponents.listComponents(componentName, components);
                } catch (IntegrationException e) {
                    log.error("Failed to find components matching name [" + componentName + "] on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                }
            }
        }
    }
}
