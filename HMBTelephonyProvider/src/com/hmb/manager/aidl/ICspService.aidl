package com.hmb.manager.aidl;
import com.hmb.manager.aidl.MarkResult;
import com.hmb.manager.aidl.RejectSmsResult;

interface ICspService {
    MarkResult getMark(int type, String number);
    String getTagName(int tagType);  
    RejectSmsResult canRejectSms(String number, String smscontent); 
    boolean canRejectSmsByKeyWord(String smscontent); 
    void updateMark(String type, String number);
}
