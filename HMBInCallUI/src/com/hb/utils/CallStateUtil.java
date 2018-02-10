package com.hb.utils;

import com.android.incallui.Call;
import com.android.incallui.CallList;
import com.android.incallui.CallUtils;
import com.android.incallui.InCallApp;
import com.android.incallui.InCallPresenter;
import com.android.incallui.Log;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.telecom.DisconnectCause;
import android.telecom.Call.Details;
import android.text.TextUtils;
import com.android.incallui.R;

public class CallStateUtil {
	private static final String IMS_MERGED_SUCCESSFULLY = "IMS_MERGED_SUCCESSFULLY";

	public static CharSequence getCallState() {
		Call call = CallList.getInstance().getActiveCall();
		if (call != null) {
			return getCallState(
					call.getState(),
					call.getVideoState(),
					call.getSessionModificationState(),
					call.getDisconnectCause(),
					// getConnectionLabel(),
					// getCallStateIcon(),
					// getGatewayNumber(),
					null, null, null, call.hasProperty(Details.PROPERTY_WIFI),
					call.isConferenceCall());
		}
		return "";
	}

	private static CharSequence getCallState(int state, int videoState,
			int sessionModificationState, DisconnectCause disconnectCause,
			String connectionLabel, Drawable callStateIcon,
			String gatewayNumber, boolean isWifi, boolean isConference) {
		boolean isGatewayCall = !TextUtils.isEmpty(gatewayNumber);
		return getCallStateLabelFromState(state, videoState,
				sessionModificationState, disconnectCause, connectionLabel,
				isGatewayCall, isWifi, isConference);
	}

	private static CharSequence getCallStateLabelFromState(int state,
			int videoState, int sessionModificationState,
			DisconnectCause disconnectCause, String label,
			boolean isGatewayCall, boolean isWifi, boolean isConference) {
		final Context context = InCallApp.getInstance();
		CharSequence callStateLabel = null; // Label to display as part of the
											// call banner

		boolean hasSuggestedLabel = label != null;
		boolean isAccount = hasSuggestedLabel && !isGatewayCall;
		boolean isAutoDismissing = false;

		switch (state) {
		case Call.State.IDLE:
			// "Call state" is meaningless in this state.
			break;
		case Call.State.ACTIVE:
			// We normally don't show a "call state label" at all in this state
			// (but we can use the call state label to display the provider
			// name).
			// / M:fix ALPS02503808, no need to show connection label if any
			// video request. @{
			/*
			 * Google code: if ((isAccount || isWifi || isConference) &&
			 * hasSuggestedLabel) {
			 */
			if ((isAccount || isWifi || isConference)
					&& hasSuggestedLabel
					&& sessionModificationState == Call.SessionModificationState.NO_REQUEST) {
				// / @}
				callStateLabel = label;
			} else if (sessionModificationState == Call.SessionModificationState.REQUEST_REJECTED) {
				callStateLabel = context
						.getString(R.string.card_title_video_call_rejected);
				isAutoDismissing = true;
			} else if (sessionModificationState == Call.SessionModificationState.REQUEST_FAILED) {
				callStateLabel = context
						.getString(R.string.card_title_video_call_error);
				isAutoDismissing = true;
			} else if (sessionModificationState == Call.SessionModificationState.WAITING_FOR_UPGRADE_RESPONSE) {
				callStateLabel = context
						.getString(R.string.card_title_video_call_requesting);
			} else if (sessionModificationState == Call.SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST) {
				// / M: fix ALPS02493295, modify incoming video call request
				// state label,
				// Google String:card_title_video_call_requesting. @{
				callStateLabel = context
						.getString(R.string.notification_requesting_video_call);
				// @}
				callStateLabel = appendCountdown(callStateLabel);
			} else if (CallUtils.isVideoCall(videoState)) {
				callStateLabel = context
						.getString(R.string.card_title_video_call);
			}
			break;
		case Call.State.ONHOLD:
			callStateLabel = context.getString(R.string.card_title_on_hold);
			break;
		case Call.State.CONNECTING:
		case Call.State.DIALING:
			//modify by lgy
//			if (hasSuggestedLabel && !isWifi) {
//				callStateLabel = context.getString(
//						R.string.calling_via_template, label);
//			} else {
				callStateLabel = context.getString(R.string.card_title_dialing);
//			}
			break;
		case Call.State.REDIALING:
			callStateLabel = context.getString(R.string.card_title_redialing);
			break;
		case Call.State.INCOMING:
		case Call.State.CALL_WAITING:
			// delete by lgy
			// /// M: [VoLTE conference]incoming volte conference @{
			// if (isIncomingVolteConferenceCall()) {
			// callStateLabel =
			// context.getString(R.string.card_title_incoming_conference);
			// break;
			// }
			// /// @}
			//
			// if (isWifi && hasSuggestedLabel) {
			// callStateLabel = label;
			// } else if (isAccount) {
			// callStateLabel =
			// context.getString(R.string.incoming_via_template, label);
			// } else if (VideoProfile.isTransmissionEnabled(videoState) ||
			// VideoProfile.isReceptionEnabled(videoState)) {
			// callStateLabel =
			// context.getString(R.string.notification_incoming_video_call);
			// } else {
			// callStateLabel =
			// context.getString(R.string.card_title_incoming_call);
			// }
			break;
		case Call.State.DISCONNECTING:
			// While in the DISCONNECTING state we display a "Hanging up"
			// message in order to make the UI feel more responsive. (In
			// GSM it's normal to see a delay of a couple of seconds while
			// negotiating the disconnect with the network, so the "Hanging
			// up" state at least lets the user know that we're doing
			// something. This state is currently not used with CDMA.)
			callStateLabel = context.getString(R.string.card_title_hanging_up);
			break;
		case Call.State.DISCONNECTED:
			callStateLabel = disconnectCause.getLabel();
			// M:fix CR:ALPS02584915,UI show error when merge conference call.
			if (TextUtils.isEmpty(callStateLabel)
					&& !IMS_MERGED_SUCCESSFULLY.equals(disconnectCause
							.getReason())) {
				Log.d(CallStateUtil.class,
						" disconnect reason is not ims merged successfully");
				callStateLabel = context
						.getString(R.string.card_title_call_ended);
			}
			break;
		case Call.State.CONFERENCED:
			callStateLabel = context.getString(R.string.card_title_conf_call);
			break;
		default:
			Log.wtf(CallStateUtil.class,
					"updateCallStateWidgets: unexpected call: " + state);
		}
		return callStateLabel;
	}

	private static CharSequence appendCountdown(CharSequence originalText) {
		long countdown = InCallPresenter.getInstance()
				.getAutoDeclineCountdown();
		if (countdown < 0) {
			return originalText;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(originalText).append(" (").append(countdown).append(")");
		return sb.toString();
	}

}