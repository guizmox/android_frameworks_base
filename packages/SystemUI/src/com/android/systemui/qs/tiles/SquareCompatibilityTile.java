package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.widget.Switch;
import android.util.Log;
import android.content.ComponentName;
import android.view.WindowManager;
import android.graphics.Point;

import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import java.util.List;
import android.os.RemoteException;

import javax.inject.Inject;

/** Quick settings tile: Control square compatibility **/
public class SquareCompatibilityTile extends QSTileImpl<BooleanState> {
    private static final String TAG = "SquareCompatibilityTile";

    private final Icon mIconOff = ResourceIcon.get(R.drawable.ic_qs_square_compatibility_off);
    private final Icon mIconOn = ResourceIcon.get(R.drawable.ic_qs_square_compatibility_on);

    private final Callback mCallback;
    private final KeyguardStateController mKeyguard;

    @Inject
    public SquareCompatibilityTile(QSHost host, KeyguardStateController keyguardStateController) {
        super(host);
        mCallback = new Callback();
        mKeyguard = keyguardStateController;
    }

    @Override
    public boolean isAvailable() {
		WindowManager wm = (WindowManager) mContext.getSystemService("window");
        Point size = new Point();
        wm.getDefaultDisplay().getRealSize(size);
        return size.x == size.y;
    }

    @Override
    public BooleanState newTileState() {
        BooleanState state = new BooleanState();
        state.handlesLongClick = false;
        return state;
    }

    @Override
    protected void handleClick() {
		try {
			String packageName = getTopActivityPackageName();
			if (!ActivityManagerNative.getDefault().getPackageSquareCompatMode(packageName)) {
                ActivityManagerNative.getDefault().setPackageSquareCompatMode(packageName, true);
			} else {
                ActivityManagerNative.getDefault().setPackageSquareCompatMode(packageName, false);
			}
	        refreshState();
        } catch (RemoteException e) {
            Log.e(TAG, "Can't switch compat mode: " + e.getMessage());
        }
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_square_compatibility_label);
    }

    private String getTopActivityPackageName() {
        try {
            List<ActivityManager.RunningTaskInfo> taskInfo = ActivityManagerNative.getDefault().getTasks(1);
            if (taskInfo == null || taskInfo.isEmpty()) {
                return null;
            }
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            return componentInfo.getPackageName();
        } catch (RemoteException e) {
            Log.e(TAG, "Could not find package name for top activity:" + e.getMessage());
            return null;
        }
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
		state.label = getTileLabel();
		state.expandedAccessibilityClassName = Switch.class.getName();
		try {
			String packageName = getTopActivityPackageName();

			if (ActivityManagerNative.getDefault().getPackageSquareCompatMode(packageName)) {
				state.icon = mIconOn;
				state.value = false;
				state.secondaryLabel = mHost.getContext().getString(R.string.quick_settings_square_compatibility_on);
			} else {
				state.icon = mIconOff;
				state.value = true;
				state.secondaryLabel = mHost.getContext().getString(R.string.quick_settings_square_compatibility_off);
			}

			if (!ActivityManagerNative.getDefault().userCanChangeSquareCompatMode(packageName) || mKeyguard.isShowing()) {
				state.secondaryLabel = mContext.getString(R.string.quick_settings_square_compatibility_disabled);
				state.state = Tile.STATE_UNAVAILABLE;
			} else {
				state.state = state.value ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
			}
		} catch (RemoteException e) {
			Log.e(TAG, "Could not get package name: " + e.getMessage());
		}
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    @Override
    public void handleSetListening(boolean listening) {
        super.handleSetListening(listening);
        if (listening) {
            mKeyguard.addCallback(mCallback);
        } else {
            mKeyguard.removeCallback(mCallback);
        }
    }

    private final class Callback implements KeyguardStateController.Callback {
        @Override
        public void onKeyguardShowingChanged() {
            refreshState();
        }
    };
}
