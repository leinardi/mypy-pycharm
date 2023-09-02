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

package com.leinardi.pycharm.mypy;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "MypyConfigService", storages = {@Storage("mypy.xml")})
public class MypyConfigService implements PersistentStateComponent<MypyConfigService> {
    private String customMypyPath;
    private String mypyConfigFilePath;
    private String mypyArguments;
    private boolean scanBeforeCheckin;

    public MypyConfigService() {
        customMypyPath = "";
        mypyArguments = "";
        mypyConfigFilePath = "";
    }

    public String getCustomMypyPath() {
        return customMypyPath;
    }

    public void setCustomMypyPath(String pathToMypy) {
        this.customMypyPath = pathToMypy;
    }

    public String getMypyConfigFilePath() {
        return mypyConfigFilePath;
    }

    public void setMypyConfigFilePath(String pathToMypyrcFile) {
        this.mypyConfigFilePath = pathToMypyrcFile;
    }

    public String getMypyArguments() {
        return mypyArguments;
    }

    public void setMypyArguments(String mypyArguments) {
        this.mypyArguments = mypyArguments;
    }

    public boolean isScanBeforeCheckin() {
        return scanBeforeCheckin;
    }

    public void setScanBeforeCheckin(boolean scanBeforeCheckin) {
        this.scanBeforeCheckin = scanBeforeCheckin;
    }

    @Nullable
    @Override
    public MypyConfigService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull MypyConfigService config) {
        XmlSerializerUtil.copyBean(config, this);
    }

    @Nullable
    public static MypyConfigService getInstance(Project project) {
        return project.getService(MypyConfigService.class);
    }
}
