package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.integration.blackduck.api.core.BlackDuckView;
import com.synopsys.integration.blackduck.api.core.ResourceLink;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Calls the REST API to perform a GET for a single result given a URL link.
 * This URL link should be the URL to a single Black Duck object e.g. a component, project, project version, license, license family etc.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.GenericGetSingleItemByHref -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -href "https://52.213.63.29/api/projects/2b4f1379-7958-4663-9b24-8f60e819869c"
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class GenericGetSingleItemByHref extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(GenericGetSingleItemByHref.class);

    static final String HREF_PARAMETER = "href";

    /**
     * Calls the REST API to retrieve a single object by href.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param href the href to call.
     * @return Optional BlackDuckView result.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<BlackDuckView> getByHref(BlackDuckRestConnector restConnector, String href) throws IntegrationException {
        BlackDuckApiClient blackDuckApiClient = restConnector.getBlackDuckApiClient();

        BlackDuckView view = blackDuckApiClient.getResponse(new HttpUrl(href), BlackDuckView.class);
        return (view != null) ? Optional.of(view) : Optional.empty();
    }

    /**
     * Outputs the object to the log.
     * @param href the href.
     * @param view BlackDuckView.
     */
    public void outputView(String href, Optional<BlackDuckView> view) {
        if (view.isPresent()) {
            log.info("-----------------------");
            log.info("JSON [" + view.get().getJson() + "]");
            log.info("-----------------------");
            if (view.get().getResourceLinks() != null) {
                log.info("Resource Links: (rel : href)");
                for (ResourceLink link : view.get().getResourceLinks()) {
                    log.info(" - " + link.getRel() + " : " + link.getHref().string());
                }
            }
            log.info("-----------------------");
        } else {
            log.info("-----------------------");
            log.error("Object at href [" + href + "] was not found.");
            log.info("-----------------------");
        }
    }

    /**
     * Example shows how to perform a GET request for an object.
     *
     * @param args command line parameters.
     */
    public static void main(String... args) {
        Options options = createCommandLineOptions();

        addRequiredSingleOptionParameter(options, HREF_PARAMETER, "The url to load");

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String href = getRequiredSingleOptionParameterValue(commandLine.get(), HREF_PARAMETER);

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

            GenericGetSingleItemByHref genericGetSingle = new GenericGetSingleItemByHref();
            if (genericGetSingle.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Loading href [" + href + "] on [" + blackDuckInstance.getServerUrl() + "]");

                    Optional<BlackDuckView> view = genericGetSingle.getByHref(restConnector, href);
                    genericGetSingle.outputView(href, view);
                } catch (IntegrationException e) {
                    log.error("Failed to load href [" + href + "] on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                }
            }
        }
    }
}
