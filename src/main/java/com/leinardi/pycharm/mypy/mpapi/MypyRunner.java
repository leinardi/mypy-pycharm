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
import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.jetbrains.python.packaging.PyPackage;
import com.jetbrains.python.packaging.PyPackageManager;
import com.jetbrains.python.sdk.PySdkUtil;
import com.jetbrains.python.sdk.PythonEnvUtil;
import com.leinardi.pycharm.mypy.MypyConfigService;
import com.leinardi.pycharm.mypy.exception.MypyPluginException;
import com.leinardi.pycharm.mypy.exception.MypyPluginParseException;
import com.leinardi.pycharm.mypy.exception.MypyToolException;
import com.leinardi.pycharm.mypy.util.FileTypes;
import com.leinardi.pycharm.mypy.util.Notifications;
import org.jdesktop.swingx.util.OS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MypyRunner {
    public static final String MYPY_PACKAGE_NAME = "mypy";
    private static final String MYPY_EXECUTABLE_NAME = MYPY_PACKAGE_NAME + (OS.isWindows() ? ".exe" : "");
    private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(MypyRunner.class);
    private static final String ENV_KEY_VIRTUAL_ENV = "VIRTUAL_ENV";
    private static final String ENV_KEY_PATH = "PATH";
    private static final String ENV_KEY_PYTHONHOME = "PYTHONHOME";
    private static final String TYPE_RE = " (error|warning|note):";
    private static final String ISSUE_RE = "([^\\s:]+):(\\d+:)?(\\d+:)?" + TYPE_RE + ".*";
    private static final String WHICH_EXECUTABLE_NAME = OS.isWindows() ? "where" : "which";
    private static final String ACTIVATE_FILE_NAME = OS.isWindows() ? "activate.bat" : "activate";

    private MypyRunner() {
    }

    public static boolean isMypyPathValid(String mypyPath, Project project) {
        File mypyFile = new File(mypyPath);
        if (!mypyFile.isAbsolute() && !mypyFile.getAbsolutePath().equalsIgnoreCase(mypyPath)) {
            mypyPath = project.getBasePath() + File.separator + mypyPath;
        }
        VirtualFile mypyVirtualFile = LocalFileSystem.getInstance().findFileByPath(mypyPath);
        if (mypyVirtualFile == null || !mypyVirtualFile.exists() || mypyVirtualFile.isDirectory()) {
            LOG.warn("Error while checking Mypy path " + mypyPath + ": null or not exists or not a file path");
            return false;
        }
        GeneralCommandLine cmd = getMypyCommandLine(project, mypyPath);
        boolean daemon = false;
        if (daemon) {
            cmd.addParameter("status");
        } else {
            cmd.addParameter("-V");
        }
        final Process process;
        try {
            process = cmd.createProcess();
            process.waitFor();
            String error = new BufferedReader(new InputStreamReader(process.getErrorStream(), UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            if (!StringUtil.isEmpty(error)) {
                LOG.info("Command Line string: " + cmd.getCommandLineString());
                LOG.warn("Error while checking Mypy path: " + error);
            }
            String output = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            if (!StringUtil.isEmpty(output)) {
                LOG.debug("Mypy path check output: " + output);
            }
            if (process.exitValue() != 0) {
                LOG.info("Command Line string: " + cmd.getCommandLineString());
                LOG.warn("Mypy path check process.exitValue: " + process.exitValue());
                return false;
            } else {
                return true;
            }
        } catch (ExecutionException | InterruptedException e) {
            LOG.info("Command Line string: " + cmd.getCommandLineString());
            LOG.warn("Error while checking Mypy path", e);
            return false;
        }
    }

    public static String getMypyPath(Project project) {
        return getMypyPath(project, true);
    }

    public static String getMypyPath(Project project, boolean checkConfigService) {
        MypyConfigService mypyConfigService = MypyConfigService.getInstance(project);
        if (checkConfigService) {
            if (mypyConfigService == null) {
                throw new IllegalStateException("MypyConfigService is null");
            }
            String mypyPath = mypyConfigService.getCustomMypyPath();
            if (!mypyPath.isEmpty()) {
                return mypyPath;
            }
        }
        VirtualFile interpreterFile = getInterpreterFile(project);
        if (isVenv(interpreterFile)) {
            VirtualFile mypyFile = LocalFileSystem.getInstance()
                    .findFileByPath(interpreterFile.getParent().getPath() + File.separator + MYPY_EXECUTABLE_NAME);
            if (mypyFile != null && mypyFile.exists()) {
                return mypyFile.getPath();
            }
        } else {
            return detectSystemMypyPath();
        }
        return "";
    }

    public static boolean checkMypyAvailable(Project project) {
        return checkMypyAvailable(project, false);
    }

    public static boolean checkMypyAvailable(Project project, boolean showNotifications) {
        Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (projectSdk == null
                || projectSdk.getHomeDirectory() == null
                || !projectSdk.getHomeDirectory().exists()) {
            if (showNotifications) {
                Notifications.showNoPythonInterpreter(project);
            }
            return false;
        } else if (showNotifications) {
            PyPackageManager pyPackageManager = PyPackageManager.getInstance(projectSdk);
            List<PyPackage> packages = pyPackageManager.getPackages();
            if (packages != null) {
                if (packages.stream().noneMatch(it -> MYPY_PACKAGE_NAME.equals(it.getName()))) {
                    Notifications.showInstallMypy(project);
                    return false;
                }
            }
        }
        MypyConfigService mypyConfigService = MypyConfigService.getInstance(project);
        if (mypyConfigService == null) {
            throw new IllegalStateException("MypyConfigService is null");
        }
        String mypyPath = getMypyPath(project);
        boolean isMypyPathValid = !mypyPath.isEmpty() && isMypyPathValid(mypyPath, project);
        if (showNotifications && !isMypyPathValid) {
            Notifications.showUnableToRunMypy(project);
        }
        return isMypyPathValid;
    }

    private static String getMypyConfigFile(Project project, String mypyConfigFilePath) throws MypyPluginException {
        String absolutePath = new File(mypyConfigFilePath).getAbsolutePath();
        if (mypyConfigFilePath.isEmpty()) {
            return "";
        } else if (!absolutePath.equals(mypyConfigFilePath)) {
            mypyConfigFilePath = project.getBasePath() + File.separator + mypyConfigFilePath;
        }
        VirtualFile mypyConfigFileFile = LocalFileSystem.getInstance().findFileByPath(mypyConfigFilePath);
        if (mypyConfigFileFile == null || !mypyConfigFileFile.exists()) {
            throw new MypyPluginException("mypy config file is not valid. File does not exist or can't be read.");
        }
        return mypyConfigFilePath;
    }

    public static String detectSystemMypyPath() {
        GeneralCommandLine cmd = new GeneralCommandLine(WHICH_EXECUTABLE_NAME);
        cmd.addParameter(MYPY_PACKAGE_NAME);
        final Process process;
        try {
            process = cmd.createProcess();
            Optional<String> path = new BufferedReader(
                    new InputStreamReader(cmd.createProcess().getInputStream(), UTF_8))
                    .lines()
                    .findFirst();
            process.waitFor();
            String error = new BufferedReader(new InputStreamReader(process.getErrorStream(), UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            if (!StringUtil.isEmpty(error)) {
                LOG.info("Command Line string: " + cmd.getCommandLineString());
                LOG.warn("Error while detecting Mypy path: " + error);
            }
            if (process.exitValue() != 0 || !path.isPresent()) {
                LOG.info("Command Line string: " + cmd.getCommandLineString());
                LOG.warn("Mypy path detect process.exitValue: " + process.exitValue());
                return "";
            }
            LOG.info("Detected Mypy path: " + path.get());
            return path.get();
        } catch (ExecutionException | InterruptedException e) {
            return "";
        }
    }

    public static List<Issue> scan(Project project, Set<String> filesToScan)
            throws InterruptedIOException, InterruptedException {
        if (!checkMypyAvailable(project, true)) {
            return Collections.emptyList();
        }
        MypyConfigService mypyConfigService = MypyConfigService.getInstance(project);
        if (filesToScan.isEmpty()) {
            throw new MypyPluginException("Illegal state: filesToScan is empty");
        }
        if (mypyConfigService == null) {
            throw new MypyPluginException("Illegal state: mypyConfigService is null");
        }

        String mypyPath = getMypyPath(project);
        if (mypyPath.isEmpty()) {
            throw new MypyToolException("Path to Mypy executable not set (check Plugin Settings)");
        }

        String mypyConfigFilePath = getMypyConfigFile(project, mypyConfigService.getMypyConfigFilePath());

        // Necessary because of this: https://github.com/python/mypy/issues/4008#issuecomment-417862464
        List<Issue> result = new ArrayList<>();
        Set<String> filesToScanFiltered = new HashSet<>();
        for (String filePath : filesToScan) {
            if (filePath.endsWith("__init__.py")
                    || filePath.endsWith("__main__.py")
                    || filePath.endsWith("setup.py")
            ) {
                result.addAll(runMypy(project, Collections.singleton(filePath), mypyPath, mypyConfigFilePath,
                        mypyConfigService));
            } else {
                filesToScanFiltered.add(filePath);
            }
        }
        result.addAll(runMypy(project, filesToScanFiltered, mypyPath, mypyConfigFilePath, mypyConfigService));
        return result;
    }

    private static List<Issue> runMypy(Project project, Set<String> filesToScan, String mypyPath,
                                       String mypyConfigFilePath, MypyConfigService mypyConfigService)
            throws InterruptedIOException, InterruptedException {
        if (filesToScan.isEmpty()) {
            return Collections.emptyList();
        }
        boolean daemon = false;

        GeneralCommandLine cmd = new GeneralCommandLine(mypyPath);
        cmd.setCharset(Charset.forName("UTF-8"));
        if (daemon) {
            cmd.addParameter("run");
            cmd.addParameter("--");
            cmd.addParameter("``--show-column-numbers");
        } else {
            cmd.addParameter("--show-column-numbers");
        }
        cmd.addParameter("--follow-imports");
        cmd.addParameter("silent");

        injectEnvironmentVariables(project, cmd);

        if (!mypyConfigFilePath.isEmpty()) {
            cmd.addParameter("--config-file");
            cmd.addParameter(mypyConfigFilePath);
        }

        ParametersList parametersList = cmd.getParametersList();
        parametersList.addParametersString(mypyConfigService.getMypyArguments());

        for (String file : filesToScan) {
            cmd.addParameter(file);
        }
        cmd.setWorkDirectory(project.getBasePath());
        final Process process;
        try {
            LOG.info("Running command: " + cmd.getCommandLineString());
            process = cmd.createProcess();
            InputStream inputStream = process.getInputStream();
            assert (inputStream != null);
            //TODO check stderr for errors
            //            process.waitFor();
            return parseMypyOutput(inputStream);
        } catch (InterruptedIOException e) {
            LOG.info("Command Line string: " + cmd.getCommandLineString());
            throw e;
        } catch (IOException e) {
            LOG.info("Command Line string: " + cmd.getCommandLineString());
            throw new MypyPluginParseException(e.getMessage(), e);
        } catch (ExecutionException e) {
            LOG.info("Command Line string: " + cmd.getCommandLineString());
            throw new MypyToolException("Error creating Mypy process", e);
        }
    }

    @NotNull
    public static List<Issue> parseMypyOutput(@NotNull InputStream inputStream) throws IOException {
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
                    String[] splitError = rawLine.substring(typeIndexStart).split(":", 2);
                    SeverityLevel severityLevel = SeverityLevel.valueOf(splitError[0].trim().toUpperCase());
                    String message = splitError[1].trim();
                    issues.add(new Issue(path, line, column, severityLevel, message));
                }
            }
        }
        return issues;
    }

    private static GeneralCommandLine getMypyCommandLine(Project project, String mypyPath) {
        GeneralCommandLine cmd;
        VirtualFile interpreterFile = getInterpreterFile(project);
        if (interpreterFile == null || FileTypes.isWindowsExecutable(mypyPath)) {
            cmd = new GeneralCommandLine(mypyPath);
        } else {
            cmd = new GeneralCommandLine(interpreterFile.getPath());
            cmd.addParameter(mypyPath);
        }
        return cmd;
    }

    @Nullable
    private static VirtualFile getInterpreterFile(Project project) {
        Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (projectSdk != null) {
            return projectSdk.getHomeDirectory();
        }
        return null;
    }

    private static void injectEnvironmentVariables(Project project, GeneralCommandLine cmd) {
        VirtualFile interpreterFile = getInterpreterFile(project);
        Map<String, String> extraEnv = null;
        Map<String, String> systemEnv = System.getenv();
        Map<String, String> expandedCmdEnv = PySdkUtil.mergeEnvVariables(systemEnv, cmd.getEnvironment());
        if (isVenv(interpreterFile)) {
            String venvPath = PathUtil.getParentPath(PathUtil.getParentPath(interpreterFile.getPath()));
            extraEnv = new HashMap<>();
            extraEnv.put(ENV_KEY_VIRTUAL_ENV, venvPath);
            if (expandedCmdEnv.containsKey(ENV_KEY_PATH)) {
                PythonEnvUtil.addPathToEnv(expandedCmdEnv, ENV_KEY_PATH, venvPath);
            }
            expandedCmdEnv.remove(ENV_KEY_PYTHONHOME);
        }
        Map<String, String> env = extraEnv != null ? PySdkUtil.mergeEnvVariables(expandedCmdEnv, extraEnv) :
                expandedCmdEnv;
        cmd.withEnvironment(env);
    }

    private static boolean isVenv(@Nullable VirtualFile interpreterFile) {
        return interpreterFile != null && interpreterFile.getParent().findChild(ACTIVATE_FILE_NAME) != null;
    }
}
