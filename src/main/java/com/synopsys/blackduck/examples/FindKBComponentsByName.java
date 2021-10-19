package com.synopsys.blackduck.examples;

import com.google.gson.Gson;
import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.blackduck.util.UrlUtils;
import com.synopsys.integration.blackduck.api.core.BlackDuckView;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.response.Response;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Finds KnowledgeBase components by name.  This will find components in the KnowledgeBase by name.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.FindKBComponentsByName -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -componentname "apache log4j"
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class FindKBComponentsByName extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(FindKBComponentsByName.class);

    /**
     * Calls the components REST API to retrieve the KnowledgeBase components that match by name.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param componentName the component name to match.
     * @return Array of AutoCompleteResult objects.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<AutoCompleteResult[]> findKBComponents(BlackDuckRestConnector restConnector, String componentName) throws IntegrationException {
        BlackDuckApiClient blackDuckApiClient = restConnector.getBlackDuckApiClient();

        Response response = blackDuckApiClient.get(new HttpUrl(restConnector.getServerUrl() + "/api/autocomplete/component?ownership=1&q=" + UrlUtils.encode(componentName)));

        if (response.isStatusCodeSuccess()) {
            String content = response.getContentString();
            Gson gson = new Gson();
            AutoCompleteResult[] matchingComponents = gson.fromJson(content, AutoCompleteResult[].class);
            return (matchingComponents != null) ? Optional.of(matchingComponents) : Optional.empty();
        } else {
            throw new IntegrationException("Failed to find KB Components - returned [" + response.getStatusCode() + "] : " + response.getStatusMessage());
        }
    }

    /**
     * Outputs the components to the log.
     * @param componentName the component name to match.
     * @param components Array of AutoCompleteResult objects.
     */
    public void listComponents(String componentName, Optional<AutoCompleteResult[]> components) {
        if (components.isPresent()) {
            log.info("-----------------------");
            log.info(components.get().length + " KnowledgeBase component(s) matching [" + componentName + "] found.");

            log.info("-----------------------");
            log.info("value,url,ownership,fields...");
            log.info("-----------------------");

            for (AutoCompleteResult component : components.get()) {
                String output = component.getValue() + SEPARATOR + component.getUrl() + " ";
                for (Map.Entry field : component.getFields().entrySet()) {
                    String[] values = (String[]) field.getValue();
                    for (String value : values) {
                        output = output + SEPARATOR + field.getKey() + "=" + value + " ";
                    }
                }
                log.info(output);
            }
            log.info("-----------------------");
        } else {
            log.info("-----------------------");
            log.error("No KnowledgeBase components found matching name [" + componentName + "] - not found.");
            log.info("-----------------------");
        }
    }

    /**
     * Example shows how to find KB components by name.
     *
     * @param args command line parameters.
     */
    public static void main(String... args) {
        Options options = createCommandLineOptions();

        addRequiredSingleOptionParameter(options, FindComponentsByName.COMPONENT_NAME_PARAMETER, "The component name");

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String componentName = getRequiredSingleOptionParameterValue(commandLine.get(), FindComponentsByName.COMPONENT_NAME_PARAMETER);

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

            FindKBComponentsByName findKBComponents = new FindKBComponentsByName();
            if (findKBComponents.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Finding KnowledgeBase components matching name [" + componentName + "] on [" + blackDuckInstance.getServerUrl() + "]");

                    Optional<AutoCompleteResult[]> components = findKBComponents.findKBComponents(restConnector, componentName);
                    findKBComponents.listComponents(componentName, components);
                } catch (IntegrationException e) {
                    log.error("Failed to find KnowledgeBase components matching name [" + componentName + "] on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                }
            }
        }
    }

    private static class AutoCompleteResult extends BlackDuckView {
        private String value;
        private String url;
        private Map<String, String[]> fields;

        AutoCompleteResult() {
            fields = new HashMap<>();
        }

        String getValue() {
            return value;
        }

        void setValue(String value) {
            this.value = value;
        }

        String getUrl() {
            return url;
        }

        void setUrl(String url) {
            this.url = url;
        }

        Map<String, String[]> getFields() {
            return fields;
        }

        void setFields(Map<String, String[]> fields) {
            this.fields = fields;
        }
    }
}
