package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.response.Response;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Simple example for how to connect to a Black Duck instance and validate the connection.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.ValidateBlackDuckConnection -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class ValidateBlackDuckConnection extends BaseBlackDuckAPICommand {

    private static final Logger log = LoggerFactory.getLogger(ValidateBlackDuckConnection.class);

    /**
     * Validate the connection, this calls the /api/current-user API to retrieve the current user associated with the API token.
     * @param restConnector the Black Duck Rest Connector to use.
     * @return boolean true if the connection is valid.
     */
    public boolean validateBlackDuckConnection(BlackDuckRestConnector restConnector) {
        try {
            validateConnection(restConnector);
            return true;
        } catch (IntegrationException e) {
            return false;
        }
    }

    /**
     * Validates the connection to the Black Duck service via a call to get the current user.
     * If successful it will do nothing, if fails it will throw IntegrationException.
     *
     * @throws IntegrationException could not contact the Black Duck server.
     */
    public void validateConnection(BlackDuckRestConnector restConnector) throws IntegrationException {
        try {
            Response response = restConnector.getBlackDuckApiClient().get(new HttpUrl(restConnector.getServerUrl() + "/api/current-user"));
            if (response.getStatusCode() != 200) {
                throw new IntegrationException("Non success status code returned from Black Duck connection validation for [" + restConnector.getServerUrl() + "] : " + response.getStatusCode());
            } else {
                log.info("Validated connection to Black Duck Instance [" + restConnector.getServerUrl() + "]");
            }
        } catch (IntegrationException e) {
            log.error("Failed to validate Black Duck blackDuckInstance connection to [" + restConnector.getServerUrl() + "] due to : " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Example shows how to connect to a Black Duck Instance and validate the connection.
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

            ValidateBlackDuckConnection validateBlackDuckConnection = new ValidateBlackDuckConnection();
            validateBlackDuckConnection.validateBlackDuckConnection(restConnector);
        }
    }
}
