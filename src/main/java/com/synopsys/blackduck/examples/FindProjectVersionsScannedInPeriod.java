package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.integration.blackduck.api.manual.component.VersionBomCodeLocationBomComputedNotificationContent;
import com.synopsys.integration.blackduck.api.manual.enumeration.NotificationType;
import com.synopsys.integration.blackduck.api.manual.view.NotificationView;
import com.synopsys.integration.blackduck.service.dataservice.NotificationService;
import com.synopsys.integration.blackduck.service.request.NotificationEditor;
import com.synopsys.integration.exception.IntegrationException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Finds project versions that have been scanned in the last X hours.  Utilises the notifications API.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.FindProjectVersionsScannedInPeriod -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -period 24
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class FindProjectVersionsScannedInPeriod extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(FindProjectVersionsScannedInPeriod.class);

    static final String PERIOD_PARAMETER = "period";

    /**
     * Calls the notifications REST API to retrieve the notifications on the Black Duck instance in the past X hours.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param period the period in hours to find notifications from.
     * @return List of NotificationView objects.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<NotificationView>> getNotificationsForPeriod(BlackDuckRestConnector restConnector, int period) throws IntegrationException {

        NotificationService notificationService = restConnector.getBlackDuckServicesFactory().createNotificationService();

        List<String> notificationTypesToInclude = new ArrayList<>();
        notificationTypesToInclude.add(NotificationType.VERSION_BOM_CODE_LOCATION_BOM_COMPUTED.toString());
        // Add any notification types you wish to look for.

        // E.g. https://52.213.63.19/api/notifications?limit=10000&filter=notificationType:VERSION_BOM_CODE_LOCATION_BOM_COMPUTED&startDate=2021-02-18T16:29:30.964Z
        NotificationEditor editor = new NotificationEditor(getDateForPeriodStart(period), new Date(), notificationTypesToInclude);

        List<NotificationView> matchingNotifications = notificationService.getAllNotifications(editor);
        return (matchingNotifications != null) ? Optional.of(matchingNotifications) : Optional.empty();
    }

    /**
     * Outputs the notifications to the log.
     * @param period the period in hours to find notifications from.
     * @param notifications List of NotificationView objects.
     */
    public void listNotifications(int period, Optional<List<NotificationView>> notifications) {
        if (notifications.isPresent()) {
            log.info("-----------------------");
            log.info(notifications.get().size() + " notifications(s) in the past [" + period + "] hours found.");

            Set<String> uniqueProjectVersionsScannedInPeriod = new HashSet<>();
            log.info("-----------------------");
            log.info("NOTIFICATIONS:");
            log.info("href,content");
            log.info("-----------------------");

            for (NotificationView notification : notifications.get()) {
                String output = (notification.getHref() != null ? notification.getHref().string() : "");
                output += SEPARATOR + notification.getContent().toString();
                log.info(output);
                if (notification.getType().equals(NotificationType.VERSION_BOM_CODE_LOCATION_BOM_COMPUTED)) {
                    VersionBomCodeLocationBomComputedNotificationContent content = (VersionBomCodeLocationBomComputedNotificationContent) notification.getContent();
                    uniqueProjectVersionsScannedInPeriod.add(content.getProjectVersion());
                }
            }
        } else {
            log.info("-----------------------");
            log.error("No notifications found in the past [" + period + "] hours.");
            log.info("-----------------------");
        }
    }

    /**
     * Outputs the project versions referred to in the notifications to the log.
     * @param period the period in hours to find notifications from.
     * @param notifications List of NotificationView objects.
     */
    public void listProjectVersionsFromNotifications(int period, Optional<List<NotificationView>> notifications) {
        if (notifications.isPresent()) {
            Set<String> uniqueProjectVersionsScannedInPeriod = new HashSet<>();
            for (NotificationView notification : notifications.get()) {
                if (notification.getType().equals(NotificationType.VERSION_BOM_CODE_LOCATION_BOM_COMPUTED)) {
                    VersionBomCodeLocationBomComputedNotificationContent content = (VersionBomCodeLocationBomComputedNotificationContent) notification.getContent();
                    uniqueProjectVersionsScannedInPeriod.add(content.getProjectVersion());
                }
            }
            log.info("-----------------------");
            log.info(uniqueProjectVersionsScannedInPeriod.size() + " unique project version(s) scanned in the past [" + period + "] hours found.");

            log.info("-----------------------");
            log.info("Unique Project Versions Scanned in past [" + period + "] hours:");
            log.info("href");
            log.info("-----------------------");

            for (String projectVersionHref : uniqueProjectVersionsScannedInPeriod) {
                log.info(projectVersionHref);
            }
            log.info("-----------------------");
        } else {
            log.info("-----------------------");
            log.error("No notifications found in the past [" + period + "] hours.");
            log.info("-----------------------");
        }
    }

    /**
     * Calculates the date for the period start based on the number of hours ago.
     * @param period number of hours ago.
     * @return Date date representation.
     */
    private Date getDateForPeriodStart(int period) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR_OF_DAY, -period);
        return calendar.getTime();
    }

    /**
     * Example shows how to find components by name.
     *
     * @param args command line parameters.
     */
    public static void main(String... args) {
        Options options = createCommandLineOptions();

        addRequiredSingleOptionParameter(options, PERIOD_PARAMETER, "The period to find notifications for, e.g. the last 24 hours = 24");

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String periodString = getRequiredSingleOptionParameterValue(commandLine.get(), PERIOD_PARAMETER);
            try {
                int period = Integer.parseInt(periodString);
                // Create a Rest Connector with the specified connection details.
                BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

                FindProjectVersionsScannedInPeriod findProjectVersionsScannedInPeriod = new FindProjectVersionsScannedInPeriod();
                if (findProjectVersionsScannedInPeriod.validateBlackDuckConnection(restConnector)) {
                    try {
                        log.info("Finding notifications in the past [" + period + "] on [" + blackDuckInstance.getServerUrl() + "]");

                        Optional<List<NotificationView>> notifications = findProjectVersionsScannedInPeriod.getNotificationsForPeriod(restConnector, period);
                        findProjectVersionsScannedInPeriod.listNotifications(period, notifications);
                        findProjectVersionsScannedInPeriod.listProjectVersionsFromNotifications(period, notifications);
                    } catch (IntegrationException e) {
                        log.error("Failed to find notifications in past [" + period + "] hours on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                    }
                }
            } catch (NumberFormatException e) {
                log.error("Period [" + periodString + "] must be a valid integer for the number of hours.");
            }
        }
    }
}
