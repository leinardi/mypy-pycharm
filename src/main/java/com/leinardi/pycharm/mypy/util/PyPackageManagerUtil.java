/*
 * Copyright 2021 Roberto Leinardi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leinardi.pycharm.mypy.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.webcore.packaging.PackageManagementService;
import com.intellij.webcore.packaging.RepoPackage;
import com.jetbrains.python.packaging.PyPackageManagers;

@SuppressWarnings("SameParameterValue")
public class PyPackageManagerUtil {
    private PyPackageManagerUtil() {
    }

    static void install(Project project, String packageName, PackageManagementService.Listener listener) {
        final PyPackageManagers packageManagers = PyPackageManagers.getInstance();
        PackageManagementService managementService = packageManagers.getManagementService(project,
                ProjectRootManager.getInstance(project).getProjectSdk());
        managementService.installPackage(
                new RepoPackage(packageName, null),
                null,
                false,
                null,
                listener,
                false);
    }
}
