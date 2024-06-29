Points-To Analysis Plugin for IntelliJ IDEA
Overview
This project includes two main components:

analyzerService: A service built with Spring Boot for conducting points-to analysis using the TAI-E-Zipper framework.
PointsTo-0.0.1.zip: A plugin for IntelliJ IDEA that integrates our points-to analysis capabilities directly into the IDE.
Components
1. analyzerService
   The analyzerService directory contains:

tai-e-zipper: This directory houses the core logic for performing points-to analysis.
Service Implementation: Spring Boot application (MockAnalyzerService) providing endpoints to trigger points-to analysis on uploaded Java files.
2. PointsTo-0.0.1.zip
   This plugin enables IntelliJ IDEA users to:

example
The example directory contains a simple Java project that demonstrates how to use the PointsTo plugin. It includes three classes and a main method that utilizes them.

Install our points-to analysis tool directly within their IDE environment.
Utilize advanced points-to analysis features seamlessly integrated into their development workflow.
Installation and Usage
Running analyzerService
To run the analyzerService locally:

Navigate to the analyzerService directory.
Build the project using Gradle:
bash
Copy code
./gradlew build
Start the Spring Boot application:
bash
Copy code
./gradlew bootRun
The service will be accessible at http://localhost:8080.
Installing PointsTo-0.0.1.zip Plugin
To install the IntelliJ IDEA plugin:

Open IntelliJ IDEA.
Go to File -> Settings -> Plugins.
Click on Install plugin from disk....
Select PointsTo-0.0.1.zip from your local file system.
Restart IntelliJ IDEA to enable the plugin.

Running the Example Java Project
Open in IntelliJ IDEA:

Launch IntelliJ IDEA.
Open the example directory as a project.
Modify the main method and classes within the example Java project to incorporate the PointsTo plugin functionality.
Click the "Run" button to initiate the visual analysis.
Use the "Refresh" button to update the analysis after making any changes.
To remove the graph from the workspace, click the "Clear" button.