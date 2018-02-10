/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.incallui;

import hb.app.dialog.AlertDialog;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telecom.VideoProfile;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnTouchListener;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hb.listener.ScreenListener;
import com.hb.listener.ScreenListener.ScreenStateListener;

import java.util.ArrayList;
import java.util.List;

/// M: add for plug in. @{
import com.mediatek.incallui.ext.ExtensionManager;
/// @}

/**
 *
 */
public class AnswerFragment extends BaseFragment<AnswerPresenter, AnswerPresenter.AnswerUi>
        implements GlowPadWrapper.AnswerListener, AnswerPresenter.AnswerUi {

    public static final int TARGET_SET_FOR_AUDIO_WITHOUT_SMS = 0;
    public static final int TARGET_SET_FOR_AUDIO_WITH_SMS = 1;
    public static final int TARGET_SET_FOR_VIDEO_WITHOUT_SMS = 2;
    public static final int TARGET_SET_FOR_VIDEO_WITH_SMS = 3;
    public static final int TARGET_SET_FOR_VIDEO_ACCEPT_REJECT_REQUEST = 4;
    /**
     * M: [video call]3G VT call doesn't support answer as voice.
     */
    public static final int TARGET_SET_FOR_VIDEO_WITHOUT_SMS_AUDIO = 10;

    /**
     * The popup showing the list of canned responses.
     *
     * This is an AlertDialog containing a ListView showing the possible choices.  This may be null
     * if the InCallScreen hasn't ever called showRespondViaSmsPopup() yet, or if the popup was
     * visible once but then got dismissed.
     */
    private Dialog mCannedResponsePopup = null;

    /**
     * The popup showing a text field for users to type in their custom message.
     */
    private AlertDialog mCustomMessagePopup = null;

    private ArrayAdapter<String> mSmsResponsesAdapter;

    private final List<String> mSmsResponses = new ArrayList<>();

    private GlowPadWrapper mGlowpad;

    public AnswerFragment() {
    }

    @Override
    public AnswerPresenter createPresenter() {
        return InCallPresenter.getInstance().getAnswerPresenter();
    }

    @Override
    public AnswerPresenter.AnswerUi getUi() {
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	//modify by lgy
//        mGlowpad = (GlowPadWrapper) inflater.inflate(R.layout.answer_fragment,
//                 container, false);
        final View parent = inflater.inflate(R.layout.answer_fragment, container, false);      
        mGlowpad = (GlowPadWrapper) parent.findViewById(R.id.glow_pad_view);

        Log.d(this, "Creating view for answer fragment ", this);
        Log.d(this, "Created from activity", getActivity());
        mGlowpad.setAnswerListener(this);
        ExtensionManager.getVilteAutoTestHelperExt().registerReceiverForAcceptAndRejectUpgrade(
                getActivity(),getPresenter());
        initHbViews(parent);
//        return mGlowpad;
        return parent;
    }

    @Override
    public void onDestroyView() {
        Log.d(this, "onDestroyView");
        if (mGlowpad != null) {
            mGlowpad.stopPing();
            mGlowpad = null;
        }
        ExtensionManager.getVilteAutoTestHelperExt().unregisterReceiverForAcceptAndRejectUpgrade();
        //add by lgy 
//        mArrowAnimator.cancel();
        mScreenListener.unregisterListener();
        super.onDestroyView();
    }

    @Override
    public void onShowAnswerUi(boolean shown) {
        Log.d(this, "Show answer UI: " + shown);
        if (shown) {
            mGlowpad.startPing();
        } else {
            mGlowpad.stopPing();
        }
    }

    /**
     * Sets targets on the glowpad according to target set identified by the parameter.
     * @param targetSet Integer identifying the set of targets to use.
     */
    public void showTargets(int targetSet) {
        showTargets(targetSet, VideoProfile.STATE_BIDIRECTIONAL);
    }

    /**
     * Sets targets on the glowpad according to target set identified by the parameter.
     * @param targetSet Integer identifying the set of targets to use.
     */
    @Override
    public void showTargets(int targetSet, int videoState) {
    	//add by lgy 
    	if(InCallApp.isHbUI) {
    		return;
    	}
        final int targetResourceId;
        final int targetDescriptionsResourceId;
        final int directionDescriptionsResourceId;
        final int handleDrawableResourceId;
        mGlowpad.setVideoState(videoState);

        switch (targetSet) {
            case TARGET_SET_FOR_AUDIO_WITH_SMS:
                targetResourceId = R.array.incoming_call_widget_audio_with_sms_targets;
                targetDescriptionsResourceId =
                        R.array.incoming_call_widget_audio_with_sms_target_descriptions;
                directionDescriptionsResourceId =
                        R.array.incoming_call_widget_audio_with_sms_direction_descriptions;
                handleDrawableResourceId = R.drawable.ic_incall_audio_handle;
                break;
            case TARGET_SET_FOR_VIDEO_WITHOUT_SMS:
                targetResourceId = R.array.incoming_call_widget_video_without_sms_targets;
                targetDescriptionsResourceId =
                        R.array.incoming_call_widget_video_without_sms_target_descriptions;
                directionDescriptionsResourceId =
                        R.array.incoming_call_widget_video_without_sms_direction_descriptions;
                handleDrawableResourceId = R.drawable.ic_incall_video_handle;
                break;
            case TARGET_SET_FOR_VIDEO_WITH_SMS:
                targetResourceId = R.array.incoming_call_widget_video_with_sms_targets;
                targetDescriptionsResourceId =
                        R.array.incoming_call_widget_video_with_sms_target_descriptions;
                directionDescriptionsResourceId =
                        R.array.incoming_call_widget_video_with_sms_direction_descriptions;
                handleDrawableResourceId = R.drawable.ic_incall_video_handle;
                break;
            case TARGET_SET_FOR_VIDEO_ACCEPT_REJECT_REQUEST:
                targetResourceId =
                    R.array.incoming_call_widget_video_request_targets;
                targetDescriptionsResourceId =
                        R.array.incoming_call_widget_video_request_target_descriptions;
                directionDescriptionsResourceId = R.array
                        .incoming_call_widget_video_request_target_direction_descriptions;
                handleDrawableResourceId = R.drawable.ic_incall_video_handle;
                break;
            /**
             * M: [video call]3G Video call doesn't support answer as audio, and reject via SMS. @{
             */
            case TARGET_SET_FOR_VIDEO_WITHOUT_SMS_AUDIO:
                targetResourceId =
                        R.array.mtk_incoming_call_widget_video_without_sms_audio_targets;
                targetDescriptionsResourceId = R.array
                        .mtk_incoming_call_widget_video_without_sms_audio_target_descriptions;
                directionDescriptionsResourceId = R.array
                        .mtk_incoming_call_widget_video_without_sms_audio_direction_descriptions;
                handleDrawableResourceId = R.drawable.ic_incall_video_handle;
                break;
            /** @]*/
            case TARGET_SET_FOR_AUDIO_WITHOUT_SMS:
            default:
                targetResourceId = R.array.incoming_call_widget_audio_without_sms_targets;
                targetDescriptionsResourceId =
                        R.array.incoming_call_widget_audio_without_sms_target_descriptions;
                directionDescriptionsResourceId =
                        R.array.incoming_call_widget_audio_without_sms_direction_descriptions;
                handleDrawableResourceId = R.drawable.ic_incall_audio_handle;
                break;
        }

        if (targetResourceId != mGlowpad.getTargetResourceId()) {
            mGlowpad.setTargetResources(targetResourceId);
            mGlowpad.setTargetDescriptionsResourceId(targetDescriptionsResourceId);
            mGlowpad.setDirectionDescriptionsResourceId(directionDescriptionsResourceId);
            mGlowpad.setHandleDrawable(handleDrawableResourceId);
            mGlowpad.reset(false);
        }
    }

    @Override
    public void showMessageDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        mSmsResponsesAdapter = new ArrayAdapter<>(builder.getContext(),
                android.R.layout.simple_list_item_1, android.R.id.text1, mSmsResponses);

        final ListView lv = new ListView(getActivity());
        lv.setAdapter(mSmsResponsesAdapter);
        lv.setOnItemClickListener(new RespondViaSmsItemClickListener());
        //add by lgy
        builder.setNegativeButton(android.R.string.cancel, 
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mGlowpad != null) {
                    mGlowpad.startPing();
                }
                dismissCannedResponsePopup();
                getPresenter().onDismissDialog();
            }
        });

        builder.setCancelable(true).setView(lv).setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        if (mGlowpad != null) {
                            mGlowpad.startPing();
                        }
                        dismissCannedResponsePopup();
                        getPresenter().onDismissDialog();
                    }
                });
        mCannedResponsePopup = builder.create();
        mCannedResponsePopup.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        //add by lgy
        Window window = mCannedResponsePopup.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setWindowAnimations(R.style.mypopwindow_anim_style); 
        InCallApp.getInCallActivity().showBlurBg(true);
        
        mCannedResponsePopup.show();
        updateWhenRinging(window.getDecorView());
    }

    private boolean isCannedResponsePopupShowing() {
        if (mCannedResponsePopup != null) {
            return mCannedResponsePopup.isShowing();
        }
        return false;
    }

    private boolean isCustomMessagePopupShowing() {
        if (mCustomMessagePopup != null) {
            return mCustomMessagePopup.isShowing();
        }
        return false;
    }

    /**
     * Dismiss the canned response list popup.
     *
     * This is safe to call even if the popup is already dismissed, and even if you never called
     * showRespondViaSmsPopup() in the first place.
     */
    private void dismissCannedResponsePopup() {
        if (mCannedResponsePopup != null) {
            mCannedResponsePopup.dismiss();  // safe even if already dismissed
            mCannedResponsePopup = null;
            //add by lgy
            InCallApp.getInCallActivity().showBlurBg(false);
        }
    }

    /**
     * Dismiss the custom compose message popup.
     */
    private void dismissCustomMessagePopup() {
       if (mCustomMessagePopup != null) {
           mCustomMessagePopup.dismiss();
           mCustomMessagePopup = null;
       }
    }

    /// M: override dismissPendingDialogs in AnswerUi. @{
    @Override
    public void dismissPendingDialogs() {
    /// @}
        if (isCannedResponsePopupShowing()) {
            dismissCannedResponsePopup();
        }

        if (isCustomMessagePopupShowing()) {
            dismissCustomMessagePopup();
        }
    }

    public boolean hasPendingDialogs() {
        return !(mCannedResponsePopup == null && mCustomMessagePopup == null);
    }

    /**
     * Shows the custom message entry dialog.
     */
    public void showCustomMessageDialog() {
        // Create an alert dialog containing an EditText
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final EditText et = new EditText(builder.getContext());
        builder.setCancelable(true).setView(et)
                .setPositiveButton(R.string.custom_message_send,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // The order is arranged in a way that the popup will be destroyed when the
                        // InCallActivity is about to finish.
                        final String textMessage = et.getText().toString().trim();
                        dismissCustomMessagePopup();
                        getPresenter().rejectCallWithMessage(textMessage);
                    }
                })
                .setNegativeButton(R.string.custom_message_cancel,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /// M: ALPS01856137, need to start ping when back from dialog.
                        if (mGlowpad != null) {
                            mGlowpad.startPing();
                        }
                        /// @}
                        dismissCustomMessagePopup();
                        getPresenter().onDismissDialog();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        /// M: ALPS01856137, need to start ping when back from dialog.
                        if (mGlowpad != null) {
                            mGlowpad.startPing();
                        }
                        /// @}
                        dismissCustomMessagePopup();
                        getPresenter().onDismissDialog();
                    }
                })
                .setTitle(R.string.respond_via_sms_custom_message);
        mCustomMessagePopup = builder.create();

        // Enable/disable the send button based on whether there is a message in the EditText
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                /// M: For ALPS02035613. Asynchronous callback
                // mCustomMessagePopup maybe null. @{
                if (mCustomMessagePopup == null) {
                    Log.e(this, "afterTextChanged, mCustomMessagePopup is null");
                    return;
                }
                /// @}
                final Button sendButton = mCustomMessagePopup.getButton(
                        DialogInterface.BUTTON_POSITIVE);
                sendButton.setEnabled(s != null && s.toString().trim().length() != 0);
            }
        });

        // Keyboard up, show the dialog
        mCustomMessagePopup.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        mCustomMessagePopup.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        mCustomMessagePopup.show();

        // Send button starts out disabled
        final Button sendButton = mCustomMessagePopup.getButton(DialogInterface.BUTTON_POSITIVE);
        sendButton.setEnabled(false);
        
        updateWhenRinging(mCustomMessagePopup.getWindow().getDecorView());
    }

    @Override
    public void configureMessageDialog(List<String> textResponses) {
        mSmsResponses.clear();
        mSmsResponses.addAll(textResponses);
        //delete by lgy
//        mSmsResponses.add(getResources().getString(
//                R.string.respond_via_sms_custom_message));
        if (mSmsResponsesAdapter != null) {
            mSmsResponsesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void onAnswer(int videoState, Context context) {
        Log.d(this, "onAnswer videoState=" + videoState + " context=" + context);
        getPresenter().onAnswer(videoState, context);
    }

    @Override
    public void onDecline(Context context) {
        getPresenter().onDecline(context);
    }

    @Override
    public void onDeclineUpgradeRequest(Context context) {
        InCallPresenter.getInstance().declineUpgradeRequest(context);
    }

    @Override
    public void onText() {
        getPresenter().onText();
    }

    /**
     * OnItemClickListener for the "Respond via SMS" popup.
     */
    public class RespondViaSmsItemClickListener implements AdapterView.OnItemClickListener {

        /**
         * Handles the user selecting an item from the popup.
         */
        @Override
        public void onItemClick(AdapterView<?> parent,  // The ListView
                View view,  // The TextView that was clicked
                int position, long id) {
            Log.d(this, "RespondViaSmsItemClickListener.onItemClick(" + position + ")...");
            final String message = (String) parent.getItemAtPosition(position);
            Log.v(this, "- message: '" + message + "'");
            dismissCannedResponsePopup();

            // The "Custom" choice is a special case.
            // (For now, it's guaranteed to be the last item.)
            //modigy by lgy            
//            if (position == (parent.getCount() - 1)) {
//                // Show the custom message dialog
//                showCustomMessageDialog();
//            } else {
                getPresenter().rejectCallWithMessage(message);
//            }
        }
    }

    /// @}
    
    //add by lgy
    private ImageButton mSmsRject;
	private ObjectAnimator mArrowAnimator; 
	private ImageView mArrow;
	private View mNoLockContainer;
	private ImageButton mHangup, mAnswer;
	
	private void initHbViews(View parent){
		mSmsRject = (ImageButton) parent.findViewById(R.id.sms_reject);
		mSmsRject.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onText();
			}
		});
		
		mNoLockContainer = parent.findViewById(R.id.action_container);
		mHangup = (ImageButton) parent.findViewById(R.id.hangup);
		mHangup.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onDecline(getContext());
			}
		});
		
		mAnswer = (ImageButton) parent.findViewById(R.id.answer);
		mAnswer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onAnswer(VideoProfile.STATE_AUDIO_ONLY, getContext());
			}
		});
		
		mArrow = (ImageView) parent.findViewById(R.id.arrow);
		initArrowAnimator();		
//		mArrowAnimator.start();
		
		mScreenListener = new ScreenListener(getContext());
		mScreenListener.begin(new ScreenStateListener() {

			@Override
			public void onUserPresent() {
				Log.i("onUserPresent", "onUserPresent");
				updateLockState(false);
			}

			@Override
			public void onScreenOn() {
				Log.i("onScreenOn", "onScreenOn");
			}

			@Override
			public void onScreenOff() {
				Log.i("onScreenOff", "onScreenOff");
				updateLockState(true);
			}
		});
		
        KeyguardManager keyguardManager = (KeyguardManager) InCallApp.getInstance().getSystemService(Context.KEYGUARD_SERVICE);
        updateLockState(keyguardManager.isKeyguardLocked());

	}
	
	private void updateLockState(boolean lock) {
        if(lock) {
        	mNoLockContainer.setVisibility(View.GONE);
        	mGlowpad.setVisibility(View.VISIBLE);
	    } else {
        	mNoLockContainer.setVisibility(View.VISIBLE);
        	mGlowpad.setVisibility(View.GONE);
	    }
	}
	
	private void initArrowAnimator() {
	 	PropertyValuesHolder pvhy = PropertyValuesHolder.ofFloat("TranslationY",
 				0,  -26f);
     	PropertyValuesHolder pvha = PropertyValuesHolder.ofFloat("alpha",
 				0.1f,  0.6f);
     	mArrowAnimator = ObjectAnimator.ofPropertyValuesHolder(
 				mArrow, pvhy, pvha);
     	mArrowAnimator.setStartDelay(100);
     	mArrowAnimator.setDuration(600);
		mArrowAnimator.setInterpolator(new LinearInterpolator());
		mArrowAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
            	mArrow.setAlpha(0.3f);
            	mArrow.setTranslationY(0);
            }
        });
		mArrowAnimator.setRepeatCount(ValueAnimator.INFINITE);
	}
	
	private ScreenListener mScreenListener;
	    
	private void updateWhenRinging(View v) {
		v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
	}
}
