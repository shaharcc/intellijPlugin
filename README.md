# Points-To Analysis Plugin for IntelliJ IDEA
## Overview
This project integrates a points-to analysis tool directly into IntelliJ IDEA, combining a Spring Boot service for analysis and an IntelliJ plugin for seamless integration.

### Components
1. **analyzerService**
   - **tai-e-zipper**: Core logic for points-to analysis.
   - **Service Implementation**: Spring Boot application (`AnalyzerService`) providing endpoints for analysis.

2. **PointsTo-0.0.1.zip**
   - IntelliJ IDEA plugin enabling advanced points-to analysis features.
  

## Installation and Usage
### Running analyzerService
To run the analyzerService locally:

1. Navigate to the analyzerService directory.
2. Build the project using Gradle:
```bash
./gradlew build
```
3. Start the Spring Boot application:
```bash
./gradlew bootRun
```

4. Access the service at http://localhost:8080.


### Installing PointsTo-0.0.1.zip Plugin
To install the IntelliJ IDEA plugin:

1. Open IntelliJ IDEA.
2. Go to File -> Settings -> Plugins.
3. Click on Install plugin from disk....
4. Select PointsTo-0.0.1.zip from your local file system.

### Running the Example Java Project
#### To demonstrate the PointsTo plugin:
- Launch IntelliJ IDEA.
- Open the example directory as a project.
- Modify the main method and classes to use the PointsTo plugin.
- Click **Run** to initiate visual analysis.
- Use **Refresh** to update analysis after changes.
- Click **Clear** to remove the graph from the workspace.

#### Watch the Demo
Watch a demo of the Points-To Analysis Plugin in action: [Watch Video](https://drive.google.com/file/d/1c1ZOMR-tR7XBI-TxKN7dAJn07cPKPT08/view?usp=sharing)
