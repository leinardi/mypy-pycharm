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

package com.leinardi.pycharm.mypy.util;

import com.intellij.openapi.components.ServiceKt;
import com.intellij.openapi.components.StorageScheme;
import com.intellij.openapi.components.impl.stores.IProjectStore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jdesktop.swingx.util.OS;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Locate and/or create temporary directories for use by this plugin.
 */
public class TempDirProvider {
    public String forPersistedPsiFile(final PsiFile tempPsiFile) {
        String systemTempDir = System.getProperty("java.io.tmpdir");
        if (OS.isWindows() && driveLetterOf(systemTempDir) != driveLetterOf(pathOf(tempPsiFile))) {
            // Some tool on Windows requires the files to be on the same drive
            final File projectTempDir = temporaryDirectoryLocationFor(tempPsiFile.getProject());
            if (projectTempDir.exists() || projectTempDir.mkdirs()) {
                projectTempDir.deleteOnExit();
                return projectTempDir.getAbsolutePath();
            }
        }
        return systemTempDir;
    }

    @NotNull
    private File temporaryDirectoryLocationFor(final Project project) {
        return getIdeaFolder(project).map(vf -> new File(vf.getPath(), "mypymypy.tmp"))
                .orElse(new File(project.getBasePath(), "mypymypy.tmp"));
    }

    Optional<VirtualFile> getIdeaFolder(@NotNull final Project pProject) {
        final IProjectStore projectStore = (IProjectStore) ServiceKt.getStateStore(pProject);
        if (projectStore.getStorageScheme() == StorageScheme.DIRECTORY_BASED) {
            final VirtualFile ideaStorageDir =
                    ProjectUtil.guessProjectDir(pProject).findChild(Project.DIRECTORY_STORE_FOLDER);
            if (ideaStorageDir != null && ideaStorageDir.exists() && ideaStorageDir.isDirectory()) {
                return Optional.of(ideaStorageDir);
            }
        }
        return Optional.empty();
    }

    private char driveLetterOf(final String windowsPath) {
        if (windowsPath != null && windowsPath.length() > 0) {
            final Path normalisedPath = Paths.get(windowsPath).normalize().toAbsolutePath();
            return normalisedPath.toFile().toString().charAt(0);
        }
        return '?';
    }

    private String pathOf(@NotNull final PsiFile file) {
        return virtualFileOf(file).map(VirtualFile::getPath).orElseThrow(() -> new IllegalStateException("PSIFile " +
                "does not have associated virtual file: " + file));
    }

    private Optional<VirtualFile> virtualFileOf(final PsiFile file) {
        return Optional.ofNullable(file.getVirtualFile());
    }

}
