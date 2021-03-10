package com.synopsys.blackduck.api;

import java.util.Objects;

/**
 * Model object for connection details to a single Black Duck instance.
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class BlackDuckInstance {
    private String apiToken = null;
    private String serverUrl = null;
    private boolean trustHttpsCert = false;
    private int timeoutInSeconds;

    /**
     * Construct an empty instance.
     */
    public BlackDuckInstance() {
    }

    /**
     * Holds the connection details for a single Black Duck instance.
     *
     * @param serverUrl the url for the Black Duck server, e.g. https://my-ip or https://my-ip:8443.
     * @param apiToken the API token for the connection, note the API will have the permissions related to this user.
     * @param trustHttpsCert boolean whether to trust the SSL certificate, not recommended for production.
     */
    public BlackDuckInstance(String serverUrl, String apiToken, boolean trustHttpsCert) {
        this(serverUrl, apiToken, trustHttpsCert, null);
    }

    /**
     * Holds the connection details for a single Black Duck instance.
     *
     * @param serverUrl the url for the Black Duck server, e.g. https://my-ip or https://my-ip:8443.
     * @param apiToken the API token for the connection, note the API will have the permissions related to this user.
     * @param trustHttpsCert boolean whether to trust the SSL certificate, not recommended for production.
     * @param timeoutInSeconds the timeout in seconds, defaults to 20000.
     */
    public BlackDuckInstance(String serverUrl, String apiToken, boolean trustHttpsCert, Integer timeoutInSeconds) {
        this.serverUrl = serverUrl;
        this.apiToken = apiToken;
        this.trustHttpsCert = trustHttpsCert;
        this.timeoutInSeconds = (timeoutInSeconds != null) ? timeoutInSeconds : 20000;
    }

    /**
     * Returns the server url.
     * @return String server url.
     */
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Returns the API token.  Note this is sensitive...do not log.
     * @return String the api token.
     */
    public String getApiToken() {
        return apiToken;
    }

    /**
     * Sets the API token, note the API will have the permissions related to this user.
     * @param apiToken String api token.
     */
    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    /**
     * Sets the server url for the Black Duck server, e.g. https://my-ip or https://my-ip:8443.
     * @param serverUrl String the server url.
     */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * Returns whether to trust the SSL certificate, not recommended for production.
     * @return boolean whether to trust.
     */
    public boolean isTrustHttpsCert() {
        return trustHttpsCert;
    }

    /**
     * Sets whether to trust the SSL certificate, not recommended for production.
     * @param trustHttpsCert boolean whether to trust.
     */
    public void setTrustHttpsCert(boolean trustHttpsCert) {
        this.trustHttpsCert = trustHttpsCert;
    }

    /**
     * Returns the timeout in seconds, defaults to 20000.
     * @return int the timeout in seconds.
     */
    public int getTimeoutInSeconds() {
        return timeoutInSeconds;
    }

    /**
     * Sets the timeout in seconds.
     * @param timeoutInSeconds int timeout in seconds.
     */
    public void setTimeoutInSeconds(int timeoutInSeconds) {
        this.timeoutInSeconds = timeoutInSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlackDuckInstance blackDuckInstance1 = (BlackDuckInstance) o;
        return trustHttpsCert == blackDuckInstance1.trustHttpsCert &&
                timeoutInSeconds == blackDuckInstance1.timeoutInSeconds &&
                Objects.equals(apiToken, blackDuckInstance1.apiToken) &&
                Objects.equals(serverUrl, blackDuckInstance1.serverUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiToken, serverUrl, trustHttpsCert, timeoutInSeconds);
    }

    @Override
    public String toString() {
        return "BlackDuckInstance{" +
                "apiToken='" + apiToken + '\'' +
                ", server='" + serverUrl + '\'' +
                ", trustHttpsCert=" + trustHttpsCert +
                ", timeoutInSeconds=" + timeoutInSeconds +
                '}';
    }
}
