package com.synopsys.blackduck.examples;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.synopsys.blackduck.api.BlackDuckInstance;
import com.synopsys.blackduck.api.BlackDuckRestConnector;
import com.synopsys.integration.blackduck.service.dataservice.ReportBomService;
import com.synopsys.integration.blackduck.api.manual.view.ReportBomView;
import com.synopsys.integration.blackduck.api.manual.view.ReportBomContentView;
import com.synopsys.integration.blackduck.api.manual.component.ReportBomRequest;

import com.synopsys.integration.blackduck.service.dataservice.ProjectService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Downloads the Bill of Material report for a given project version.
 * 
 * Based on the example "ListBillOfMaterialsForProjectVersion.java" 
 * by David Nicholls - Synopsys Black Duck Technical Architect 
 *
 * Usage Example : java -cp target\blackduck-java-api-examples-2021.10.0-jar-with-dependencies.jar \
 * com.synopsys.blackduck.examples.GetBomReportForProjectVersion \
 * -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== \
 * -url https://52.213.63.19 \
 * -trusthttps \
 * -projectName com.synopsys.blackduck.examples \
 * -projectVersion 2022.9.0 \
 * -reportFormat JSON
 * -reportType SBOM
 * -sbomType SPDX_22
 * -outputDir .
 * 
 * This will write the report contents <outputDir>./<projectName>-<projectVersion>_<report type dependant filename as per BDH>
 * e.g. ./com.synopsys.blackduck.examples-2022.9.0_sbom_2023-09-09_002720.spdx
 *
 * @author Jens Nachtigall
 */
public class GetBomReportForProjectVersion extends ValidateBlackDuckConnection {

    private static final Logger log = LoggerFactory.getLogger(GetBomReportForProjectVersion.class);

    private static final String PROJECT_NAME_PARAMETER = "projectName";
    private static final String PROJECT_VERSION_PARAMETER = "projectVersion";
    private static final String SBOM_FORMAT_PARAMETER = "sbomFormat";
    private static final String SBOM_TYPE_PARAMETER = "sbomType";
    private static final String OUTPUT_PATH_PARAMETER = "outputDir";

    /**
     * Main Method
     * @param args command line parameters.
     */
    public static void main(String... args) {
        Options options = createCommandLineOptions();    

        addRequiredSingleOptionParameter(options, PROJECT_NAME_PARAMETER, "The projects name on blackduck.");
        addRequiredSingleOptionParameter(options, PROJECT_VERSION_PARAMETER, "The projects version on blackduck.");
        addRequiredSingleOptionParameter(options, SBOM_FORMAT_PARAMETER, "The format of the report to be created in: [JSON, RDF, TAGVALUE, YAML]");
        addRequiredSingleOptionParameter(options, SBOM_TYPE_PARAMETER, "The SBOM type of the report [SPDX_22, CYCLONEDX_13, CYCLONEDX_14]");
        addRequiredSingleOptionParameter(options, OUTPUT_PATH_PARAMETER, "The directory to write the SBOMs content to (existing files will be overwritten)");

        Optional<CommandLine> commandLine = parseCommandLine(options, args);
        if (commandLine.isPresent()) {

            String argProjectName = getRequiredSingleOptionParameterValue(commandLine.get(), PROJECT_NAME_PARAMETER);
            String argProjectVersion = getRequiredSingleOptionParameterValue(commandLine.get(), PROJECT_VERSION_PARAMETER);
            String argSbomType = getRequiredSingleOptionParameterValue(commandLine.get(), SBOM_TYPE_PARAMETER);
            String argSbomFormat = getRequiredSingleOptionParameterValue(commandLine.get(), SBOM_FORMAT_PARAMETER);
            String argOutputDir = getRequiredSingleOptionParameterValue(commandLine.get(), OUTPUT_PATH_PARAMETER);

            log.info("Project Name [" + argProjectName + "]");
            log.info("Project Version [" + argProjectVersion + "]");
            log.info("SBOM Type [" + argSbomType + "]");
            log.info("SBOM Format [" + argSbomFormat + "]");
            log.info("SBOM Output Directory [" + argOutputDir + "]");

            // Parse the Black Duck instance from the command line.
            BlackDuckInstance blackDuckInstance = getBlackDuckInstance(commandLine.get());

            // Create a Rest Connector with the specified connection details.
            BlackDuckRestConnector restConnector = new BlackDuckRestConnector(blackDuckInstance);
            GetBomReportForProjectVersion getSBOMForProjectVersion = new GetBomReportForProjectVersion();

            if (getSBOMForProjectVersion.validateBlackDuckConnection(restConnector)) {
                try {

                    // Setting up the BD project data service
                    ProjectService projectService = restConnector.getBlackDuckServicesFactory().createProjectService();
                    // Retrieve Project and Version view simulataneously
                    Optional<ProjectVersionWrapper> projectWrapper = projectService.getProjectVersion(argProjectName, argProjectVersion);

                    if (projectWrapper.isPresent()) {

                        // I was not able to find anything in the blackduck-common and blackduck-common-api to interface with.... the SBOM reports.
                        // https://github.com/search?q=org%3Ablackducksoftware+sbom-reports&type=code only returns buggy python implementations
                        // Note: Whatever is there for BillOfMaterials does not refer to reports but to the Hubs own list of dependencies for the webinterface.

                        ReportBomService sbomReportService = restConnector.getBlackDuckServicesFactory().createReportBomService();

                        // Schedule report creation for project and vesion
                        HttpUrl reportUrl = sbomReportService.createReport(
                            projectWrapper.get(), 
                            sbomReportService.createRequest(argSbomType, argSbomFormat)
                        );

                        // Await availabilit and successful download of report
                        ReportBomView sbomReport = sbomReportService.downloadReports(reportUrl, 30, log);

                        //Dump report contents to file (filename is supplied by blackduck)
                        for (ReportBomContentView entry : sbomReport.getReportContent()) {
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            String jsonString = gson.toJson(entry.getFileContent()); //make it pretty

                            Path outputDir = Paths.get(argOutputDir);

                            if (!Files.exists(outputDir)) {
                                Files.createDirectories(outputDir);
                            }

                            if (!Files.isDirectory(outputDir)) {
                                log.error("Error: The specified path is not a directory!");
                                System.exit (-1);
                            }

                            // "sbom_2023-09-06_213359.spdx"
                            Path fileName = Paths.get(
                                outputDir.toString(), 
                                argProjectName + "-" + argProjectVersion + "_" + entry.getFileNamePrefix()
                            );

                            log.info("Written SBOM to " + fileName);
                            Files.write(fileName, jsonString.getBytes());
                        }
                    } else {
                        log.error("The Project " + argProjectName + ":" + argProjectVersion + 
                            " was not found on " + restConnector.getServerUrl()); 
                        System.exit (-1);
                    }
                }
                catch (IntegrationException e) {
                    log.error(
                        "Failed to create Report for [" + argProjectName + ":" + argProjectVersion + "] " + 
                        "as BoM could not be loaded for the project version due to : " + e.getMessage(), e);
                    System.exit(-1);
                }
                catch (InterruptedException e) {
                    log.error("Singal Received: Interrupted");
                    System.exit(-1);
                }
                catch (Exception e) {
                    log.error("Other: " + e.getMessage());
                    System.exit(-1);
                }
            } else {
                // if (getSBOMForProjectVersion.validateBlackDuckConnection(restConnector))
                // prints Exception by itself
                System.exit(-1);
            }
        } else {
            // if (commandLine.isPresent())
            // prints Exception by itself
            System.exit(-1);
        }
    }
}
