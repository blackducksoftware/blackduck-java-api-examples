package com.synopsys.blackduck.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.blackduck.http.client.BlackDuckHttpClient;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.log.PrintStreamIntLogger;
import com.synopsys.integration.util.IntEnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigKeys.KEYS;

/**
 * Rest Connector, connects to a Black Duck server and creates a service for calling and factory for creating more specific services e.g. License Service.
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class BlackDuckRestConnector {

    private static final Logger log = LoggerFactory.getLogger(BlackDuckRestConnector.class);

    private final BlackDuckInstance blackDuckInstance;
    private final BlackDuckServicesFactory blackDuckServicesFactory;
    private final BlackDuckApiClient blackDuckApiClient;

    /**
     * Constructs a Rest Connector for the given Black Duck instance with a default log level of INFO.
     *
     * @param blackDuckInstance the Black Duck instance to connect to.
     */
    public BlackDuckRestConnector(BlackDuckInstance blackDuckInstance) {
        this(blackDuckInstance, LogLevel.INFO);
    }

    /**
     * Constructs a Rest Connector for the given Black Duck instance with the specified log level.
     *
     * @param blackDuckInstance the Black Duck instance to connect to.
     * @param logLevel the log level to use.  Note this will log to System.out and this class can be modified to log elsewhere.
     */
    public BlackDuckRestConnector(BlackDuckInstance blackDuckInstance, LogLevel logLevel) {
        this.blackDuckInstance = blackDuckInstance;
        BlackDuckServerConfig blackDuckServerConfig = createBlackDuckServerConfig(blackDuckInstance);
        IntLogger intLogger = createIntLogger(logLevel);
        this.blackDuckServicesFactory = createBlackDuckServicesFactory(blackDuckServerConfig, intLogger);
        this.blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();
    }

    /**
     * Constructs the Black Duck Server configuration used by the blackduck-common library.
     *
     * @param blackDuckInstance the Black Duck server connection details.
     * @return BlackDuckServerConfig for use by blackduck-common.
     */
    private BlackDuckServerConfig createBlackDuckServerConfig(BlackDuckInstance blackDuckInstance) {
        final BlackDuckServerConfigBuilder builder = new BlackDuckServerConfigBuilder(KEYS.apiToken);
        builder.setUrl(blackDuckInstance.getServerUrl());
        builder.setApiToken(blackDuckInstance.getApiToken());
        builder.setTimeoutInSeconds(blackDuckInstance.getTimeoutInSeconds());
        builder.setTrustCert(blackDuckInstance.isTrustHttpsCert());

        return builder.build();
    }

    /**
     * Constructs a PrintStreamIntLogger for System.out.  This can be modified to use a different implementation.
     *
     * @param logLevel the log level to use.
     * @return IntLogger.
     */
    private static IntLogger createIntLogger(LogLevel logLevel) {
        return new PrintStreamIntLogger(System.out, logLevel);
    }

    /**
     * Constructs a BlackDuckServicesFactory that can be used to get hold of the services e.g. License Service.
     *
     * @param blackDuckServerConfig the BlackDuckServerConfig to use.
     * @param logger the logger to log output to.
     * @return BlackDuckServicesFactory.
     */
    private BlackDuckServicesFactory createBlackDuckServicesFactory(BlackDuckServerConfig blackDuckServerConfig, IntLogger logger) {
        BlackDuckHttpClient blackDuckHttpClient = blackDuckServerConfig.createBlackDuckHttpClient(logger);
        IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.empty();
        Gson gson = BlackDuckServicesFactory.createDefaultGson();
        ObjectMapper objectMapper = BlackDuckServicesFactory.createDefaultObjectMapper();

        // Note you could construct a pool of threads to handle execution.  For simplicity just using single thread here.
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        log.info("Creating Black Duck Services Factory");

        return new BlackDuckServicesFactory(intEnvironmentVariables, executorService, logger, blackDuckHttpClient, gson, objectMapper);
    }

    /**
     * Convenience method for returning the server Url of the Black Duck server.
     * Also done so we hide the API token so it isn't easily retrieved after construction of the Rest Connector.
     *
     * @return String the server url.
     */
    public String getServerUrl() {
        return this.blackDuckInstance.getServerUrl();
    }

    /**
     * Returns the BlackDuckServicesFactory for easy access to the services available.
     * @return BlackDuckServicesFactory.
     */
    public BlackDuckServicesFactory getBlackDuckServicesFactory() {
        return blackDuckServicesFactory;
    }

    /**
     * Returns the BlackDuckApiClient, this is good to call any API within BlackDuck.
     * @return BlackDuckApiClient.
     */
    public BlackDuckApiClient getBlackDuckApiClient() {
        return blackDuckApiClient;
    }
}
