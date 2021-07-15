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
import com.intellij.ui.components.JBCheckBox;
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
    private com.intellij.openapi.ui.TextFieldWithBrowseButton mypyPathField;
    private com.intellij.openapi.ui.TextFieldWithBrowseButton mypyConfigFilePathField;
    private JBTextField argumentsField;
    private JBCheckBox useDaemonCheckBox;
    private Project project;

    public MypyConfigPanel(Project project) {
        this.project = project;
        MypyConfigService mypyConfigService = MypyConfigService.getInstance(project);
        if (mypyConfigService == null) {
            throw new IllegalStateException("MypyConfigService is null");
        }
        testButton.setAction(new TestAction());
        mypyPathField.setText(mypyConfigService.getCustomMypyPath());
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(
                true, false, false, false, false, false);
        mypyPathField.addBrowseFolderListener(
                "",
                MypyBundle.message("config.mypy.path.tooltip"),
                null,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        mypyConfigFilePathField.setText(mypyConfigService.getMypyConfigFilePath());
        mypyConfigFilePathField.addBrowseFolderListener(
                "",
                MypyBundle.message("config.mypy-config-file.path.tooltip"),
                null,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        argumentsField.setText(mypyConfigService.getMypyArguments());
        argumentsField.getEmptyText().setText(MypyBundle.message("config.optional"));
        useDaemonCheckBox.setSelected(mypyConfigService.isUseDaemon());
    }

    public JPanel getPanel() {
        return rootPanel;
    }

    public String getMypyPath() {
        return getMypyPath(false);
    }

    public String getMypyPath(boolean autodetect) {
        String path = mypyPathField.getText();
        if (path.isEmpty() && autodetect) {
            return MypyRunner.getMypyPath(project, false);
        }
        return path;
    }

    public String getMypyConfigFilePath() {
        return mypyConfigFilePathField.getText();
    }

    public String getMypyArguments() {
        return argumentsField.getText();
    }

    public boolean getUseDaemon() { return useDaemonCheckBox.isSelected(); }

    private void createUIComponents() {
        JBTextField autodetectTextField = new JBTextField();
        autodetectTextField.getEmptyText()
                .setText(MypyBundle.message("config.auto-detect", MypyRunner.getMypyPath(project, false)));
        mypyPathField = new TextFieldWithBrowseButton(autodetectTextField);
        JBTextField optionalTextField = new JBTextField();
        optionalTextField.getEmptyText().setText(MypyBundle.message("config.optional"));
        mypyConfigFilePathField = new TextFieldWithBrowseButton(optionalTextField);
    }

    private final class TestAction extends AbstractAction {

        TestAction() {
            putValue(Action.NAME, MypyBundle.message(
                    "config.mypy.path.test"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            String pathToMypy = getMypyPath(true);
            if (!pathToMypy.isEmpty() && MypyRunner.isMypyPathValid(pathToMypy, project)) {
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
