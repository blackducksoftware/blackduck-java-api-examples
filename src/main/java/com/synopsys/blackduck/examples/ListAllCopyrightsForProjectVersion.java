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
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Lists copyrights for all component origins within a project version's bill of materials.  Used as an example
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.11.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.ListAllCopyrightsForProjectVersion -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -projectVersionUrl https://52.213.63.29/api/projects/2b8e2496-891a-42b7-abcd-5ae0cd527fd7/versions/32b7d1da-e62d-41a6-845d-29b7add91428
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class ListAllCopyrightsForProjectVersion extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(ListAllCopyrightsForProjectVersion.class);

    private static final String PROJECT_VERSION_URL_PARAMETER = "projectVersionUrl";

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

    public void handleBoMComponent(BlackDuckRestConnector restConnector, ProjectVersionComponentView bomItem, Stats stats) {
        stats.components++;
        if (bomItem.getOrigins() == null || bomItem.getOrigins().size() < 1) {
            // All origins applies to this component as no origins are listed.  We need to load all origins separately.
            Optional<List<OriginView>> allOrigins = getAllOriginsForComponent(restConnector, bomItem);
            if (allOrigins.isPresent()) {
                for (OriginView origin : allOrigins.get()) {
                    stats.origins++;
                    handleComponentOrigin(restConnector, bomItem, origin.getOriginName(), origin.getMeta().getHref(), stats);
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
                    handleComponentOrigin(restConnector, bomItem, origin.getName(), originUrl, stats);
                }
            }
        }
    }

    public void handleComponentOrigin(BlackDuckRestConnector restConnector, ProjectVersionComponentView component, String originName, HttpUrl originUrl, Stats stats) {
        // For each origin get the copyrights
        Optional<List<ComponentVersionOriginCopyright>> copyrights = getCopyrightsForOrigin(restConnector, originUrl);
        int activeCount = 0;
        if (copyrights.isPresent() && copyrights.get().size() > 0) {
            // For each copyright list it.
            for (ComponentVersionOriginCopyright copyright : copyrights.get()) {
                if (copyright.active) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(component.getComponentName()).append(",");
                    sb.append(originName).append(",");
                    sb.append(copyright.getKbCopyright().replace("\n", " ").replace("\r", " "));
                    log.info(sb.toString());
                    stats.copyrights++;
                    activeCount++;
                }
            }
        }
        if (activeCount == 0) {
            log.info("No copyrights for component [" + component.getComponentName() + "] origin [" + originUrl.string() + "]");
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

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {
            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());
            String projectVersionUrl = getRequiredSingleOptionParameterValue(commandLine.get(), PROJECT_VERSION_URL_PARAMETER);

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);

            ListAllCopyrightsForProjectVersion listCopyrightsForProjectVersion = new ListAllCopyrightsForProjectVersion();
            if (listCopyrightsForProjectVersion.validateBlackDuckConnection(restConnector)) {
                try {
                    log.info("Listing copyrights for [" + projectVersionUrl + "]");
                    log.info("Loads all components, for each component loads the origins in the bill of materials and for each origin lists the copyrights....");
                    log.info("Output is in format : component_name,origin_name,copyright_with_newlines_removed");

                    Stats stats = new Stats();

                    // Get the list of BoM entries
                    Optional<List<ProjectVersionComponentView>> bomItems = listCopyrightsForProjectVersion.getBoMEntriesForProjectVersion(restConnector, projectVersionUrl);
                    if (bomItems.isPresent() && bomItems.get().size() > 0) {
                        // For each get the list of origins.
                        for (ProjectVersionComponentView bomItem : bomItems.get()) {
                            log.info("Handling component [" + bomItem.getComponentName() + "]");
                            listCopyrightsForProjectVersion.handleBoMComponent(restConnector, bomItem, stats);

                        }
                        log.info("...Complete...");
                        log.info("Component count [" + stats.components + "]");
                        log.info("Component origin count [" + stats.origins + "]");
                        log.info("Total number of copyrights [" + stats.copyrights + "]");
                        log.info("...End...");
                    } else {
                        log.info("No items in the Bill of Materials for [" + projectVersionUrl + "]");
                    }
                } catch (IntegrationException e) {
                    log.error("Failed to list copyrights for [" + projectVersionUrl + "] as BoM could not be loaded for the project version due to : " + e.getMessage(), e);
                }
            }
        }
    }

    public static class Stats {
        private int components = 0;
        private int origins = 0;
        private int copyrights = 0;
    }

    public static class ComponentVersionOriginCopyright extends BlackDuckView {
        private Boolean active;
        private List<String> fileSha1s;
        private String kbCopyright;
        private OriginSourceType source;
        private Date updatedAt;
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
