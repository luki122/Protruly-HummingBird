/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.launcher3.config;

import com.android.launcher3.folder.FolderIcon;

/**
 * Defines a set of flags used to control various launcher behaviors
 */
public final class FeatureFlags {
    private FeatureFlags() {}

    // Custom flags go below this
    public static boolean LAUNCHER3_DISABLE_ICON_NORMALIZATION = true;
    // As opposed to the new spring-loaded workspace.
    public static boolean LAUNCHER3_LEGACY_WORKSPACE_DND = false;
    public static FolderIcon.FolderIconMode LAUNCHER3_LEGACY_FOLDER_ICON = FolderIcon.FolderIconMode.SudokuFolderIconLayoutRule;
    public static boolean LAUNCHER3_USE_SYSTEM_DRAG_DRIVER = true;
    public static boolean LAUNCHER3_DISABLE_PINCH_TO_OVERVIEW = false;
    public static boolean LAUNCHER3_ALL_APPS_PULL_UP = false;

    public static boolean LAUNCHER3_ENABLE_QUICKSEARCHBAR = false;

    // Feature flag to enable moving the QSB on the 0th screen of the workspace.
    public static final boolean QSB_ON_FIRST_SCREEN = LAUNCHER3_ENABLE_QUICKSEARCHBAR;
    // When enabled the all-apps icon is not added to the hotseat.
    public static final boolean NO_ALL_APPS_ICON = true;
    // When enabled fling down gesture on the first workspace triggers search.
    public static final boolean PULLDOWN_SEARCH = false;
    // When enabled the status bar may show dark icons based on the top of the wallpaper.
    public static final boolean LIGHT_STATUS_BAR = false;

    public static boolean LAUNCHER3_LEGACY_DELETEDROPTARGET = false;
    public static boolean SHOW_ALL_APPS_ON_WORKSPACE = true;

    //lijun add for pageindicator cube
    public static boolean SHOW_PAGEINDICATOR_CUBE = true;

    //lijun add to enable WidgetsContainerPage
    public static boolean WIDGETS_CONTAINER_PAGE = true;

    // Feature flag to enable uninstalling the app by clicking icons;
    public static final boolean UNINSTALL_MODE = true;

    //lijun add for overscroll
    public static final boolean OVERSCROLL_SPRINGBACK = true;

      // cyl add for cycle slide 
	public static final boolean CYCLE_SLIDE = false;
}
