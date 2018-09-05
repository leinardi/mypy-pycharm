/*
 * Copyright 2018 Roberto Leinardi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leinardi.pycharm.mypy.mpapi;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.leinardi.pycharm.mypy.MypyConfigService;
import com.leinardi.pycharm.mypy.exception.MypyPluginException;
import com.leinardi.pycharm.mypy.exception.MypyPluginParseException;
import com.leinardi.pycharm.mypy.exception.MypyToolException;
import com.leinardi.pycharm.mypy.util.Notifications;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MypyRunner {
    private static final String TYPE_RE = " (error|warning|note):";
    private static final String ISSUE_RE = "([^\\s:]+):(\\d+:)?(\\d+:)?" + TYPE_RE + ".*";

    private MypyRunner() {
    }

    public static boolean isPathToMypyValid(String pathToMypy) {
        boolean daemon = false;
        if (pathToMypy.startsWith(File.separator)) {
            VirtualFile mypyFile = LocalFileSystem.getInstance().findFileByPath(pathToMypy);
            if (mypyFile == null || !mypyFile.exists()) {
                return false;
            }
        }
        GeneralCommandLine cmd = new GeneralCommandLine(pathToMypy);
        if (daemon) {
            cmd.addParameter("status");
        } else {
            cmd.addParameter("-V");
        }
        final Process process;
        try {
            process = cmd.createProcess();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (ExecutionException | InterruptedException e) {
            return false;
        }
    }

    public static boolean isMypyAvailable(Project project) {
        MypyConfigService mypyConfigService = MypyConfigService.getInstance(project);
        if (mypyConfigService == null) {
            throw new IllegalStateException("MypyConfigService is null");
        }
        return isPathToMypyValid(mypyConfigService.getPathToMypy());
    }

    public static List<Issue> scan(Project project, Set<String> filesToScan)
            throws InterruptedIOException, InterruptedException {
        if (!isMypyAvailable(project)) {
            Notifications.showMypyNotAvailable(project);
            return Collections.emptyList();
        }
        MypyConfigService mypyConfigService = MypyConfigService.getInstance(project);
        if (filesToScan.isEmpty()) {
            throw new MypyPluginException("Illegal state: filesToScan is empty");
        }
        if (mypyConfigService == null) {
            throw new MypyPluginException("Illegal state: mypyConfigService is null");
        }

        String pathToMypy = mypyConfigService.getPathToMypy();
        if (pathToMypy.isEmpty()) {
            throw new MypyToolException("Path to Mypy executable not set (check Plugin Settings)");
        }

        String[] args = mypyConfigService.getMypyArguments().split(" ", -1);

        // Necessary because of this: https://github.com/python/mypy/issues/4008#issuecomment-417862464
        List<Issue> result = new ArrayList<>();
        Set<String> filesToScanFiltered = new HashSet<>();
        for (String filePath : filesToScan) {
            if (filePath.endsWith("__init__.py")
                    || filePath.endsWith("__main__.py")
                    || filePath.endsWith("setup.py")
            ) {
                result.addAll(runMypy(project, Collections.singleton(filePath), pathToMypy, args));
            } else {
                filesToScanFiltered.add(filePath);
            }
        }
        result.addAll(runMypy(project, filesToScanFiltered, pathToMypy, args));
        return result;
    }

    private static List<Issue> runMypy(Project project, Set<String> filesToScan, String pathToMypy, String[] args)
            throws InterruptedIOException, InterruptedException {
        if (filesToScan.isEmpty()) {
            return Collections.emptyList();
        }
        boolean daemon = false;

        GeneralCommandLine cmd = new GeneralCommandLine(pathToMypy);
        cmd.setCharset(Charset.forName("UTF-8"));
        if (daemon) {
            cmd.addParameter("run");
            cmd.addParameter("--");
            cmd.addParameter("``--show-column-numbers");
        } else {
            cmd.addParameter("--show-column-numbers");
        }
        cmd.addParameter("--follow-imports");
        cmd.addParameter("skip");
        for (String arg : args) {
            if (!StringUtil.isEmpty(arg)) {
                cmd.addParameter(arg);
            }
        }
        for (String file : filesToScan) {
            cmd.addParameter(file);
        }
        cmd.setWorkDirectory(project.getBasePath());
        final Process process;
        try {
            process = cmd.createProcess();
            InputStream inputStream = process.getInputStream();
            //            process.waitFor();
            return parseMypyOutput(inputStream);
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            throw new MypyPluginParseException(e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new MypyToolException("Error creating Mypy process", e);
        }
    }

    private static List<Issue> parseMypyOutput(InputStream inputStream) throws IOException {
        ArrayList<Issue> issues = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, UTF_8));
        String rawLine;
        Pattern typePattern = Pattern.compile(TYPE_RE);
        while ((rawLine = bufferedReader.readLine()) != null) {
            if (rawLine.matches(ISSUE_RE)) {
                Matcher matcher = typePattern.matcher(rawLine);
                if (matcher.find()) {
                    int typeIndexStart = matcher.start();
                    String[] splitPosition = rawLine.substring(0, typeIndexStart - 1).split(":", -1);
                    String path = splitPosition[0].trim();
                    int line = splitPosition.length > 1 ? Integer.parseInt(splitPosition[1].trim()) : 1;
                    int column = splitPosition.length > 2 ? Integer.parseInt(splitPosition[2].trim()) : 1;
                    String[] splitError = rawLine.substring(typeIndexStart).split(":", -1);
                    SeverityLevel severityLevel = SeverityLevel.valueOf(splitError[0].trim().toUpperCase());
                    String message = splitError[1].trim();
                    issues.add(new Issue(path, line, column, severityLevel, message));
                }
            }
        }
        return issues;
    }

}
