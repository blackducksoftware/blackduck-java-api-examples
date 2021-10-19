package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.integration.blackduck.api.generated.view.UserView;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.exception.IntegrationException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Lists Users and outputs the information to the log.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.ListUsers -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class ListUsers extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(ListUsers.class);

    /**
     * Calls the users REST API to retrieve the users.
     * @param restConnector BlackDuckRestConnector to connect.
     * @return List of UserView objects.
     * @throws IntegrationException if the request was not successful.
     */
    public List<UserView> getUsers(BlackDuckRestConnector restConnector) throws IntegrationException {
        BlackDuckApiClient blackDuckApiClient = restConnector.getBlackDuckApiClient();

        return blackDuckApiClient.getAllResponses(restConnector.getBlackDuckServicesFactory().getApiDiscovery().metaUsersLink());
    }

    /**
     * Outputs the users to the log.
     * @param users List of UserView objects.
     */
    public void listUsers(List<UserView> users) {
        log.info("-----------------------");
        log.info(users.size() + " user(s) found.");

        log.info("-----------------------");
        log.info("username,email,first_name,last_name,external_username,active");
        log.info("-----------------------");

        for (UserView user : users) {
            String output = user.getUserName() + SEPARATOR + user.getEmail() + SEPARATOR + user.getFirstName();
            output += SEPARATOR + user.getLastName() + SEPARATOR;
            if (StringUtils.isNotEmpty(user.getExternalUserName())) {
                output += user.getExternalUserName();
            }
            output += SEPARATOR + user.getActive();
            log.info(output);
        }
        log.info("-----------------------");
    }

    /**
     * Example shows how to list users configured on a Black Duck instance.
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

            ListUsers listUsers = new ListUsers();
            if (listUsers.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Listing users on [" + blackDuckInstance.getServerUrl() + "]");

                    List<UserView> users = listUsers.getUsers(restConnector);
                    listUsers.listUsers(users);
                } catch (IntegrationException e) {
                    log.error("Failed to list users on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                }
            }
        }
    }
}
