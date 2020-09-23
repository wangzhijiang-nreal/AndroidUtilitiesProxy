package ai.nreal.android.utilitiesproxy;

public interface PhoneCallListener {
    void onIncomingCallStart(String number, long startDate);
    void onOutgoingCallStart(String number, long startDate);
    void onIncomingCallEnd(String number, long startDate, long endDate);
    void onOutgoingCallEnd(String number, long startDate, long endDate);
    void onMissCall(String number, long startDate);
}

