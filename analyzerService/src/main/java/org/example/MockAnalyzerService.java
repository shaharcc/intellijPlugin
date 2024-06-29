package org.example;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
public class MockAnalyzerService {
    private static final String TAI_E_DIR = getBaseDir() + "/tai-e-zipper";
    private static final String GRAPH_INPUT_FILE_PATH = getBaseDir() + File.separator + "graphInput.txt";

    @PostMapping("/analyze")
    public ResponseEntity<String> analyze(@RequestParam("files") MultipartFile[] files) {
        System.out.println("Received a request to /analyze");

        try {
            Files.createDirectories(Path.of(TAI_E_DIR + "/input"));
            List<Path> savedFiles = saveFiles(files);
            compileJavaFiles(savedFiles);
            String jarCommand = "java -jar build/tai-e-all-0.5.1-SNAPSHOT.jar -cp input -m main.Main -java 8 -a \"pta=cs:2-type;only-app:true;distinguish-string-constants:app;dump:true;advanced:zipper\"";
            executeCommand(jarCommand);

            // Read the content of graphInput.txt and return as response
            return readGraphInputFile();
        } catch (IOException | InterruptedException e) {
            System.out.println("Error processing files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing files");
        }
    }

    private List<Path> saveFiles(MultipartFile[] files) throws IOException {
        List<Path> savedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            Path filePath = Path.of(TAI_E_DIR + "/input", file.getOriginalFilename());
            System.out.println("Saving uploaded file to: " + filePath.toString());
            file.transferTo(filePath.toFile());
            savedFiles.add(filePath);
        }

        return savedFiles;
    }

    private void compileJavaFiles(List<Path> javaFiles) throws IOException, InterruptedException {
        // Compile all .java files using javac
        String compileCommand = "javac -g -d input input/*.java";
        executeCommand(compileCommand);
    }

    private void executeCommand(String command) throws IOException, InterruptedException {
        String[] commandArgs = command.split("\\s+");
        ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
        processBuilder.directory(new File(TAI_E_DIR));
        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + command + ", with exit code: " + exitCode);
        }
    }

    private ResponseEntity<String> readGraphInputFile() throws IOException {
        // Read the content of graphInput.txt
        File graphInputFile = new File(GRAPH_INPUT_FILE_PATH);
        if (graphInputFile.exists()) {
            System.out.println("Reading contents of: " + GRAPH_INPUT_FILE_PATH);
            String output = new String(Files.readAllBytes(graphInputFile.toPath()));
            System.out.println("Returning content of graphInput.txt");
            return ResponseEntity.ok(output);
        } else {
            System.out.println("Graph input file not found at: " + GRAPH_INPUT_FILE_PATH);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Graph input file not found");
        }
    }

    private static String getBaseDir() {
        return new File("").getAbsolutePath();
    }
}
