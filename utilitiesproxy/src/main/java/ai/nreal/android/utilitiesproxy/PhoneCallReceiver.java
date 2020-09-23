package ai.nreal.android.utilitiesproxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Date;

// https://stackoverflow.com/questions/21990947/detect-the-end-of-an-answered-incoming-call-by-user-in-android-not-declined
public class PhoneCallReceiver extends BroadcastReceiver {

    private static final String TAG = PhoneCallReceiver.class.getSimpleName();
    private static final String ACTION_NEW_OUTGOING_CALL = "android.intent.action.NEW_OUTGOING_CALL";

    private static PhoneCallListener mPhoneCallListener;
    private static InternalPhoneStateListener mStateListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mStateListener == null) {
            mStateListener = new InternalPhoneStateListener();
        }

        if (intent.getAction().equals(ACTION_NEW_OUTGOING_CALL)) {
            mStateListener.setOutgoingNumber(intent.getExtras().getString(Intent.EXTRA_PHONE_NUMBER));
            return;
        }

        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephony.listen(mStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public static void setPhoneCallListener(PhoneCallListener listener) {
        mPhoneCallListener = listener;
    }

    public class InternalPhoneStateListener extends PhoneStateListener {

        private int mLastState = TelephonyManager.CALL_STATE_IDLE;

        private boolean mIsIncoming;
        private Date mCallStartTime;
        private String mCallNumber;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);

            if (mLastState == state) {
                return;
            }

            Log.d(TAG, "onCallStateChanged state = " + state + ", incomingNumber =" + incomingNumber);

            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    mIsIncoming = true;
                    mCallStartTime = new Date();
                    mCallNumber = incomingNumber;

                    if (mPhoneCallListener != null) {
                        mPhoneCallListener.onIncomingCallStart(incomingNumber, mCallStartTime.getTime());
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // Transition of ringing -> offhook are pickups of incoming calls.
                    if (mLastState != TelephonyManager.CALL_STATE_RINGING) {
                        mIsIncoming = false;
                        mCallStartTime = new Date();
                        if (mPhoneCallListener != null) {
                            mPhoneCallListener.onOutgoingCallStart(mCallNumber, mCallStartTime.getTime());
                        }
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    // Went to idle - this is the end of a call.
                    if (mLastState == TelephonyManager.CALL_STATE_RINGING) {
                        // Ring but no pickup - a miss
                        if (mPhoneCallListener != null) {
                            mPhoneCallListener.onMissCall(mCallNumber, mCallStartTime.getTime());
                        }
                    } else if (mIsIncoming) {
                        if (mPhoneCallListener != null) {
                            mPhoneCallListener.onIncomingCallEnd(mCallNumber, mCallStartTime.getTime(), new Date().getTime());
                        }
                    } else {
                        if (mPhoneCallListener != null) {
                            mPhoneCallListener.onOutgoingCallEnd(mCallNumber, mCallStartTime.getTime(), new Date().getTime());
                        }
                    }
                    break;
            }

            mLastState = state;
        }

        public void setOutgoingNumber(String number) {
            mCallNumber = number;
        }
    }
}
