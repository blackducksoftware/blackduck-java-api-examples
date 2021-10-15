# blackduck-java-api-examples

Showcase of example scripts giving usage examples of how to call and utilise the blackduck-common Java API.

# Example library
The examples are written as self contained command line callable examples so you can quickly test them from a terminal but with the logic for command line processing and calling Black Duck separated out for clarity.

This library deliberately uses as few dependencies as possible so you can copy and paste the code as a starting point for your integration.

Building the library via 'mvn clean package' will produce a self contained jar file so you can call the examples directly from the terminal.

# Understanding the code

The connection parameters for a Black Duck instance are stored in the BlackDuckInstance model and passed to the BlackDuckRestConnector class, which highlights how to connect to Black Duck and obtain the services.

For a full list of examples of how to use the services browse the src/main/java/com/synopsys/blackduck/examples package.  Each command has an example command line usage in the class javadoc.

# Command Line Usage

For example FindProjectsByName will list all projects that match the name given and can be called via the following command:

`
java -cp <dependencies-jar> com.synopsys.blackduck.examples.<ExampleClass> -apikey <API-TOKEN> -url <BLACK-DUCK-URL -trusthttps -projectname "<PROJECT-NAME>"
`

e.g.

`
java -cp target\blackduck-java-api-examples-2021.2.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.FindProjectsByName -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmEyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps -projectname "Test Project"
`

All command line examples give usage information on parameters e.g.

java -cp target\blackduck-java-api-examples-2021.2.0-jar-with-dependencies.jar com.synopsys.blackduck.examples.GetProjectWithVersionsAndTagsByName -apikey ZGY4MWU1ZjktMzk0ZC00OTRkLTk2ODYtYjFkMWU1OTk0Y2EzOmDyNzU5MDFjLWQxMjktNDRlZC1iNTFjLWY5M2VhZjU5NzMxYg== -url https://52.213.63.19 -trusthttps

2020-06-16 13:48:48 ERROR BaseBlackDuckAPICommand:108 - Failed to parse command line options due to : Missing required option: projectname

usage: apikey

 -apikey <apikey>             The API key to connect to Black Duck with.
                              Note the API will have access according to
                              the user that generated the API key.

 -projectname <projectname>   The project name (case sensitive)

 -timeout <timeout>           The timeout in seconds.  Defaults to 20000
                              if not supplied.

 -trusthttps                  Whether to trust the SSL certificate,
                              defaults to false.  Not recommended for
                              production.

 -url <url>                   The Black Duck server url.

# List of Examples
- com.synopsys.blackduck.examples.DisableCopyrightsForProjectVersion - Load the BoM for a project version, for each origin of each component load and deactivate the copyrights.
- com.synopsys.blackduck.examples.FindComponentsByName - Find a component by name that exists within the Black Duck instance.
- com.synopsys.blackduck.examples.FindKBComponentsByName - Find a component by name from the Black Duck KnowledgeBase.
- com.synopsys.blackduck.examples.FindKBComponentsBySuiteID - Find a component within the Black Duck KnowledgeBase given it's Protex or Code Center Component ID.
- com.synopsys.blackduck.examples.FindProjectsByName - Find projects by name.
- com.synopsys.blackduck.examples.FindProjectVersionsModifiedInPeriod - Find project versions modified in last X hours by loading all projects, versions and the journal for each to determine changes made.
- com.synopsys.blackduck.examples.FindProjectVersionsScannedInPeriod - Find project versions scanned in last X hours using the notifications API.
- com.synopsys.blackduck.examples.GenericGetMultipleItemsByHref - Load a list of objects given the Black Duck URL.
- com.synopsys.blackduck.examples.GenericGetSingleItemByHref - Load the JSON for a Black Duck object given the URL.
- com.synopsys.blackduck.examples.GetProjectWithVersionsAndTagsByName - Load a project along with versions, tags and custom field values given the project name.
- com.synopsys.blackduck.examples.GetUserAndAssignmentsByUsername - Load a user by username along with their roles and user group membership.
- com.synopsys.blackduck.examples.ListBillOfMaterialsForProjectVersion - List the components in the Bill of Materials for a project version.
- com.synopsys.blackduck.examples.ListProjects - List all projects.
- com.synopsys.blackduck.examples.ListUsers - List all users.
- com.synopsys.blackduck.examples.ValidateBlackDuckConnection - Validate the connection to Black Duck.