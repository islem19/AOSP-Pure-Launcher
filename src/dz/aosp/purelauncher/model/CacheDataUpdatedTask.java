/*
 * Copyright (C) 2016 The Android Open Source Project
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
package dz.aosp.purelauncher.model;

import android.content.ComponentName;
import android.os.UserHandle;

import dz.aosp.purelauncher.AllAppsList;
import dz.aosp.purelauncher.AppInfo;
import dz.aosp.purelauncher.IconCache;
import dz.aosp.purelauncher.ItemInfo;
import dz.aosp.purelauncher.LauncherAppState;
import dz.aosp.purelauncher.LauncherModel.CallbackTask;
import dz.aosp.purelauncher.LauncherModel.Callbacks;
import dz.aosp.purelauncher.LauncherSettings;
import dz.aosp.purelauncher.ShortcutInfo;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Handles changes due to cache updates.
 */
public class CacheDataUpdatedTask extends BaseModelUpdateTask {

    public static final int OP_CACHE_UPDATE = 1;
    public static final int OP_SESSION_UPDATE = 2;

    private final int mOp;
    private final UserHandle mUser;
    private final HashSet<String> mPackages;

    public CacheDataUpdatedTask(int op, UserHandle user, HashSet<String> packages) {
        mOp = op;
        mUser = user;
        mPackages = packages;
    }

    @Override
    public void execute(LauncherAppState app, BgDataModel dataModel, AllAppsList apps) {
        IconCache iconCache = app.getIconCache();

        final ArrayList<AppInfo> updatedApps = new ArrayList<>();

        ArrayList<ShortcutInfo> updatedShortcuts = new ArrayList<>();
        synchronized (dataModel) {
            for (ItemInfo info : dataModel.itemsIdMap) {
                if (info instanceof ShortcutInfo && mUser.equals(info.user)) {
                    ShortcutInfo si = (ShortcutInfo) info;
                    ComponentName cn = si.getTargetComponent();
                    if (si.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                            && isValidShortcut(si) && cn != null
                            && mPackages.contains(cn.getPackageName())) {
                        iconCache.getTitleAndIcon(si, si.usingLowResIcon);
                        updatedShortcuts.add(si);
                    }
                }
            }
            apps.updateIconsAndLabels(mPackages, mUser, updatedApps);
        }
        bindUpdatedShortcuts(updatedShortcuts, mUser);

        if (!updatedApps.isEmpty()) {
            scheduleCallbackTask(new CallbackTask() {
                @Override
                public void execute(Callbacks callbacks) {
                    callbacks.bindAppsAddedOrUpdated(updatedApps);
                }
            });
        }
    }

    public boolean isValidShortcut(ShortcutInfo si) {
        switch (mOp) {
            case OP_CACHE_UPDATE:
                return true;
            case OP_SESSION_UPDATE:
                return si.hasPromiseIconUi();
            default:
                return false;
        }
    }
}
