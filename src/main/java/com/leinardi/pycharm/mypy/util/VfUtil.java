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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class VfUtil {
    private VfUtil() {
    }

    public static VirtualFile findVfUp(VirtualFile item, String searchItemName) {
        VirtualFile parent = item.getParent();
        if (parent != null) {
            VirtualFile vf = VfsUtilCore.findRelativeFile(searchItemName, parent);
            if (vf != null && !vf.isDirectory()) {
                return vf;
            }
        }
        return findVfUp(parent, searchItemName);
    }

    public static Collection<VirtualFile> getAllSubFiles(VirtualFile virtualFile) {
        Collection<VirtualFile> list = new ArrayList<>();

        VfsUtilCore.visitChildrenRecursively(virtualFile, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory()) {
                    list.add(file);
                }
                return super.visitFile(file);
            }
        });

        return list;
    }

    public static List<VirtualFile> filterOnlyPythonProjectFiles(Project project, VirtualFile[] virtualFiles) {
        return filterOnlyPythonProjectFiles(project, Arrays.asList(virtualFiles));
    }

    public static List<VirtualFile> filterOnlyPythonProjectFiles(Project project, List<VirtualFile> virtualFiles) {
        List<VirtualFile> list = new ArrayList<>();
        ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);
        for (VirtualFile file : virtualFiles) {
            if (FileTypes.isPython(file.getFileType())
                    && !projectFileIndex.isExcluded(file)
                    && !projectFileIndex.isInLibraryClasses(file)) {
                list.add(file);
            }
        }
        return list;
    }

    public static List<VirtualFile> flattenFiles(final VirtualFile[] files) {
        final List<VirtualFile> flattened = new ArrayList<>();

        if (files != null) {
            for (final VirtualFile file : files) {
                flattened.addAll(VfUtil.getAllSubFiles(file));
            }
        }
        return flattened;
    }
}
