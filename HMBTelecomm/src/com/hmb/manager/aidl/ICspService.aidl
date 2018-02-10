package com.hmb.manager.aidl;
import com.hmb.manager.aidl.MarkResult;

interface ICspService {
    MarkResult getMark(int type, String number);
    String getTagName(int tagType);  
    boolean canRejectSms(String number, String smscontent); 
    boolean canRejectSmsByKeyWord(String smscontent); 
}
