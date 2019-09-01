/*
 * Copyright (C) 2017 The Android Open Source Project
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
package dz.aosp.purelauncher.uioverrides;

import static dz.aosp.purelauncher.LauncherAnimUtils.OVERVIEW_TRANSITION_MS;
import static dz.aosp.purelauncher.anim.Interpolators.DEACCEL_2;
import static dz.aosp.purelauncher.states.RotationHelper.REQUEST_ROTATE;

import android.view.View;

import dz.aosp.purelauncher.AbstractFloatingView;
import dz.aosp.purelauncher.DeviceProfile;
import dz.aosp.purelauncher.Launcher;
import dz.aosp.purelauncher.LauncherState;
import dz.aosp.purelauncher.R;
import dz.aosp.purelauncher.Workspace;
import dz.aosp.purelauncher.allapps.DiscoveryBounce;
import dz.aosp.purelauncher.userevent.nano.LauncherLogProto.ContainerType;
import dz.aosp.quickstep.RecentsModel;
import dz.aosp.quickstep.views.RecentsView;

/**
 * Definition for overview state
 */
public class OverviewState extends LauncherState {

    private static final int STATE_FLAGS = FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED
            | FLAG_DISABLE_RESTORE | FLAG_OVERVIEW_UI | FLAG_DISABLE_ACCESSIBILITY;

    public OverviewState(int id) {
        this(id, OVERVIEW_TRANSITION_MS, STATE_FLAGS);
    }

    protected OverviewState(int id, int transitionDuration, int stateFlags) {
        super(id, ContainerType.TASKSWITCHER, transitionDuration, stateFlags);
    }

    @Override
    public float[] getWorkspaceScaleAndTranslation(Launcher launcher) {
        RecentsView recentsView = launcher.getOverviewPanel();
        Workspace workspace = launcher.getWorkspace();
        View workspacePage = workspace.getPageAt(workspace.getCurrentPage());
        float workspacePageWidth = workspacePage != null && workspacePage.getWidth() != 0
                ? workspacePage.getWidth() : launcher.getDeviceProfile().availableWidthPx;
        recentsView.getTaskSize(sTempRect);
        float scale = (float) sTempRect.width() / workspacePageWidth;
        float parallaxFactor = 0.5f;
        return new float[]{scale, 0, -getDefaultSwipeHeight(launcher) * parallaxFactor};
    }

    @Override
    public float[] getOverviewScaleAndTranslationYFactor(Launcher launcher) {
        return new float[] {1f, 0f};
    }

    @Override
    public void onStateEnabled(Launcher launcher) {
        RecentsView rv = launcher.getOverviewPanel();
        rv.setOverviewStateEnabled(true);
        AbstractFloatingView.closeAllOpenViews(launcher);
    }

    @Override
    public void onStateDisabled(Launcher launcher) {
        RecentsView rv = launcher.getOverviewPanel();
        rv.setOverviewStateEnabled(false);
        RecentsModel.getInstance(launcher).resetAssistCache();
    }

    @Override
    public void onStateTransitionEnd(Launcher launcher) {
        launcher.getRotationHelper().setCurrentStateRequest(REQUEST_ROTATE);
        DiscoveryBounce.showForOverviewIfNeeded(launcher);
    }

    public PageAlphaProvider getWorkspacePageAlphaProvider(Launcher launcher) {
        return new PageAlphaProvider(DEACCEL_2) {
            @Override
            public float getPageAlpha(int pageIndex) {
                return 0;
            }
        };
    }

    @Override
    public int getVisibleElements(Launcher launcher) {
        if (launcher.getDeviceProfile().isVerticalBarLayout()) {
            return VERTICAL_SWIPE_INDICATOR;
        } else {
            return HOTSEAT_SEARCH_BOX | VERTICAL_SWIPE_INDICATOR |
                    (launcher.getAppsView().getFloatingHeaderView().hasVisibleContent()
                            ? ALL_APPS_HEADER_EXTRA : HOTSEAT_ICONS);
        }
    }

    @Override
    public float getWorkspaceScrimAlpha(Launcher launcher) {
        return 0.5f;
    }

    @Override
    public float getVerticalProgress(Launcher launcher) {
        if ((getVisibleElements(launcher) & ALL_APPS_HEADER_EXTRA) == 0) {
            // We have no all apps content, so we're still at the fully down progress.
            return super.getVerticalProgress(launcher);
        }
        return 1 - (getDefaultSwipeHeight(launcher)
                / launcher.getAllAppsController().getShiftRange());
    }

    @Override
    public String getDescription(Launcher launcher) {
        return launcher.getString(R.string.accessibility_desc_recent_apps);
    }

    public static float getDefaultSwipeHeight(Launcher launcher) {
        DeviceProfile dp = launcher.getDeviceProfile();
        return dp.allAppsCellHeightPx - dp.allAppsIconTextSizePx;
    }
}
