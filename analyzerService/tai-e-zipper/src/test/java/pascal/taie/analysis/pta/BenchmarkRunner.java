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

package pascal.taie.analysis.pta;

import picocli.CommandLine;
import pascal.taie.Main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@CommandLine.Command
public class BenchmarkRunner {

    private static final String BENCHMARK_HOME = "java-benchmarks";
    private static final String BENCHMARK_INFO = "java-benchmarks/benchmark-info.yml";
    private static final Map<String, BenchmarkInfo> benchmarkInfos = BenchmarkInfo.load(BENCHMARK_INFO);

    @CommandLine.Option(names = "-cs", defaultValue = "2-type")
    private String cs;

    @CommandLine.Option(names = "-java", defaultValue = "8")
    private int jdk;

    @CommandLine.Option(names = "-advanced", defaultValue = "zipper")
    private String advanced;

    public static void main(String[] args) {
        BenchmarkRunner runner = CommandLine.populateCommand(new BenchmarkRunner(), args);
        runner.runAll();
    }

    private void runAll() {
        if (benchmarkInfos.isEmpty()) {
            throw new IllegalArgumentException("No benchmarks are provided in the YAML file.");
        }
        benchmarkInfos.values().forEach(this::run);
    }

    private void run(BenchmarkInfo info) {
        System.out.println("\nAnalyzing " + info.id());
        if (Objects.equals(info.id(), "fop") || Objects.equals(info.id(), "jython") ||
        Objects.equals(info.id(), "briss-0.9")){
            return;
        }
        Main.main(composeArgs(info));
    }

    private String[] composeArgs(BenchmarkInfo info) {
        List<String> args = new ArrayList<>();
        int jdkVersion = jdk != 0 ? jdk : info.jdk();
        Collections.addAll(args,
                "-java", Integer.toString(jdkVersion),
                "-cp", buildClassPath(info.apps()),
                "-cp", buildClassPath(info.libs()),
                "-m", info.main());
        Map<String, String> ptaArgs = Map.of(
                "cs", cs,
                "only-app", "true",
                "distinguish-string-constants", "app",
                "dump", "true",
                "advanced", advanced,
                "reflection-log", new File(BENCHMARK_HOME, info.reflectionLog()).toString());
        Collections.addAll(args,
                "-a", "pta=" + ptaArgs.entrySet()
                        .stream()
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .collect(Collectors.joining(";")));
        if (info.allowPhantom()) {
            args.add("--allow-phantom");
        }
        return args.toArray(new String[0]);
    }

    private String buildClassPath(List<String> paths) {
        return paths.stream()
                .map(this::extendCP)
                .flatMap(List::stream)
                .collect(Collectors.joining(File.pathSeparator));
    }

    private List<String> extendCP(String path) {
        File file = new File(BENCHMARK_HOME, path);
        List<String> paths = new ArrayList<>();
        if (isJar(file)) {
            paths.add(file.toString());
        } else if (file.isDirectory()) {
            paths.add(file.toString());
            for (File item : Objects.requireNonNull(file.listFiles())) {
                if (isJar(item)) {
                    paths.add(item.toString());
                }
            }
        } else {
            throw new RuntimeException(path + " is neither a directory nor a JAR");
        }
        return paths;
    }

    private static boolean isJar(File file) {
        return file.getName().endsWith(".jar");
    }
}
