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

package com.leinardi.pycharm.mypy.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.ui.content.Content;
import com.leinardi.pycharm.mypy.MypyBundle;
import org.jetbrains.annotations.NotNull;

public class MypyToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        final Content toolContent = toolWindow.getContentManager().getFactory().createContent(
                new MypyToolWindowPanel(toolWindow, project),
                MypyBundle.message("plugin.toolwindow.action"),
                false);
        toolWindow.getContentManager().addContent(toolContent);

        toolWindow.setTitle(MypyBundle.message("plugin.toolwindow.name"));
        toolWindow.setType(ToolWindowType.DOCKED, null);
    }

}
