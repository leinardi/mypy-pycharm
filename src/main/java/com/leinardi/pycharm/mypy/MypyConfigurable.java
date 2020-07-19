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

package com.leinardi.pycharm.mypy;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.leinardi.pycharm.mypy.ui.MypyConfigPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

/**
 * The "configurable component" required by PyCharm to provide a Swing form for inclusion into the 'Settings'
 * dialog. Registered in {@code plugin.xml} as a {@code projectConfigurable} extension.
 */
public class MypyConfigurable implements Configurable {
    private static final Logger LOG = Logger.getInstance(MypyConfigurable.class);

    private final MypyConfigPanel configPanel;
    private final MypyConfigService mypyConfigService;

    public MypyConfigurable(@NotNull final Project project) {
        this(project, new MypyConfigPanel(project));
    }

    MypyConfigurable(@NotNull final Project project,
                     @NotNull final MypyConfigPanel configPanel) {
        this.configPanel = configPanel;
        mypyConfigService = MypyConfigService.getInstance(project);
    }

    @Override
    public String getDisplayName() {
        return MypyBundle.message("plugin.configuration-name");
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        reset();
        return configPanel.getPanel();
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean isModified() {
        boolean result = !configPanel.getMypyPath().equals(mypyConfigService.getCustomMypyPath())
                || !configPanel.getMypyConfigFilePath().equals(mypyConfigService.getMypyConfigFilePath())
                || !configPanel.getMypyArguments().equals(mypyConfigService.getMypyArguments())
                || !configPanel.getUseDaemon() == (mypyConfigService.isUseDaemon());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Has config changed? " + result);
        }
        return result;
    }

    @Override
    public void apply() {
        mypyConfigService.setCustomMypyPath(configPanel.getMypyPath());
        mypyConfigService.setMypyConfigFilePath(configPanel.getMypyConfigFilePath());
        mypyConfigService.setMypyArguments(configPanel.getMypyArguments());
        mypyConfigService.setUseDaemon(configPanel.getUseDaemon());
    }

    @Override
    public void disposeUIResources() {
        // do nothing
    }
}
