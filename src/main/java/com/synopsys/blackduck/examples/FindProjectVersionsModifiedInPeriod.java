package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.blackduck.util.UrlUtils;
import com.synopsys.integration.blackduck.api.core.BlackDuckResponse;
import com.synopsys.integration.blackduck.api.core.response.UrlMultipleResponses;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.blackduck.service.dataservice.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Finds project versions that have been modified in any way in the last X hours.
 *
 * Utilises the project API, project version API and journal API.
 *
 * Loads all projects and from there all versions on each project and calls the journal API for each project version
 * to find the latest journal entry and determines if that was in the period we are interested in.
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.FindProjectVersionsModifiedInPeriod -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -period 24
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class FindProjectVersionsModifiedInPeriod extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(FindProjectVersionsModifiedInPeriod.class);

    static final String PERIOD_PARAMETER = "period";
    static final String INCLUDE_VULNS_PARAMETER = "include_vulns";

    /**
     * Finds project versions that have been modified in any way in the last X hours.
     *
     * @param restConnector BlackDuckRestConnector to connect.
     * @param period the period in hours to find modifications from.
     * @param includeVulns whether to include vulnerability findings.  If false a new vulnerability in a project will not mean it is included in the results, only edits, manually adding components, scans etc.
     * @return Set of unique project version href strings that have been modified in the period.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<Set<String>> getProjectVersionsModifiedInPeriod(BlackDuckRestConnector restConnector, int period, boolean includeVulns) throws IntegrationException {
        Set<String> uniqueProjectVersionsModified = new HashSet<>();
        Date periodStart = getDateForPeriodStart(period);

        // Create services, we do this here so we do not create them for every request.
        BlackDuckApiClient blackDuckApiClient = restConnector.getBlackDuckApiClient();
        ProjectService projectService = restConnector.getBlackDuckServicesFactory().createProjectService();

        // Load the projects.
        Optional<List<ProjectView>> allProjects = getAllProjects(projectService);

        if (allProjects.isPresent()) {
            for (ProjectView project : allProjects.get()) {

                // Load the versions for this project.
                Optional<List<ProjectVersionView>> projectVersions = getProjectVersions(projectService, project);
                if (projectVersions.isPresent()) {
                    for (ProjectVersionView projectVersion : projectVersions.get()) {

                        // Load the latest journal entry for this project version.
                        log.info("Loading latest journal entry for [" + projectVersion.getHref().string() + "]");
                        Optional<SimpleJournalView> latestJournalEntry = getLatestJournalForProjectVersion(restConnector, blackDuckApiClient, project, projectVersion, includeVulns);
                        if (latestJournalEntry.isPresent()) {
                            // Check the timestamp of the journal entry - we sorted by latest journal entry first.
                            if (latestJournalEntry.get().timestamp != null) {
                                Date journalDate = fromTimestampString(latestJournalEntry.get().timestamp);
                                if (journalDate != null) {
                                    if (journalDate.getTime() > periodStart.getTime()) {
                                        // This journal entry happened after the period start.
                                        log.info("Journal entry action [" + latestJournalEntry.get().getAction() + "] for project version [" + projectVersion.getHref().string() + "] occurred after the period start.");
                                        uniqueProjectVersionsModified.add(projectVersion.getHref().string());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return (uniqueProjectVersionsModified != null) ? Optional.of(uniqueProjectVersionsModified) : Optional.empty();
    }

    /**
     * Calls the projects REST API to retrieve the projects.
     * @param projectService ProjectService to connect so we do not construct this for every request.
     * @return List of ProjectView objects.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<ProjectView>> getAllProjects(ProjectService projectService) throws IntegrationException {
        List<ProjectView> allProjects = projectService.getAllProjects();
        return (allProjects != null) ? Optional.of(allProjects) : Optional.empty();
    }

    /**
     * Calls the projects REST API to retrieve the project versions for a project.
     * @param projectService ProjectService to connect so we do not construct this for every request.
     * @param project the project to find versions for.
     * @return List of ProjectVersionView objects.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<ProjectVersionView>> getProjectVersions(ProjectService projectService, ProjectView project) throws IntegrationException {
        List<ProjectVersionView> allProjectVersions = projectService.getAllProjectVersions(project);
        return (allProjectVersions != null) ? Optional.of(allProjectVersions) : Optional.empty();
    }

    /**
     * Calls the journals REST API for a project version to retrieve the latest single journal on the Black Duck instance.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param blackDuckApiClient BlackDuckApiClient to connect so we do not construct it for every project version.
     * @param projectView the project to find journal entries for.
     * @param projectVersionView the project version to find journal entries for.
     * @param includeVulns whether to include vulnerability findings.
     * @return SimpleJournalView the latest journal entry.
     */
    public Optional<SimpleJournalView> getLatestJournalForProjectVersion(BlackDuckRestConnector restConnector, BlackDuckApiClient blackDuckApiClient, ProjectView projectView, ProjectVersionView projectVersionView, boolean includeVulns) {
        try {
            // E.g. https://52.213.63.19/api/journal/projects/0c378012-b562-4dc1-ba56-5b267e9573dd/versions/8942913c-c4fa-498e-bb4a-ae3254b0ff7b?limit=1&sort=timestamp desc&filter=journalObjectType:COMPONENT&filter=journalObjectType:SCAN&filter=journalObjectType:SNIPPET&filter=journalObjectType:SOURCE_FILE&filter=journalObjectType:KB_COMPONENT&filter=journalObjectType:KB_COMPONENT_VERSION&filter=journalObjectType:VERSION&filter=journalObjectType:VULNERABILITY

            StringBuilder queryUrl = new StringBuilder();
            queryUrl.append(restConnector.getBlackDuckServicesFactory().getApiDiscovery().metaJournalLink().getUrl().string());
            queryUrl.append("/projects/").append(UrlUtils.getId(projectView));
            queryUrl.append("/versions/").append(UrlUtils.getId(projectVersionView));
            queryUrl.append("?sort=timestamp%20desc");
            if (includeVulns) {
                // If we are including vulns.
                queryUrl.append("&filter=journalObjectType:VULNERABILITY");
            }
            queryUrl.append("&filter=journalObjectType:COMPONENT&filter=journalObjectType:SCAN&filter=journalObjectType:SNIPPET&filter=journalObjectType:SOURCE_FILE&filter=journalObjectType:KB_COMPONENT&filter=journalObjectType:KB_COMPONENT_VERSION&filter=journalObjectType:VERSION");
            // We only want to load 1 - the latest as we sorted by timestamp desc as we only care if the latest is within the period we are looking for.
            queryUrl.append("&limit=1");

            List<SimpleJournalView> journalEntries = blackDuckApiClient.getAllResponses(new UrlMultipleResponses<>(new HttpUrl(queryUrl.toString()), SimpleJournalView.class));

            if (journalEntries != null && journalEntries.size() >= 1) {
                return Optional.of(journalEntries.get(0));
            } else {
                return Optional.empty();
            }
        } catch (IntegrationException e) {
            log.error("Failed to load journal entries for version [" + projectVersionView.getHref().string() + "] due to : " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Outputs the project versions to the log.
     * @param period the period in hours to find notifications from.
     * @param versions List of String href objects.
     */
    public void listProjectVersions(int period, Optional<Set<String>> versions) {
        if (versions.isPresent()) {
            log.info("-----------------------");
            log.info(versions.get().size() + " project version(s) modified in the past [" + period + "] hours found.");
            log.info("-----------------------");
            log.info("Project Versions Modified in the past [" + period + "] hours:");
            log.info("href");
            log.info("-----------------------");

            for (String href : versions.get()) {
                log.info(href);
            }
        } else {
            log.info("-----------------------");
            log.error("No project versions modified in the past [" + period + "] hours.");
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
        addRequiredSingleOptionParameter(options, INCLUDE_VULNS_PARAMETER, "true/false Whether to count vulnerability updates as modifications. E.g. if false a new vulnerability added to a version will not count and be listed.");

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String periodString = getRequiredSingleOptionParameterValue(commandLine.get(), PERIOD_PARAMETER);
            try {
                int period = Integer.parseInt(periodString);
                boolean includeVulns = Boolean.parseBoolean(getRequiredSingleOptionParameterValue(commandLine.get(), INCLUDE_VULNS_PARAMETER));

                // Create a Rest Connector with the specified connection details.
                BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

                FindProjectVersionsModifiedInPeriod findProjectVersionsModifiedInPeriod = new FindProjectVersionsModifiedInPeriod();
                if (findProjectVersionsModifiedInPeriod.validateBlackDuckConnection(restConnector)) {
                    try {
                        log.info("Finding project versions modified in the past [" + period + "] on [" + blackDuckInstance.getServerUrl() + "]");

                        Optional<Set<String>> modifiedInPeriod = findProjectVersionsModifiedInPeriod.getProjectVersionsModifiedInPeriod(restConnector, period, includeVulns);
                        findProjectVersionsModifiedInPeriod.listProjectVersions(period, modifiedInPeriod);
                    } catch (IntegrationException e) {
                        log.error("Failed to find project versions modified in past [" + period + "] hours on [" + blackDuckInstance.getServerUrl() + "] due to : " + e.getMessage(), e);
                    }
                }
            } catch (NumberFormatException e) {
                log.error("Period [" + periodString + "] must be a valid integer for the number of hours.");
            }
        }
    }

    private static class SimpleJournalView extends BlackDuckResponse {
        private String eventId;
        private String timestamp;
        private String action;

        public String getEventId() {
            return eventId;
        }

        public void setEventId(String eventId) {
            this.eventId = eventId;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }
}
