package dz.aosp.purelauncher.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.os.Process;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import dz.aosp.purelauncher.IconCache;
import dz.aosp.purelauncher.InvariantDeviceProfile;
import dz.aosp.purelauncher.ItemInfo;
import dz.aosp.purelauncher.LauncherAppState;
import dz.aosp.purelauncher.ShortcutInfo;
import dz.aosp.purelauncher.Utilities;
import dz.aosp.purelauncher.compat.LauncherAppsCompat;
import dz.aosp.purelauncher.graphics.BitmapInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;

import static dz.aosp.purelauncher.LauncherSettings.BaseLauncherColumns.INTENT;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.CELLX;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.CELLY;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.CONTAINER;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.CONTAINER_DESKTOP;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.CONTAINER_HOTSEAT;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.ICON;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.ICON_PACKAGE;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.ICON_RESOURCE;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.ITEM_TYPE;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.PROFILE_ID;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.RESTORED;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.SCREEN;
import static dz.aosp.purelauncher.LauncherSettings.Favorites.TITLE;
import static dz.aosp.purelauncher.LauncherSettings.Favorites._ID;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LoaderCursor}
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class LoaderCursorTest {

    private LauncherAppState mMockApp;
    private IconCache mMockIconCache;

    private MatrixCursor mCursor;
    private InvariantDeviceProfile mIDP;
    private Context mContext;
    private LauncherAppsCompat mLauncherApps;

    private LoaderCursor mLoaderCursor;

    @Before
    public void setup() {
        mIDP = new InvariantDeviceProfile();
        mCursor = new MatrixCursor(new String[] {
                ICON, ICON_PACKAGE, ICON_RESOURCE, TITLE,
                _ID, CONTAINER, ITEM_TYPE, PROFILE_ID,
                SCREEN, CELLX, CELLY, RESTORED, INTENT
        });
        mContext = InstrumentationRegistry.getTargetContext();

        mMockApp = mock(LauncherAppState.class);
        mMockIconCache = mock(IconCache.class);
        when(mMockApp.getIconCache()).thenReturn(mMockIconCache);
        when(mMockApp.getInvariantDeviceProfile()).thenReturn(mIDP);
        when(mMockApp.getContext()).thenReturn(mContext);
        mLauncherApps = LauncherAppsCompat.getInstance(mContext);

        mLoaderCursor = new LoaderCursor(mCursor, mMockApp);
        mLoaderCursor.allUsers.put(0, Process.myUserHandle());
    }

    private void initCursor(int itemType, String title) {
        mCursor.newRow()
                .add(_ID, 1)
                .add(PROFILE_ID, 0)
                .add(ITEM_TYPE, itemType)
                .add(TITLE, title)
                .add(CONTAINER, CONTAINER_DESKTOP);
    }

    @Test
    public void getAppShortcutInfo_dontAllowMissing_invalidComponent() {
        initCursor(ITEM_TYPE_APPLICATION, "");
        assertTrue(mLoaderCursor.moveToNext());
        ComponentName cn = new ComponentName(mContext.getPackageName(), "dummy-do");
        assertNull(mLoaderCursor.getAppShortcutInfo(
                new Intent().setComponent(cn), false /* allowMissingTarget */, true));
    }

    @Test
    public void getAppShortcutInfo_dontAllowMissing_validComponent() {
        initCursor(ITEM_TYPE_APPLICATION, "");
        assertTrue(mLoaderCursor.moveToNext());

        ComponentName cn = mLauncherApps.getActivityList(null, mLoaderCursor.user)
                .get(0).getComponentName();
        ShortcutInfo info = mLoaderCursor.getAppShortcutInfo(
                new Intent().setComponent(cn), false /* allowMissingTarget */, true);
        assertNotNull(info);
        assertTrue(Utilities.isLauncherAppTarget(info.intent));
    }

    @Test
    public void getAppShortcutInfo_allowMissing_invalidComponent() {
        initCursor(ITEM_TYPE_APPLICATION, "");
        assertTrue(mLoaderCursor.moveToNext());

        ComponentName cn = new ComponentName(mContext.getPackageName(), "dummy-do");
        ShortcutInfo info = mLoaderCursor.getAppShortcutInfo(
                new Intent().setComponent(cn), true  /* allowMissingTarget */, true);
        assertNotNull(info);
        assertTrue(Utilities.isLauncherAppTarget(info.intent));
    }

    @Test
    public void loadSimpleShortcut() {
        initCursor(ITEM_TYPE_SHORTCUT, "my-shortcut");
        assertTrue(mLoaderCursor.moveToNext());

        Bitmap icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
        when(mMockIconCache.getDefaultIcon(eq(mLoaderCursor.user)))
                .thenReturn(BitmapInfo.fromBitmap(icon));
        ShortcutInfo info = mLoaderCursor.loadSimpleShortcut();
        assertEquals(icon, info.iconBitmap);
        assertEquals("my-shortcut", info.title);
        assertEquals(ITEM_TYPE_SHORTCUT, info.itemType);
    }

    @Test
    public void checkItemPlacement_wrongWorkspaceScreen() {
        ArrayList<Long> workspaceScreens = new ArrayList<>(Arrays.asList(1L, 3L));
        mIDP.numRows = 4;
        mIDP.numColumns = 4;
        mIDP.numHotseatIcons = 3;

        // Item on unknown screen are not placed
        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, CONTAINER_DESKTOP, 4L), workspaceScreens));
        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, CONTAINER_DESKTOP, 5L), workspaceScreens));
        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, CONTAINER_DESKTOP, 2L), workspaceScreens));

        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, CONTAINER_DESKTOP, 1L), workspaceScreens));
        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, CONTAINER_DESKTOP, 3L), workspaceScreens));

    }
    @Test
    public void checkItemPlacement_outsideBounds() {
        ArrayList<Long> workspaceScreens = new ArrayList<>(Arrays.asList(1L, 2L));
        mIDP.numRows = 4;
        mIDP.numColumns = 4;
        mIDP.numHotseatIcons = 3;

        // Item outside screen bounds are not placed
        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(4, 4, 1, 1, CONTAINER_DESKTOP, 1L), workspaceScreens));
    }

    @Test
    public void checkItemPlacement_overlappingItems() {
        ArrayList<Long> workspaceScreens = new ArrayList<>(Arrays.asList(1L, 2L));
        mIDP.numRows = 4;
        mIDP.numColumns = 4;
        mIDP.numHotseatIcons = 3;

        // Overlapping items are not placed
        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, CONTAINER_DESKTOP, 1L), workspaceScreens));
        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, CONTAINER_DESKTOP, 1L), workspaceScreens));

        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, CONTAINER_DESKTOP, 2L), workspaceScreens));
        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(0, 0, 1, 1, CONTAINER_DESKTOP, 2L), workspaceScreens));

        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(1, 1, 1, 1, CONTAINER_DESKTOP, 1L), workspaceScreens));
        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(2, 2, 2, 2, CONTAINER_DESKTOP, 1L), workspaceScreens));

        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(3, 2, 1, 2, CONTAINER_DESKTOP, 1L), workspaceScreens));
    }

    @Test
    public void checkItemPlacement_hotseat() {
        ArrayList<Long> workspaceScreens = new ArrayList<>();
        mIDP.numRows = 4;
        mIDP.numColumns = 4;
        mIDP.numHotseatIcons = 3;

        // Hotseat items are only placed based on screenId
        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(3, 3, 1, 1, CONTAINER_HOTSEAT, 1L), workspaceScreens));
        assertTrue(mLoaderCursor.checkItemPlacement(
                newItemInfo(3, 3, 1, 1, CONTAINER_HOTSEAT, 2L), workspaceScreens));

        assertFalse(mLoaderCursor.checkItemPlacement(
                newItemInfo(3, 3, 1, 1, CONTAINER_HOTSEAT, 3L), workspaceScreens));
    }

    private ItemInfo newItemInfo(int cellX, int cellY, int spanX, int spanY,
            long container, long screenId) {
        ItemInfo info = new ItemInfo();
        info.cellX = cellX;
        info.cellY = cellY;
        info.spanX = spanX;
        info.spanY = spanY;
        info.container = container;
        info.screenId = screenId;
        return info;
    }
}
