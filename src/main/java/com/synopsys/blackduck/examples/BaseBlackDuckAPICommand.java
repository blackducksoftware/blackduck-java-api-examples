package com.synopsys.blackduck.examples;

import com.synopsys.blackduck.api.BlackDuckInstance;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * Handles the common task of gathering the command line options for the API calls to a Black Duck instance.
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
class BaseBlackDuckAPICommand {

    private static final Logger log = LoggerFactory.getLogger(BaseBlackDuckAPICommand.class);

    static final String INTERNAL_API_HEADER_NAME = "Accept";
    static final String INTERNAL_API_HEADER_VALUE = "application/vnd.blackducksoftware.internal-1+json";

    static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(DATE_FORMAT);

    public static final String SEPARATOR = ",";

    private static final String URL_PARAMETER = "url";
    private static final String API_KEY_PARAMETER = "apikey";
    private static final String TIMEOUT_PARAMETER = "timeout";
    private static final String TRUST_HTTPS_PARAMETER = "trusthttps";

    /**
     * Creates the command line options and adds the Black Duck connection options as they are common to all API calls.
     * @return Options with Black Duck REST API required options.  You can add example specific options to this.
     */
    static Options createCommandLineOptions() {
        Options options = new Options();
        addBlackDuckInstanceOptions(options);
        return options;
    }

    /**
     * Adds the Black Duck connection options as they are common to all API calls.
     * @param options Options with Black Duck REST API required options.
     */
    private static void addBlackDuckInstanceOptions(Options options) {
        addRequiredSingleOptionParameter(options, URL_PARAMETER, "The Black Duck server url.");
        addRequiredSingleOptionParameter(options, API_KEY_PARAMETER, "The API key to connect to Black Duck with.  Note the API will have access according to the user that generated the API key.");
        Option trustHttps = Option.builder(TRUST_HTTPS_PARAMETER).argName(TRUST_HTTPS_PARAMETER).desc("Whether to trust the SSL certificate, defaults to false.  Not recommended for production.").build();
        Option timeoutInSeconds = Option.builder(TIMEOUT_PARAMETER).argName(TIMEOUT_PARAMETER).type(Integer.class).desc("The timeout in seconds.  Defaults to 20000 if not supplied.").hasArg().build();
        options.addOption(trustHttps).addOption(timeoutInSeconds);
    }

    /**
     * Helper method to add a single option required parameter.
     * @param options Options.
     * @param name the name of the parameter.
     * @param description the description of the parameter.
     */
    static void addRequiredSingleOptionParameter(Options options, String name, String description) {
        Option opt = Option.builder(name).argName(name).desc(description).required().hasArg().build();
        options.addOption(opt);
    }

    /**
     * Helper method to retrieve the parameter value provided.
     * @param cmd CommandLine.
     * @param name the name of the parameter.
     * @return the String value provided.
     */
    static String getRequiredSingleOptionParameterValue(CommandLine cmd, String name) {
        String val = cmd.getOptionValue(name);
        if (val != null) {
            val = val.trim();
        }
        return val;
    }

    /**
     * Parses a timestamp String to Date object.
     * @param timestamp String timestamp e.g. 2021-02-18T16:29:57.065Z.
     * @return Date representation.
     */
    static Date fromTimestampString(String timestamp) {
        try {
            return DATE_FORMATTER.parse(timestamp);
        } catch (java.text.ParseException e) {
            log.error("Failed to parse timestamp [" + timestamp + "] due to : " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Returns the Black Duck instance populated with the command line values.
     * @param cmd the command line options passed in.
     * @return BlackDuckInstance we want to connect to.
     */
    static BlackDuckInstance getBlackDuckInstance(CommandLine cmd) {
        String serverUrl = getRequiredSingleOptionParameterValue(cmd, URL_PARAMETER);
        String apiKey = getRequiredSingleOptionParameterValue(cmd, API_KEY_PARAMETER);
        boolean trustHttpsCert = cmd.hasOption(TRUST_HTTPS_PARAMETER);
        Integer timeout = (cmd.hasOption(TIMEOUT_PARAMETER) ? Integer.parseInt(cmd.getOptionValue("timeout")) : null);

        BlackDuckInstance blackDuckInstance = new BlackDuckInstance(serverUrl, apiKey, trustHttpsCert, timeout);

        log.info("Black Duck Server [" + serverUrl + "]");
        String apiKeyDisplay = (StringUtils.isNotEmpty(apiKey)) ? "********" : "not provided";
        log.info("API Key [" + apiKeyDisplay + "]");
        log.info("Trust Https Cert [" + trustHttpsCert + "]");
        String timeoutDisplay = (timeout != null) ? timeout.toString() : "default (20000)";
        log.info("Timeout In Seconds [" + timeoutDisplay + "]");

        return blackDuckInstance;
    }

    /**
     * Parses the command line and returns the CommandLine if successful.
     * @param options The Options to apply to the command line.
     * @param args The command line parameters.
     * @return Optional of CommandLine if successfully parsed.
     */
    static Optional<CommandLine> parseCommandLine(Options options, String... args) {
        CommandLineParser parser = new DefaultParser();
        try {
            return Optional.of(parser.parse(options, args));
        } catch (ParseException e) {
            log.error("Failed to parse command line options due to : " + e.getMessage());
            new HelpFormatter().printHelp("apikey", options);
            return Optional.empty();
        }
    }
}
