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

package com.leinardi.pycharm.mypy.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBTextField;
import com.leinardi.pycharm.mypy.MypyBundle;
import com.leinardi.pycharm.mypy.MypyConfigService;
import com.leinardi.pycharm.mypy.mpapi.MypyRunner;
import com.leinardi.pycharm.mypy.util.Icons;
import com.leinardi.pycharm.mypy.util.Notifications;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;

public class MypyConfigPanel {
    private JPanel rootPanel;
    private JButton testButton;
    private JBTextField argumentsField;
    private TextFieldWithBrowseButton pathToMypyField;
    private Project project;

    public MypyConfigPanel(Project project) {
        this.project = project;
        MypyConfigService mypyConfigService = MypyConfigService.getInstance(project);
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(
                true, false, false, false, false, false);
        pathToMypyField.addBrowseFolderListener(
                "",
                MypyBundle.message("config.file.browse.tooltip"),
                null,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        testButton.setAction(new TestAction());
        pathToMypyField.setText(mypyConfigService.getPathToMypy());
        argumentsField.setText(mypyConfigService.getMypyArguments());
        argumentsField.getEmptyText().setText(MypyBundle.message("config.optional"));
    }

    public JPanel getPanel() {
        return rootPanel;
    }

    public String getPathToMypy() {
        return pathToMypyField.getText();
    }

    public String getMypyArguments() {
        return argumentsField.getText();
    }

    private final class TestAction extends AbstractAction {

        TestAction() {
            putValue(Action.NAME, MypyBundle.message(
                    "config.mypy.path.test"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            String pathToMypy = getPathToMypy();
            if (MypyRunner.isPathToMypyValid(pathToMypy)) {
                testButton.setIcon(Icons.icon("/general/inspectionsOK.png"));
                Notifications.showInfo(
                        project,
                        MypyBundle.message("config.mypy.path.success.message")
                );
            } else {
                testButton.setIcon(Icons.icon("/general/error.png"));
                Notifications.showError(
                        project,
                        MypyBundle.message("config.mypy.path.failure.message", pathToMypy)
                );
            }
        }
    }
}
