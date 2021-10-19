package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.blackduck.util.UrlUtils;
import com.synopsys.integration.blackduck.api.core.BlackDuckResponse;
import com.synopsys.integration.blackduck.api.core.response.UrlMultipleResponses;
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
 * Finds Black Duck KnowledgeBase components by Suite ID, i.e. based on the Protex/Code Center KnowledgeBase ID.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.FindKBComponentsBySuiteID -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -componentid "jsoninjava496227"
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class FindKBComponentsBySuiteID extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(FindKBComponentsBySuiteID.class);

    static final String COMPONENT_ID_PARAMETER = "componentid";
    static final String COMPONENT_RELEASE_ID_PARAMETER = "componentreleaseid";

    /**
     * Calls the components REST API to retrieve the KnowledgeBase components that match by Suite ID.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param componentID the Suite KB Component ID.
     * @param componentReleaseID the Suite KB Component Release ID (optional).
     * @return List of SimpleComponentView objects.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<SimpleComponentView>> findKBComponents(BlackDuckRestConnector restConnector, String componentID, Optional<String> componentReleaseID) throws IntegrationException {
        try {
            StringBuilder queryUrl = new StringBuilder();
            queryUrl.append(restConnector.getBlackDuckServicesFactory().getApiDiscovery().metaComponentsLink().getUrl().string());
            queryUrl.append("?q=bdsuite:").append(UrlUtils.encode(componentID));

            if (componentReleaseID.isPresent()) {
                queryUrl.append("%23").append(UrlUtils.encode(componentReleaseID.get()));
            }
            queryUrl.append("&limit=9999");

            log.info("Using [" + queryUrl.toString() + "] to find hub match for protex component");

            List<SimpleComponentView> components = restConnector.getBlackDuckApiClient().getAllResponses(new UrlMultipleResponses<>(new HttpUrl(queryUrl.toString()), SimpleComponentView.class));

            return (components != null) ? Optional.of(components) : Optional.empty();
        } catch (IntegrationException e) {
            log.error("Failed to get Matching Components for [" + componentID + "] on [" + restConnector.getServerUrl() + "]");
            throw e;
        }
    }

    /**
     * Outputs the components to the log.
     * @param componentID the component id to match.
     * @param components List of SimpleComponentView objects.
     */
    public void listComponents(String componentID, Optional<List<SimpleComponentView>> components) {
        if (components.isPresent()) {
            log.info("-----------------------");
            log.info(components.get().size() + " KnowledgeBase component(s) matching Suite Component ID [" + componentID + "] found.");

            log.info("-----------------------");
            log.info("name,component");
            log.info("-----------------------");

            for (SimpleComponentView component : components.get()) {
                String output = component.getComponentName() + SEPARATOR + component.getComponent() + " ";
                log.info(output);
            }
            log.info("-----------------------");
        } else {
            log.info("-----------------------");
            log.error("No KnowledgeBase components found matching Suite Component ID [" + componentID + "] - not found.");
            log.info("-----------------------");
        }
    }

    /**
     * Example shows how to find KB Component by the Suite KB Component ID.
     *
     * @param args command line parameters.
     */
    public static void main(String... args) {
        Options options = createCommandLineOptions();

        addRequiredSingleOptionParameter(options, COMPONENT_ID_PARAMETER, "The Suite component id");
        Option componentReleaseIDOption = Option.builder(COMPONENT_RELEASE_ID_PARAMETER).argName(COMPONENT_RELEASE_ID_PARAMETER).desc("The Suite component release id (optional).").hasArg().build();
        options.addOption(componentReleaseIDOption);

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String componentID = getRequiredSingleOptionParameterValue(commandLine.get(), COMPONENT_ID_PARAMETER);
            String componentReleaseIDValue = commandLine.get().getOptionValue(COMPONENT_RELEASE_ID_PARAMETER);
            Optional<String> componentReleaseID = (componentReleaseIDValue != null) ? Optional.of(componentReleaseIDValue) : Optional.empty();

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

            FindKBComponentsBySuiteID findKBComponents = new FindKBComponentsBySuiteID();
            if (findKBComponents.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Finding KnowledgeBase components matching Suite Component ID [" + componentID + "] on [" + blackDuckInstance.getServerUrl() + "]");

                    Optional<List<SimpleComponentView>> components = findKBComponents.findKBComponents(restConnector, componentID, componentReleaseID);
                    findKBComponents.listComponents(componentID, components);
                } catch (IntegrationException e) {
                    log.error("Failed to find KnowledgeBase components matching Suite Component ID [" + componentID + "] on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                }
            }
        }
    }

    private static class SimpleComponentView extends BlackDuckResponse {
        private String componentName;
        private String component;

        String getComponentName() {
            return componentName;
        }

        void setComponentName(String componentName) {
            this.componentName = componentName;
        }

        String getComponent() {
            return component;
        }

        void setComponent(String component) {
            this.component = component;
        }
    }
}
