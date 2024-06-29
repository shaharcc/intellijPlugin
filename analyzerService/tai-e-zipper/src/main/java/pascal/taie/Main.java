/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.AnalysisManager;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.AnalysisPlanner;
import pascal.taie.config.ConfigManager;
import pascal.taie.config.Configs;
import pascal.taie.config.LoggerConfigs;
import pascal.taie.config.Options;
import pascal.taie.config.Plan;
import pascal.taie.config.PlanConfig;
import pascal.taie.config.Scope;
import pascal.taie.frontend.cache.CachedWorldBuilder;
import pascal.taie.util.Timer;
import pascal.taie.util.collection.Lists;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String... args) {
        Timer.runAndCount(() -> {
            Options options = processArgs(args);
            LoggerConfigs.setOutput(options.getOutputDir());
            Plan plan = processConfigs(options);
            if (plan.analyses().isEmpty()) {
                logger.info("No analyses are specified");
                System.exit(0);
            }
            buildWorld(options, plan.analyses());
            executePlan(plan);
            LoggerConfigs.reconfigure();
            createGraphInput();
        }, "Tai-e");
    }

    /**
     * If the given options is empty or specify to print help information,
     * then print help and exit immediately.
     */
    private static Options processArgs(String... args) {
        Options options = Options.parse(args);
        if (options.isPrintHelp() || args.length == 0) {
            options.printHelp();
            System.exit(0);
        }
        return options;
    }

    private static Plan processConfigs(Options options) {
        InputStream content = Configs.getAnalysisConfig();
        List<AnalysisConfig> analysisConfigs = AnalysisConfig.parseConfigs(content);
        ConfigManager manager = new ConfigManager(analysisConfigs);
        AnalysisPlanner planner = new AnalysisPlanner(
                manager, options.getKeepResult());
        boolean reachableScope = options.getScope().equals(Scope.REACHABLE);
        if (!options.getAnalyses().isEmpty()) {
            // Analyses are specified by options
            List<PlanConfig> planConfigs = PlanConfig.readConfigs(options);
            manager.overwriteOptions(planConfigs);
            Plan plan = planner.expandPlan(
                    planConfigs, reachableScope);
            // Output analysis plan to file.
            // For outputting purpose, we first convert AnalysisConfigs
            // in the expanded plan to PlanConfigs
            planConfigs = Lists.map(plan.analyses(),
                    ac -> new PlanConfig(ac.getId(), ac.getOptions()));
            PlanConfig.writeConfigs(planConfigs, options.getOutputDir());
            if (!options.isOnlyGenPlan()) {
                // This run not only generates plan file but also executes it
                return plan;
            }
        } else if (options.getPlanFile() != null) {
            // Analyses are specified by file
            List<PlanConfig> planConfigs = PlanConfig.readConfigs(options.getPlanFile());
            manager.overwriteOptions(planConfigs);
            return planner.makePlan(planConfigs, reachableScope);
        }
        // No analyses are specified
        return Plan.emptyPlan();
    }

    /**
     * Convenient method for building the world from String arguments.
     */
    public static void buildWorld(String... args) {
        Options options = Options.parse(args);
        LoggerConfigs.setOutput(options.getOutputDir());
        Plan plan = processConfigs(options);
        buildWorld(options, plan.analyses());
        LoggerConfigs.reconfigure();
    }

    private static void buildWorld(Options options, List<AnalysisConfig> analyses) {
        Timer.runAndCount(() -> {
            try {
                Class<? extends WorldBuilder> builderClass = options.getWorldBuilderClass();
                Constructor<? extends WorldBuilder> builderCtor = builderClass.getConstructor();
                WorldBuilder builder = builderCtor.newInstance();
                if (options.isWorldCacheMode()) {
                    builder = new CachedWorldBuilder(builder);
                }
                builder.build(options, analyses);
                logger.info("{} classes with {} methods in the world",
                        World.get()
                                .getClassHierarchy()
                                .allClasses()
                                .count(),
                        World.get()
                                .getClassHierarchy()
                                .allClasses()
                                .mapToInt(c -> c.getDeclaredMethods().size())
                                .sum());
            } catch (InstantiationException | IllegalAccessException |
                    NoSuchMethodException | InvocationTargetException e) {
                System.err.println("Failed to build world due to " + e);
                System.exit(1);
            }
        }, "WorldBuilder");
    }

    private static void executePlan(Plan plan) {
        new AnalysisManager(plan).execute();
    }

    private static void createGraphInput() {
        private static final String ANALYZER_SERVICE_PATH = System.getenv("ANALYZER_SERVICE_PATH");
        Timer.runAndCount(() -> {
            try {
                String sourceFilePath = ANALYZER_SERVICE_PATH + "/tai-e-zipper/input/Main.java";
                Map<Integer, String> lineNumberToVariable = extractVariableNames(sourceFilePath);
                BufferedReader reader = new BufferedReader(new FileReader(ANALYZER_SERVICE_PATH + "/tai-e-zipper/pta-results.txt"));
                Map<String, String> variableToType = new HashMap<>();
                Map<String, Set<String>> typeToProperties = new HashMap<>();
                Map<String, String> variableToLineInfo = new HashMap<>();
                List<String[]> relationships = new ArrayList<>();
                String line;

                Pattern varPattern = Pattern.compile("\\[\\]:NewObj\\{<main\\.\\w+: void main\\(java\\.lang\\.String\\[\\]\\)>\\[(\\d+)@L(\\d+)] new main\\.(\\w+)}");
                Pattern relationshipPattern = Pattern.compile("\\[]:<main\\.(\\w+): void (\\w+)\\(main\\.(\\w+)\\)>/(%?\\w+) -> \\[\\[\\]:NewObj\\{<main\\.\\w+: void main\\(java\\.lang\\.String\\[\\]\\)>\\[\\d+@L(\\d+)] new main\\.(\\w+)}");
                Pattern nextRelationshipPattern = Pattern.compile(", \\[]:NewObj\\{<main\\.\\w+: void main\\(java\\.lang\\.String\\[\\]\\)>\\[\\d+@L(\\d+)] new main\\.\\w+}");

                while ((line = reader.readLine()) != null) {
                    Matcher varMatcher = varPattern.matcher(line);
                    if (varMatcher.find()) {
                        int lineNum = Integer.parseInt(varMatcher.group(2));
                        String type = varMatcher.group(3);
                        String variableName = lineNumberToVariable.get(lineNum);
                        if (variableName != null) {
                            variableToType.put(variableName, type);
                            variableToLineInfo.put(variableName, varMatcher.group(2) + "@" + varMatcher.group(3));
                        }
                    }

                    Matcher relMatcher = relationshipPattern.matcher(line);
                    if (relMatcher.find()) {
                        String relationship = relMatcher.group(2);
                        int lineNum1 = Integer.parseInt(relMatcher.group(5)); // Line number in Main.java where object is created

                        String nextLine = reader.readLine();
                        Matcher relMatcher2 = relationshipPattern.matcher(nextLine);
                        if (relMatcher2.find()) {
                            int lineNum2 = Integer.parseInt(relMatcher2.group(5)); // Line number in Main.java where method is called

                            String varName1 = lineNumberToVariable.get(lineNum1);
                            String varName2 = lineNumberToVariable.get(lineNum2);
                            relationships.add(new String[]{ varName1, varName2, relationship });

                            Matcher nextRelMatcher = nextRelationshipPattern.matcher(nextLine);
                            if (nextRelMatcher.find()) {
                                int additionalLineNum2 = Integer.parseInt(nextRelMatcher.group(1)); // Line number in Main.java where method is called
                                varName2 = lineNumberToVariable.get(additionalLineNum2);
                                relationships.add(new String[]{ varName1, varName2, relationship });
                            }
                        }
                    }

                    if (line.contains("Points-to sets of all instance fields")) {
                        while ((line = reader.readLine()) != null) {
                            if (line.toLowerCase().contains("thread")) {
                                continue;
                            }

                            Pattern propPattern = Pattern.compile("\\[\\]:NewObj\\{<main\\.\\w+: void .* new main\\.(\\w+)}\\.(\\w+) ->");
                            Matcher propMatcher = propPattern.matcher(line);
                            while (propMatcher.find()) {
                                String type = propMatcher.group(1);
                                String property = propMatcher.group(2);
                                typeToProperties.computeIfAbsent(type, k -> new HashSet<>()).add(property);
                            }
                        }
                    }
                }
                reader.close();

                String outputPath = ANALYZER_SERVICE_PATH + "/graphInput.txt";
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

                for (Map.Entry<String, String> entry : variableToType.entrySet()) {
                    String variableName = entry.getKey();
                    String typeName = entry.getValue();
                    Set<String> properties = typeToProperties.getOrDefault(typeName, new HashSet<>());
                    String lineInfo = variableToLineInfo.get(variableName);
                    if (lineInfo.contains("@")) {
                        String node1 = String.format("(nodeLabel = \"%s\", nodeType = \"var\", properties = [])", variableName);
                        String node2 = String.format("(nodeLabel = \"%s\", nodeType = \"type\", properties = %s)", typeName, properties);
                        String pair = String.format("(%s, %s)\n", node1, node2);
                        writer.write(pair);
                    }
                }

                for (String[] relationship : relationships) {
                    String varName1 = relationship[0];
                    String varName2 = relationship[1];
                    String edgeText = relationship[2];
                    String node1 = String.format("nodeLabel = \"%s\", nodeType = \"var\", properties = []", varName1);
                    String node2 = String.format("nodeLabel = \"%s\", nodeType = \"var\", properties = []", varName2);
                    String relEntry = String.format("((%s), (%s), edgeText = \"%s\")\n", node1, node2, edgeText);
                    writer.write(relEntry);
                }

                writer.close();
                printFileContent(outputPath); // TODO: delete this. only for debug
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "createGraphInput");
    }

    private static void printFileContent(String filePath) {  // TODO: delete this. only for debug
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            System.out.println("File content:");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<Integer, String> extractVariableNames(String sourceFilePath) {
        Map<Integer, String> lineNumberToVariable = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(sourceFilePath));
            Pattern pattern = Pattern.compile("\\s*(\\w+)\\s+(\\w+)\\s*=\\s*new\\s*([^\\s]+)\\(");

            for (int i = 0; i < lines.size(); i++) {
                Matcher matcher = pattern.matcher(lines.get(i));
                if (matcher.find()) {
                    String varName = matcher.group(2);
                    lineNumberToVariable.put(i + 1, varName);  // Line numbers are 1-based
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lineNumberToVariable;
    }
}
