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

package com.leinardi.pycharm.mypy.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.leinardi.pycharm.mypy.MypyPlugin;
import com.leinardi.pycharm.mypy.toolwindow.MypyToolWindowPanel;
import com.leinardi.pycharm.mypy.util.VfUtil;

import java.util.List;

/**
 * Action to execute a Mypy scan on the current module.
 */
public class ScanModule extends BaseAction {

    @Override
    public final void actionPerformed(final AnActionEvent event) {
        try {
            final Project project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) {
                return;
            }

            final ToolWindow toolWindow = ToolWindowManager.getInstance(
                    project).getToolWindow(MypyToolWindowPanel.ID_TOOLWINDOW);

            final VirtualFile[] selectedFiles
                    = FileEditorManager.getInstance(project).getSelectedFiles();
            if (selectedFiles.length == 0) {
                setProgressText(toolWindow, "plugin.status.in-progress.no-file");
                return;
            }

            final MypyPlugin mypyPlugin
                    = project.getComponent(MypyPlugin.class);
            if (mypyPlugin == null) {
                throw new IllegalStateException("Couldn't get mypy plugin");
            }

            toolWindow.activate(() -> {
                try {
                    setProgressText(toolWindow, "plugin.status.in-progress.module");

                    List<VirtualFile> moduleFiles = VfUtil.filterOnlyPythonProjectFiles(
                            project, VfUtil.flattenFiles(new VirtualFile[]{selectedFiles[0].getParent()}));
                    Runnable scanAction = new ScanSourceRootsAction(project, moduleFiles.toArray(new VirtualFile[0]));
                    ApplicationManager.getApplication().runReadAction(scanAction);
                } catch (Throwable e) {
                    MypyPlugin.processErrorAndLog("Current Module scan", e);
                }
            });

        } catch (Throwable e) {
            MypyPlugin.processErrorAndLog("Current Module scan", e);
        }
    }

    @Override
    public final void update(final AnActionEvent event) {
        super.update(event);

        try {
            final Presentation presentation = event.getPresentation();

            final Project project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) { // check if we're loading...
                presentation.setEnabled(false);
                return;
            }

            final VirtualFile[] selectedFiles
                    = FileEditorManager.getInstance(project).getSelectedFiles();
            if (selectedFiles.length == 0) {
                return;
            }

            final Module module = ModuleUtil.findModuleForFile(
                    selectedFiles[0], project);
            if (module == null) {
                return;
            }

            final MypyPlugin mypyPlugin
                    = project.getComponent(MypyPlugin.class);
            if (mypyPlugin == null) {
                throw new IllegalStateException("Couldn't get mypy plugin");
            }

            VirtualFile[] moduleFiles;
            final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            moduleFiles = moduleRootManager.getContentRoots();

            // disable if no files are selected or scan in progress
            if (containsAtLeastOneFile(moduleFiles)) {
                presentation.setEnabled(!mypyPlugin.isScanInProgress());
            } else {
                presentation.setEnabled(false);
            }
        } catch (Throwable e) {
            MypyPlugin.processErrorAndLog("Current Module button update", e);
        }
    }
}
