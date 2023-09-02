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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.webcore.packaging.PackageManagementService;
import com.intellij.webcore.packaging.PackageManagementService.ErrorDescription;
import com.leinardi.pycharm.mypy.MypyBundle;
import com.leinardi.pycharm.mypy.actions.Settings;
import com.leinardi.pycharm.mypy.mpapi.MypyRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

import static com.intellij.notification.NotificationListener.URL_OPENING_LISTENER;
import static com.intellij.notification.NotificationType.ERROR;
import static com.intellij.notification.NotificationType.INFORMATION;
import static com.intellij.notification.NotificationType.WARNING;
import static com.leinardi.pycharm.mypy.MypyBundle.message;
import static com.leinardi.pycharm.mypy.util.Exceptions.rootCauseOf;

public final class Notifications {
    private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(Notifications.class);
    private static final NotificationGroup BALLOON_GROUP =
            NotificationGroupManager.getInstance().getNotificationGroup("alerts");
    private static final NotificationGroup LOG_ONLY_GROUP =
            NotificationGroupManager.getInstance().getNotificationGroup("logging");
    private static final String TITLE = message("plugin.name");

    private Notifications() {
    }

    public static void showInfo(final Project project, final String infoText) {
        BALLOON_GROUP
                .createNotification(TITLE, infoText, INFORMATION)
                .setListener(URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showWarning(final Project project, final String warningText) {
        BALLOON_GROUP
                .createNotification(TITLE, warningText, WARNING)
                .setListener(URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showWarning(final Project project, final String title, final String warningText) {
        BALLOON_GROUP
                .createNotification(title, warningText, WARNING)
                .setListener(URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showError(final Project project, final String errorText) {
        BALLOON_GROUP
                .createNotification(TITLE, errorText, ERROR)
                .setListener(URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showException(final Project project, final Throwable t) {
        LOG_ONLY_GROUP
                .createNotification(message("plugin.exception"), messageFor(t), ERROR)
                .setListener(URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showInstallMypy(final Project project) {
        Notification notification = BALLOON_GROUP
                .createNotification(
                        TITLE,
                        MypyBundle.message("plugin.notification.install-mypy.content"),
                        ERROR)
                .setSubtitle(MypyBundle.message("plugin.notification.install-mypy.subtitle"))
                .setListener(URL_OPENING_LISTENER);
        notification
                .addAction(new InstallMypyAction(project, notification))
                .notify(project);
    }

    public static void showUnableToRunMypy(final Project project) {
        Notification notification = BALLOON_GROUP
                .createNotification(
                        TITLE,
                        MypyBundle.message("plugin.notification.unable-to-run-mypy.content"),
                        ERROR)
                .setSubtitle(MypyBundle.message("plugin.notification.unable-to-run-mypy.subtitle"))
                .setListener(URL_OPENING_LISTENER);
        notification
                .addAction(new OpenPluginSettingsAction(notification))
                .notify(project);
    }

    public static void showMypyAbnormalExit(final Project project, final String detail) {
        BALLOON_GROUP
                .createNotification(TITLE, detail, ERROR)
                .setListener(URL_OPENING_LISTENER)
                .setSubtitle(MypyBundle.message("plugin.notification.abnormal-exit.subtitle"))
                .notify(project);
    }

    public static void showNoPythonInterpreter(Project project) {
        Notification notification = BALLOON_GROUP
                .createNotification(
                        TITLE,
                        MypyBundle.message("plugin.notification.no-python-interpreter.content"),
                        ERROR)
                .setListener(URL_OPENING_LISTENER);
        notification
                .addAction(new ConfigurePythonInterpreterAction(project, notification))
                .notify(project);
    }

    @NotNull
    private static String messageFor(final Throwable t) {
        if (t.getCause() != null) {
            return message("mypy.exception-with-root-cause", t.getMessage(), traceOf(rootCauseOf(t)));
        }
        return message("mypy.exception", traceOf(t));
    }

    private static String traceOf(final Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return t.getMessage() + "<br>" + sw.toString()
                .replaceAll("\t", "&nbsp;&nbsp;")
                .replaceAll("\n", "<br>");
    }

    private static class OpenPluginSettingsAction extends AnAction {
        private final Notification notification;

        OpenPluginSettingsAction(Notification notification) {
            super(MypyBundle.message("plugin.notification.action.plugin-settings"));
            this.notification = notification;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            new Settings().actionPerformed(event);
            notification.expire();
        }
    }

    private static class ConfigurePythonInterpreterAction extends AnAction {
        private final Project project;
        private final Notification notification;

        ConfigurePythonInterpreterAction(Project project, Notification notification) {
            super(MypyBundle.message("plugin.notification.action.configure-python-interpreter"));
            this.project = project;
            this.notification = notification;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent ignored) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "Project Interpreter");
            notification.expire();
        }
    }

    private static class InstallMypyAction extends AnAction {
        private final Project project;
        private final Notification notification;

        InstallMypyAction(Project project, Notification notification) {
            super(MypyBundle.message("plugin.notification.action.install-mypy"));
            this.project = project;
            this.notification = notification;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent ignored) {
            Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
            if (projectSdk == null) {
                LOG.debug("Project interpreter not set");
            } else {
                ApplicationManager.getApplication().invokeLater(() -> {
                    final PackageManagementService.Listener listener = new PackageManagementService.Listener() {
                        @Override
                        public void operationStarted(final String packageName) {
                            notification.expire();
                        }

                        @Override
                        public void operationFinished(final String packageName,
                                                      @Nullable final ErrorDescription errorDescription) {
                            notification.expire();
                        }
                    };
                    PyPackageManagerUtil.install(project, MypyRunner.MYPY_PACKAGE_NAME, listener);
                });
            }
        }
    }

}
