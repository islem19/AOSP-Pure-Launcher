/* AUTO-GENERATED FILE. DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * aapt tool from the resource data it found. It
 * should not be modified by hand.
 */

package dz.aosp.purelauncher;

public final class Manifest {
  public static final class permission {
    /**
     * Manifest entries specific to Launcher3. This is merged with AndroidManifest-common.xml.
     * Refer comments around specific entries on how to extend individual components.
     * Permissions required for read/write access to the workspace data. These permission name
     * should not conflict with that defined in other apps, as such an app should embed its package
     * name in the permissions. eq com.mypackage.permission.READ_SETTINGS
     */
    public static final String READ_SETTINGS="dz.aosp.purelauncher.permission.READ_SETTINGS";
    public static final String WRITE_SETTINGS="dz.aosp.purelauncher.permission.WRITE_SETTINGS";
    /**
     * The manifest defines the common entries that should be present in any derivative of Launcher3.
     * The components should generally not require any changes.
     * Rest of the components are defined in AndroidManifest.xml which is merged with this manifest
     * at compile time. Note that the components defined in AndroidManifest.xml are also required,
     * with some minor changed based on the derivative app.
     */
    public static final String INSTALL_SHORTCUT="com.android.launcher.permission.INSTALL_SHORTCUT";
  }

}