package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.integration.blackduck.api.core.BlackDuckView;
import com.synopsys.integration.blackduck.api.core.ResourceLink;
import com.synopsys.integration.blackduck.api.core.response.UrlMultipleResponses;
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
 * Calls the REST API to perform a GET for a list of results given a URL link.
 * This URL link should be the URL to a list of Black Duck objects e.g. a component's versions, project's versions, project's tags, license terms, project custom fields etc.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.GenericGetMultipleItemsByHref -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -href "https://52.213.63.29/api/projects/2b4f1379-7958-4663-9b24-8f60e819869c/versions"
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class GenericGetMultipleItemsByHref extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(GenericGetMultipleItemsByHref.class);

    /**
     * Calls the REST API to retrieve multiple objects by href.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param href the href to call.
     * @return Optional List of BlackDuckView results.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<BlackDuckView>> getByHref(BlackDuckRestConnector restConnector, String href) throws IntegrationException {
        BlackDuckApiClient blackDuckApiClient = restConnector.getBlackDuckApiClient();
        HttpUrl url = new HttpUrl(href);

        List<BlackDuckView> views = blackDuckApiClient.getAllResponses(new UrlMultipleResponses<>(url, BlackDuckView.class));
        return (views != null) ? Optional.of(views) : Optional.empty();
    }

    /**
     * Outputs the objects to the log.
     * @param href the href.
     * @param views list of BlackDuckViews.
     */
    public void outputViews(String href, Optional<List<BlackDuckView>> views) {

        if (views.isPresent()) {
            log.info("-----------------------");
            log.info(views.get().size() + " object(s) found.");
            log.info("-----------------------");
            log.info("Item Urls:");
            for (BlackDuckView view : views.get()) {
                if (view.getHref() != null) {
                    log.info(" - " + view.getHref().string());
                }
            }
            log.info("-----------------------");
            log.info("Item Details");
            log.info("-----------------------");
            for (BlackDuckView view : views.get()) {
                log.info("JSON [" + view.getJson() + "]");
                log.info("-----------------------");
                if (view.getResourceLinks() != null) {
                    log.info("Resource Links: (rel : href)");
                    for (ResourceLink link : view.getResourceLinks()) {
                        log.info(" - " + link.getRel() + " : " + link.getHref().string());
                    }
                }
            }

            log.info("-----------------------");
        } else {
            log.info("-----------------------");
            log.error("Objects at href [" + href + "] - not found.");
            log.info("-----------------------");
        }
    }

    /**
     * Example shows how to perform a GET request for a list of objects.
     *
     * @param args command line parameters.
     */
    public static void main(String... args) {
        Options options = createCommandLineOptions();

        addRequiredSingleOptionParameter(options, GenericGetSingleItemByHref.HREF_PARAMETER, "The url to load");

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String href = getRequiredSingleOptionParameterValue(commandLine.get(), GenericGetSingleItemByHref.HREF_PARAMETER);

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

            GenericGetMultipleItemsByHref genericGetMulti = new GenericGetMultipleItemsByHref();
            if (genericGetMulti.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Loading href [" + href + "] on [" + blackDuckInstance.getServerUrl() + "]");

                    Optional<List<BlackDuckView>> views = genericGetMulti.getByHref(restConnector, href);
                    genericGetMulti.outputViews(href, views);
                } catch (IntegrationException e) {
                    log.error("Failed to load href [" + href + "] on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                }
            }
        }
    }
}
