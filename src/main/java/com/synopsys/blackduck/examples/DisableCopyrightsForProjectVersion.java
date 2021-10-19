package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.integration.blackduck.api.core.BlackDuckView;
import com.synopsys.integration.blackduck.api.core.ResourceLink;
import com.synopsys.integration.blackduck.api.core.response.UrlMultipleResponses;
import com.synopsys.integration.blackduck.api.generated.enumeration.OriginSourceType;
import com.synopsys.integration.blackduck.api.generated.view.OriginView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionComponentView;
import com.synopsys.integration.blackduck.api.manual.temporary.component.VersionBomOriginView;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Disables copyrights for all component origins within a project version's bill of materials.  Used as an example
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.DisableCopyrightsForProjectVersion -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -projectVersionUrl https://52.213.63.29/api/projects/2b8e2496-891a-42b7-abcd-5ae0cd527fd7/versions/32b7d1da-e62d-41a6-845d-29b7add91428
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class DisableCopyrightsForProjectVersion extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(DisableCopyrightsForProjectVersion.class);

    private static final String PROJECT_VERSION_URL_PARAMETER = "projectVersionUrl";
    private static final String ONLY_DISABLE_MULTIPLE_COPYRIGHTS = "ignoreSingleCopyrights";

    private static final int RETRY_COUNT = 5;

    /**
     * Calls the BoM entries for a project version.
     * @param restConnector BlackDuckRestConnector to connect.
     * @param projectVersionUrl the project version url.
     * @return List of ProjectView objects.
     * @throws IntegrationException if the request was not successful.
     */
    public Optional<List<ProjectVersionComponentView>> getBoMEntriesForProjectVersion(BlackDuckRestConnector restConnector, String projectVersionUrl) throws IntegrationException {
        BlackDuckApiClient blackDuckApiClient = restConnector.getBlackDuckApiClient();
        // Note this URL will filter the results to show not ignored items.
        HttpUrl url = new HttpUrl(projectVersionUrl + "/components?sort=projectName%20ASC&filter=bomInclusion%3Afalse&filter=bomMatchInclusion%3Afalse");

        List<ProjectVersionComponentView> views = blackDuckApiClient.getAllResponses(new UrlMultipleResponses<>(url, ProjectVersionComponentView.class));
        return (views != null) ? Optional.of(views) : Optional.empty();
    }

    public Optional<List<OriginView>> getAllOriginsForComponent(BlackDuckRestConnector restConnector, ProjectVersionComponentView component) {
        try {
            HttpUrl link = component.getFirstLink("origins");
            if (link != null) {
                try {
                    List<OriginView> views = restConnector.getBlackDuckApiClient().getAllResponses(new UrlMultipleResponses<>(link, OriginView.class));
                    return (views != null) ? Optional.of(views) : Optional.empty();
                } catch (IntegrationException e) {
                    log.error("Failed to load all origins for component [" + component.getComponentName() + "] due to : " + e.getMessage(), e);
                }
            }
            return Optional.empty();
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    public Optional<List<ComponentVersionOriginCopyright>> getCopyrightsForOrigin(BlackDuckRestConnector restConnector, HttpUrl originUrl) {
        try {
            HttpUrl copyrightsUrl = new HttpUrl(originUrl.string() + "/copyrights");
            List<ComponentVersionOriginCopyright> views = restConnector.getBlackDuckApiClient().getAllResponses(new UrlMultipleResponses<>(copyrightsUrl, ComponentVersionOriginCopyright.class));
            return (views != null) ? Optional.of(views) : Optional.empty();
        } catch (IntegrationException e) {
            log.error("Failed to load copyrights for component origin [" + originUrl.string() + "] due to : " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    public boolean disableCopyright(BlackDuckRestConnector restConnector, ComponentVersionOriginCopyright copyright, Stats stats) {
        if (Boolean.FALSE.equals(copyright.getActive())) {
            log.info("Copyright is already inactive - skipping [" + copyright.getMeta().getHref().string() + "]");
            stats.skipped++;
        } else {
            copyright.setActive(Boolean.FALSE);
            try {
                restConnector.getBlackDuckApiClient().put(copyright);
                log.info("Disabled copyright [" + copyright.getMeta().getHref().string() + "]");
                stats.updated++;
                return true;
            } catch (IntegrationException e) {
                stats.failures.add(copyright.getMeta().getHref().string());
                stats.errors++;
                log.error("Failed to disable copyright [" + copyright.getMeta().getHref().string() + "] due to : " + e.getMessage());
            }
        }
        return false;
    }

    public List<String> validateDisabledCopyrightsForOrigin(BlackDuckRestConnector restConnector, HttpUrl originUrl, boolean onlyDisableMultiple) {
        // Reload the copyrights.
        List<String> validationFailures = new ArrayList<>();
        Optional<List<ComponentVersionOriginCopyright>> copyrights = getCopyrightsForOrigin(restConnector, originUrl);
        if (copyrights.isPresent() && copyrights.get().size() > 0) {
            // For each copyright validate it is deactivated.
            if (copyrights.get().size() == 1 && onlyDisableMultiple) {
                // Do nothing.
            } else {

                for (ComponentVersionOriginCopyright copyright : copyrights.get()) {
                    if (!Boolean.FALSE.equals(copyright.getActive())) {
                        validationFailures.add(copyright.getMeta().getHref().string());
                    }
                }
            }

        }
        return validationFailures;
    }

    public void handleBoMComponent(BlackDuckRestConnector restConnector, ProjectVersionComponentView bomItem, Stats stats, boolean onlyDisableMultiple) {
        stats.components++;
        if (bomItem.getOrigins() == null || bomItem.getOrigins().size() < 1) {
            // All origins applies to this component as no origins are listed.  We need to load all origins separately.
            Optional<List<OriginView>> allOrigins = getAllOriginsForComponent(restConnector, bomItem);
            if (allOrigins.isPresent()) {
                for (OriginView origin : allOrigins.get()) {
                    stats.origins++;
                    handleComponentOrigin(restConnector, origin.getMeta().getHref(), stats, onlyDisableMultiple, 1);
                }
            }
        } else {
            for (VersionBomOriginView origin : bomItem.getOrigins()) {
                stats.origins++;
                // Get the href link.
                HttpUrl originUrl = null;
                if (origin.getMeta() != null && origin.getMeta().getHref() != null) {
                    originUrl = origin.getMeta().getHref();
                } else {
                    Optional<HttpUrl> optionalHttpUrl = getLink(origin, "origin");
                    if (optionalHttpUrl.isPresent()) {
                        originUrl = optionalHttpUrl.get();
                    } else {
                        log.error("Failed to find origin href for origin [" + origin + "] - skipping this origin.");
                    }
                }
                if (originUrl != null) {
                    handleComponentOrigin(restConnector, originUrl, stats, onlyDisableMultiple, 1);
                }
            }
        }
    }

    public void handleComponentOrigin(BlackDuckRestConnector restConnector, HttpUrl originUrl, Stats stats, boolean onlyDisableMultiple, int attempt) {
        // For each origin get the copyrights
        Optional<List<ComponentVersionOriginCopyright>> copyrights = getCopyrightsForOrigin(restConnector, originUrl);
        if (copyrights.isPresent() && copyrights.get().size() > 0) {
            // For each copyright deactivate it.
            if (copyrights.get().size() == 1 && onlyDisableMultiple) {
                if (attempt == 1) {
                    stats.copyrights++;
                    log.info("Single copyright - skipping at user's request for [" + originUrl.string() + "]");
                }
            } else {
                boolean anyChanged = false;
                for (ComponentVersionOriginCopyright copyright : copyrights.get()) {
                    stats.copyrights++;
                    // Disabling copyright.
                    if (disableCopyright(restConnector, copyright, stats)) {
                        anyChanged = true;
                    }
                }
                if (anyChanged) {
                    List<String> validationFailures = validateDisabledCopyrightsForOrigin(restConnector, originUrl, onlyDisableMultiple);
                    if (validationFailures.size() > 0) {
                        if (attempt <= RETRY_COUNT) {
                            attempt++;
                            log.info("Retrying attempt number [" + attempt + "] - there were [" + validationFailures.size() + "] copyrights still enabled after PUT to disable them - component [" + originUrl + "]");
                            handleComponentOrigin(restConnector, originUrl, stats, onlyDisableMultiple, attempt);
                        } else {
                            log.info("Retry maximum reached - still [" + validationFailures.size() + "] copyrights enabled despite retrying to disable them [" + RETRY_COUNT + "] times.");
                            for (String validationFailure : validationFailures) {
                                stats.validationFailures++;
                                log.error("Validation failed - copyright is not disabled after PUT to active=false for [" + validationFailure + "]");
                            }

                        }
                    }
                }
            }

        } else {
            log.info("No copyrights for component origin [" + originUrl.string() + "]");
        }
    }

    public static Optional<HttpUrl> getLink(VersionBomOriginView origin, String linkString) {
        if (origin != null && origin.getMeta() != null && origin.getMeta().getLinks() != null) {
            for (ResourceLink link : origin.getMeta().getLinks()) {
                if (link.getRel().equals(linkString)) {
                    return Optional.of(link.getHref());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Example command line.
     *
     * @param args command line parameters.
     */
    public static void main(String... args) {
        Options options = createCommandLineOptions();

        addRequiredSingleOptionParameter(options, PROJECT_VERSION_URL_PARAMETER, "The project version url e.g. https://52.213.63.29/api/projects/2b8e2496-891a-42b7-abcd-5ae0cd527fd7/versions/32b7d1da-e62d-41a6-845d-29b7add91428");
        options.addOption(Option.builder(ONLY_DISABLE_MULTIPLE_COPYRIGHTS).argName(ONLY_DISABLE_MULTIPLE_COPYRIGHTS).desc("Whether to only disable multiple copyrights, e.g. if a component has a single copyright it will skip it and leave it active.").build());

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String projectVersionUrl = getRequiredSingleOptionParameterValue(commandLine.get(), PROJECT_VERSION_URL_PARAMETER);
            boolean onlyDisableMultiple = commandLine.get().hasOption(ONLY_DISABLE_MULTIPLE_COPYRIGHTS);

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

            DisableCopyrightsForProjectVersion disableCopyrightsForProjectVersion = new DisableCopyrightsForProjectVersion();
            if (disableCopyrightsForProjectVersion.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Disabling copyrights for [" + projectVersionUrl + "] - onlyDisableMultipleCopyrights [" + onlyDisableMultiple + "]");

                    Stats stats = new Stats();

                    // Get the list of BoM entries
                    Optional<List<ProjectVersionComponentView>> bomItems = disableCopyrightsForProjectVersion.getBoMEntriesForProjectVersion(restConnector, projectVersionUrl);
                    if (bomItems.isPresent() && bomItems.get().size() > 0) {
                        // For each get the list of origins.
                        for (ProjectVersionComponentView bomItem : bomItems.get()) {
                            log.info("Handling component [" + bomItem.getComponentName() + "]");
                            disableCopyrightsForProjectVersion.handleBoMComponent(restConnector, bomItem, stats, onlyDisableMultiple);

                        }
                        log.info("...Complete...");
                        log.info("There were [" + stats.failures.size() + "] errors.");
                        if (stats.failures.size() > 0) {
                            log.info("The following copyrights either failed to update or failed to validate after updating:");
                            for (String failure : stats.failures) {
                                log.info(" - " + failure);
                            }
                        }
                        log.info("Component count [" + stats.components + "]");
                        log.info("Component origin count [" + stats.origins + "]");
                        log.info("Total number of copyrights [" + stats.copyrights + "]");
                        log.info("Updated Copyrights [" + stats.updated + "]");
                        log.info("Skipped Copyrights (already disabled) [" + stats.skipped + "]");
                        log.info("Failed to disable count [" + stats.errors + "]");
                        log.info("Validation - failed to disable count [" + stats.validationFailures + "]");
                        log.info("...End...");
                    } else {
                        log.info("No items in the Bill of Materials for [" + projectVersionUrl + "]");
                    }
                } catch (IntegrationException e) {
                    log.error("Failed to disable copyrights for [" + projectVersionUrl + "] as BoM could not be loaded for the project version due to : " + e.getMessage(), e);
                }
            }
        }
    }

    public static class Stats {
        private int components = 0;
        private int origins = 0;
        private int copyrights = 0;
        private int skipped = 0;
        private int updated = 0;
        private int validationFailures = 0;
        private int errors = 0;
        private List<String> failures = new ArrayList<>();
    }

    public static class ComponentVersionOriginCopyright extends BlackDuckView {
        private Boolean active;
        private java.util.List<String> fileSha1s;
        private String kbCopyright;
        private OriginSourceType source;
        private java.util.Date updatedAt;
        private String updatedBy;
        private String updatedCopyright;

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }

        public List<String> getFileSha1s() {
            return fileSha1s;
        }

        public void setFileSha1s(List<String> fileSha1s) {
            this.fileSha1s = fileSha1s;
        }

        public String getKbCopyright() {
            return kbCopyright;
        }

        public void setKbCopyright(String kbCopyright) {
            this.kbCopyright = kbCopyright;
        }

        public OriginSourceType getSource() {
            return source;
        }

        public void setSource(OriginSourceType source) {
            this.source = source;
        }

        public Date getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Date updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getUpdatedBy() {
            return updatedBy;
        }

        public void setUpdatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
        }

        public String getUpdatedCopyright() {
            return updatedCopyright;
        }

        public void setUpdatedCopyright(String updatedCopyright) {
            this.updatedCopyright = updatedCopyright;
        }
    }
}
