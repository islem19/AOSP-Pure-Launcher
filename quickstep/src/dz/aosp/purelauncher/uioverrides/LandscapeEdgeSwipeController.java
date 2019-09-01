package dz.aosp.purelauncher.uioverrides;

import static dz.aosp.purelauncher.LauncherState.NORMAL;
import static dz.aosp.purelauncher.LauncherState.OVERVIEW;
import static dz.aosp.quickstep.TouchInteractionService.EDGE_NAV_BAR;

import android.view.MotionEvent;

import dz.aosp.purelauncher.AbstractFloatingView;
import dz.aosp.purelauncher.Launcher;
import dz.aosp.purelauncher.LauncherState;
import dz.aosp.purelauncher.LauncherStateManager.AnimationComponents;
import dz.aosp.purelauncher.touch.AbstractStateChangeTouchController;
import dz.aosp.purelauncher.touch.SwipeDetector;
import dz.aosp.purelauncher.userevent.nano.LauncherLogProto;
import dz.aosp.purelauncher.userevent.nano.LauncherLogProto.Action.Direction;
import dz.aosp.quickstep.RecentsModel;

/**
 * Touch controller for handling edge swipes in landscape/seascape UI
 */
public class LandscapeEdgeSwipeController extends AbstractStateChangeTouchController {

    private static final String TAG = "LandscapeEdgeSwipeCtrl";

    public LandscapeEdgeSwipeController(Launcher l) {
        super(l, SwipeDetector.HORIZONTAL);
    }

    @Override
    protected boolean canInterceptTouch(MotionEvent ev) {
        if (mCurrentAnimation != null) {
            // If we are already animating from a previous state, we can intercept.
            return true;
        }
        if (AbstractFloatingView.getTopOpenView(mLauncher) != null) {
            return false;
        }
        return mLauncher.isInState(NORMAL) && (ev.getEdgeFlags() & EDGE_NAV_BAR) != 0;
    }

    @Override
    protected LauncherState getTargetState(LauncherState fromState, boolean isDragTowardPositive) {
        boolean draggingFromNav = mLauncher.getDeviceProfile().isSeascape() != isDragTowardPositive;
        return draggingFromNav ? OVERVIEW : NORMAL;
    }

    @Override
    protected int getLogContainerTypeForNormalState() {
        return LauncherLogProto.ContainerType.NAVBAR;
    }

    @Override
    protected float getShiftRange() {
        return mLauncher.getDragLayer().getWidth();
    }

    @Override
    protected float initCurrentAnimation(@AnimationComponents int animComponent) {
        float range = getShiftRange();
        long maxAccuracy = (long) (2 * range);
        mCurrentAnimation = mLauncher.getStateManager().createAnimationToNewWorkspace(mToState,
                maxAccuracy, animComponent);
        return (mLauncher.getDeviceProfile().isSeascape() ? 2 : -2) / range;
    }

    @Override
    protected int getDirectionForLog() {
        return mLauncher.getDeviceProfile().isSeascape() ? Direction.RIGHT : Direction.LEFT;
    }

    @Override
    protected void onSwipeInteractionCompleted(LauncherState targetState, int logAction) {
        super.onSwipeInteractionCompleted(targetState, logAction);
        if (mStartState == NORMAL && targetState == OVERVIEW) {
            RecentsModel.getInstance(mLauncher).onOverviewShown(true, TAG);
        }
    }
}
