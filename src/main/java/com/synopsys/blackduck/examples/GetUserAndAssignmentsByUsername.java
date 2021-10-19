package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.integration.blackduck.api.core.ResourceLink;
import com.synopsys.integration.blackduck.api.core.response.UrlMultipleResponses;
import com.synopsys.integration.blackduck.api.generated.view.RoleAssignmentView;
import com.synopsys.integration.blackduck.api.generated.view.UserGroupView;
import com.synopsys.integration.blackduck.api.generated.view.UserView;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.blackduck.service.dataservice.UserGroupService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Gets a User by username along with group membership and permissions and outputs the information to the log.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.GetUserAndAssignmentsByUsername -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -username "sysadmin"
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class GetUserAndAssignmentsByUsername extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(GetUserAndAssignmentsByUsername.class);

    static final String USER_NAME_PARAMETER = "username";

    /**
     * Calls the users REST API to retrieve a user by username.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param username the username.
     * @return Optional UserView.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<UserView> getUserByName(BlackDuckRestConnector restConnector, String username) throws IntegrationException {
        UserGroupService userGroupService = restConnector.getBlackDuckServicesFactory().createUserGroupService();

        return userGroupService.getUserByUsername(username);
    }

    /**
     * Calls the users REST API to retrieve the user groups a user is a member of.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param user the UserView.
     * @return List of UserGroupView.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<UserGroupView>> getUsersGroups(BlackDuckRestConnector restConnector, Optional<UserView> user) throws IntegrationException {
        if (user.isPresent()) {
            BlackDuckApiClient blackDuckApiClient = restConnector.getBlackDuckApiClient();

            HttpUrl userGroupsLink = user.get().getFirstLink("usergroups");
            if (userGroupsLink != null) {
                List<UserGroupView> matchingGroups = blackDuckApiClient.getAllResponses(new UrlMultipleResponses(new HttpUrl(userGroupsLink.string()), UserGroupView.class));

                return (matchingGroups != null) ? Optional.of(matchingGroups) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Calls the users REST API to retrieve a users roles.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param user the UserView.
     * @return List of RoleAssignmentView.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<RoleAssignmentView>> getUserRoles(BlackDuckRestConnector restConnector, Optional<UserView> user) throws IntegrationException {
        if (user.isPresent()) {
            UserGroupService userGroupService = restConnector.getBlackDuckServicesFactory().createUserGroupService();
            List<RoleAssignmentView> roles = userGroupService.getAllRolesForUser(user.get());
            return (roles != null) ? Optional.of(roles) : Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * Outputs the user to the log.
     * @param username the username.
     * @param user UserView object.
     */
    public void outputUser(String username, Optional<UserView> user) {
        if (user.isPresent()) {
            log.info("-----------------------");
            log.info("Username [" + user.get().getUserName() + "]");
            log.info("User First Name [" + user.get().getFirstName() + "]");
            log.info("User Last Name [" + user.get().getLastName() + "]");
            if (user.get().getHref() != null) {
                log.info("User Href [" + user.get().getHref().string() + "]");
            }
            log.info("User Active [" + user.get().getActive() + "]");
            log.info("User Type [" + user.get().getType() + "]");
            log.info("User Email [" + user.get().getEmail() + "]");
            log.info("User External Username [" + user.get().getExternalUserName() + "]");
            if (user.get().getResourceLinks() != null) {
                log.info("User Resource Links: (rel : href)");
                for (ResourceLink link : user.get().getResourceLinks()) {
                    log.info(" - " + link.getRel() + " : " + link.getHref().string());
                }
            }
            log.info("-----------------------");
        } else {
            log.info("-----------------------");
            log.error("User [" + username + "] was not found.");
            log.info("-----------------------");
        }
    }

    /**
     * Outputs the user's roles and groups to the log.
     * @param user UserView object.
     * @param roles the List of RoleAssignmentView.
     * @param groups the List of UserGroupView.
     */
    public void outputRolesAndGroups(Optional<UserView> user, Optional<List<RoleAssignmentView>> roles, Optional<List<UserGroupView>> groups) {
        if (user.isPresent() && groups.isPresent()) {
            log.info("-----------------------");
            log.info("User [" + user.get().getUserName() + "] is a member of [" + groups.get().size() + "] groups.");
            log.info("User Groups:");
            log.info("-----------------------");
            log.info("name,href,external_name,created_from,active");
            log.info("-----------------------");
            for (UserGroupView group : groups.get()) {
                String output = group.getName() + SEPARATOR + group.getHref().string() + " " + SEPARATOR + group.getExternalName() + SEPARATOR;
                output = output + group.getCreatedFrom() + SEPARATOR + group.getActive();
                log.info(output);
            }
            log.info("-----------------------");
        }
        if (user.isPresent() && roles.isPresent()) {
            log.info("-----------------------");
            log.info("User [" + user.get().getUserName() + "] has [" + roles.get().size() + "] roles.");
            log.info("User Roles:");
            log.info("-----------------------");
            log.info("name,description,role,scope,href");
            log.info("-----------------------");
            for (RoleAssignmentView role : roles.get()) {
                String output = role.getName() + SEPARATOR + role.getDescription() + SEPARATOR + role.getRole() + " " + SEPARATOR + role.getScope();
                output = output + " " + SEPARATOR + role.getHref().string();
                log.info(output);
            }
            log.info("-----------------------");
        }
    }

    /**
     * Example shows how to get a user by name.
     *
     * @param args command line parameters.
     */
    public static void main(String... args) {
        Options options = createCommandLineOptions();

        addRequiredSingleOptionParameter(options, USER_NAME_PARAMETER, "The userame (case sensitive)");

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String username = getRequiredSingleOptionParameterValue(commandLine.get(), USER_NAME_PARAMETER);

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

            GetUserAndAssignmentsByUsername getUser = new GetUserAndAssignmentsByUsername();
            if (getUser.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Loading user [" + username + "] on [" + blackDuckInstance.getServerUrl() + "]");

                    Optional<UserView> user = getUser.getUserByName(restConnector, username);
                    getUser.outputUser(username, user);

                    if (user.isPresent()) {
                        Optional<List<UserGroupView>> groups = getUser.getUsersGroups(restConnector, user);
                        Optional<List<RoleAssignmentView>> roles = getUser.getUserRoles(restConnector, user);
                        getUser.outputRolesAndGroups(user, roles, groups);
                    }
                } catch (IntegrationException e) {
                    log.error("Failed to load user [" + username + "] on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                }
            }
        }
    }
}
