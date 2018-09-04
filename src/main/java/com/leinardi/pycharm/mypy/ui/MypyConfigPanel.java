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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.leinardi.pycharm.mypy.MypyBundle;
import com.leinardi.pycharm.mypy.MypyConfigService;
import com.leinardi.pycharm.mypy.mpapi.MypyRunner;
import com.leinardi.pycharm.mypy.util.Icons;
import com.leinardi.pycharm.mypy.util.Notifications;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Paths;

import static org.apache.commons.lang.StringUtils.isBlank;

public class MypyConfigPanel {
    private JTextField pathToMypyTextField;
    private JPanel rootPanel;
    private JButton browseButton;
    private JButton testButton;
    private JTextField argumentsTextField;
    private Project project;

    public MypyConfigPanel(Project project) {
        this.project = project;
        MypyConfigService mypyConfigService = MypyConfigService.getInstance(project);
        browseButton.setAction(new BrowseAction());
        testButton.setAction(new TestAction());
        pathToMypyTextField.setText(mypyConfigService.getPathToMypy());
        argumentsTextField.setText(mypyConfigService.getMypyArguments());
    }

    public JPanel getPanel() {
        return rootPanel;
    }

    public String getPathToMypy() {
        return pathToMypyTextField.getText();
    }

    public String getMypyArguments() {
        return argumentsTextField.getText();
    }

    private String fileLocation() {
        final String filename = trim(pathToMypyTextField.getText());

        if (new File(filename).exists()) {
            return filename;
        }

        final File projectRelativePath = projectRelativeFileOf(filename);
        if (projectRelativePath.exists()) {
            return projectRelativePath.getAbsolutePath();
        }

        return filename;
    }

    private File projectRelativeFileOf(final String filename) {
        return Paths.get(new File(project.getBasePath(), filename).getAbsolutePath())
                .normalize()
                .toAbsolutePath()
                .toFile();
    }

    private String trim(final String text) {
        if (text != null) {
            return text.trim();
        }
        return null;
    }

    private final class BrowseAction extends AbstractAction {

        BrowseAction() {
            putValue(Action.NAME, MypyBundle.message(
                    "config.file.browse.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    MypyBundle.message("config.file.browse.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    MypyBundle.message("config.file.browse.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final VirtualFile toSelect;
            final String configFilePath = fileLocation();
            if (!isBlank(configFilePath)) {
                toSelect = LocalFileSystem.getInstance().findFileByPath(configFilePath);
            } else {
                toSelect = project.getBaseDir();
            }

            final FileChooserDescriptor descriptor = new FileChooserDescriptor(
                    true,
                    false,
                    false,
                    false,
                    false,
                    false);
            final VirtualFile chosen = FileChooser.chooseFile(descriptor, project, toSelect);
            if (chosen != null) {
                final File newConfigFile = VfsUtilCore.virtualToIoFile(chosen);
                pathToMypyTextField.setText(newConfigFile.getAbsolutePath());
            }
        }
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
