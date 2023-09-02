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
 */

package com.leinardi.pycharm.mypy.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.leinardi.pycharm.mypy.toolwindow.MypyToolWindowPanel;

import java.util.function.Consumer;
import java.util.function.Function;

public final class ToolWindowAccess {
    private ToolWindowAccess() {
    }

    static void actOnToolWindowPanel(final ToolWindow toolWindow, final Consumer<MypyToolWindowPanel> action) {
        final Content content = toolWindow.getContentManager().getContent(0);
        // the content instance will be a JLabel while the component initialises
        if (content != null && content.getComponent() instanceof MypyToolWindowPanel) {
            action.accept((MypyToolWindowPanel) content.getComponent());
        }
    }

    static <R> R getFromToolWindowPanel(final ToolWindow toolWindow, final Function<MypyToolWindowPanel, R> action) {
        final Content content = toolWindow.getContentManager().getContent(0);
        // the content instance will be a JLabel while the component initialises
        if (content != null && content.getComponent() instanceof MypyToolWindowPanel) {
            return action.apply((MypyToolWindowPanel) content.getComponent());
        }
        return null;
    }

    static ToolWindow toolWindow(final Project project) {
        return ToolWindowManager
                .getInstance(project)
                .getToolWindow(MypyToolWindowPanel.ID_TOOLWINDOW);
    }
}
