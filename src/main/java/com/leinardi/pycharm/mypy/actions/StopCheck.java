/*
 * Copyright 2023 Roberto Leinardi.
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
 *
 * Modifications:
 * - 2024-10-03: Modified by Dominik Willner to fix ActionUpdateThread deprecation warnings.
 */

package com.leinardi.pycharm.mypy.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.wm.ToolWindow;
import com.leinardi.pycharm.mypy.MypyPlugin;
import org.jetbrains.annotations.NotNull;

import static com.leinardi.pycharm.mypy.actions.ToolWindowAccess.toolWindow;

/**
 * Action to stop a check in progress.
 */
public class StopCheck extends BaseAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final ToolWindow toolWindow = toolWindow(project);
                toolWindow.activate(() -> {
                    setProgressText(toolWindow, "plugin.status.in-progress.current");
                    final MypyPlugin mypyPlugin
                            = project.getService(MypyPlugin.class);
                    if (mypyPlugin == null) {
                        throw new IllegalStateException("Couldn't get mypy plugin");
                    }
                    mypyPlugin.stopChecks();

                    setProgressText(toolWindow, "plugin.status.aborted");
                });

            } catch (Throwable e) {
                MypyPlugin.processErrorAndLog("Abort Scan", e);
            }
        });
    }

    @Override
    public void update(final @NotNull AnActionEvent event) {
        final Presentation presentation = event.getPresentation();
        project(event).ifPresentOrElse(project -> {
            final MypyPlugin mypyPlugin = project.getService(MypyPlugin.class);
            if (mypyPlugin == null) {
                throw new IllegalStateException("Couldn't get mypy plugin");
            }
            try {
                presentation.setEnabled(mypyPlugin.isScanInProgress());
            } catch (Throwable e) {
                MypyPlugin.processErrorAndLog("Abort button update", e);
            }
        }, () -> presentation.setEnabled(false));
    }
}
