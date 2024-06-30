# Points-To Analysis Plugin for IntelliJ IDEA

## Overview
This project integrates a points-to analysis tool directly into IntelliJ IDEA, combining a Spring Boot service for analysis and an IntelliJ plugin for seamless integration.

### Components
1. **analyzerService**
   - **tai-e-zipper**: Core logic for points-to analysis.
   - **Service Implementation**: Spring Boot application (`AnalyzerService`) providing endpoints for plugin.

2. **PointsTo-0.0.1.zip**
   - IntelliJ IDEA plugin enabling advanced points-to analysis features.

## Installation and Usage

### Project Setup Instructions

#### Download and Install Microsoft Build of OpenJDK

1. Download the Microsoft Build of OpenJDK:
    ```powershell
    Invoke-WebRequest -Uri https://aka.ms/download-jdk/microsoft-jdk-17.0.11-windows-x64.zip -OutFile "$env:USERPROFILE\Downloads\microsoft-jdk-17.0.11-windows-x64.zip"
    ```

2. Create a directory for OpenJDK:
    ```powershell
    mkdir C:\OpenJDK
    ```

3. Extract the downloaded ZIP file:
    ```powershell
    Expand-Archive -Path "$env:USERPROFILE\Downloads\microsoft-jdk-17.0.11-windows-x64.zip" -DestinationPath C:\OpenJDK
    ```

4. Set the JAVA_HOME environment variable:
    ```powershell
    $jdkPath = Join-Path -Path "C:\OpenJDK" -ChildPath (Get-ChildItem -Path "C:\OpenJDK" | Where-Object {$_.PSIsContainer} | Select-Object -First 1).Name
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $jdkPath, "User")
    ```

5. Add JDK bin directory to the Path environment variable:
    ```powershell
    [Environment]::SetEnvironmentVariable("Path", "$env:Path;$jdkPath\bin", "User")
    ```

6. Close PowerShell and reopen it. Verify the installation:
    ```powershell
    java -version
    ```

#### Download and Install Gradle 8.5

1. Download Gradle:
    ```powershell
    Invoke-WebRequest -Uri https://services.gradle.org/distributions/gradle-8.5-bin.zip -OutFile "$env:USERPROFILE\Downloads\gradle-8.5-bin.zip"
    ```

2. Create a directory for Gradle:
    ```powershell
    mkdir C:\Gradle
    ```

3. Extract the downloaded ZIP file:
    ```powershell
    Expand-Archive -Path "$env:USERPROFILE\Downloads\gradle-8.5-bin.zip" -DestinationPath C:\Gradle
    ```

4. Set the GRADLE_HOME environment variable:
    ```powershell
    $gradlePath = Join-Path -Path "C:\Gradle" -ChildPath (Get-ChildItem -Path "C:\Gradle" | Where-Object {$_.PSIsContainer} | Select-Object -First 1).Name
    [Environment]::SetEnvironmentVariable("GRADLE_HOME", $gradlePath, "User")
    ```

5. Add Gradle bin directory to the Path environment variable:
    ```powershell
    [Environment]::SetEnvironmentVariable("Path", "$env:Path;$gradlePath\bin", "User")
    ```

6. Close PowerShell and reopen it. Verify the installation:
    ```powershell
    gradle –version
    ```

#### Download and Install cURL

1. Clone the cURL repository:
    ```powershell
    git clone https://github.com/curl/curl-for-win.git "$env:USERPROFILE\Downloads\curl-for-win"
    ```

2. Add cURL bin directory to the Path environment variable:
    ```powershell
    $curlPath = "$env:USERPROFILE\Downloads\curl-for-win\bin"
    [Environment]::SetEnvironmentVariable("Path", "$env:Path;$curlPath", "User")
    ```

3. Validate the installation by running:
    ```powershell
    curl
    ```

#### Download and Install IntelliJ IDEA

1. Go to [IntelliJ IDEA – the Leading Java and Kotlin IDE](https://www.jetbrains.com/idea/download/) and download the IDE.

#### Download and Install Graphviz

1. Download the Graphviz installer:
   - Visit the [Graphviz download page](https://www.graphviz.org/download/) and download version: `graphviz-10.0.1`

2. During installation, add Graphviz to the Path when prompted.

3. **After installing, restart your computer!**

#### Clone and Install the IntelliJ Plugin Project

1. Clone the project repository:
    ```powershell
    git clone https://github.com/shaharcc/intellijPlugin.git
    ```

### Running analyzerService
To run the analyzerService locally:

1. Navigate to the analyzerService directory.
   ```powershell
    cd .\intellijPlugin\analyzerService\
    ```
2. Build the project using Gradle:
    ```bash
    ./gradlew build
    ```
3. Start the Spring Boot application:
    ```bash
    ./gradlew bootRun
    ```

4. Access the service at http://localhost:8080.

### Install the plugin:
- Open IntelliJ IDEA.
- Go to File -> Settings -> Plugins.
- Click on Install plugin from disk....
- Select `PointsTo-0.0.1.zip` from your local file system.
- Go to View -> Tool window and choose the pointsTo graph


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
