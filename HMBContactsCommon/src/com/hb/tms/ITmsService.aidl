package com.hb.tms;
import com.hb.tms.MarkResult;
import com.hb.tms.UsefulNumberResult;

interface ITmsService {
    String getArea(String number);
    void updateDatabaseIfNeed();
    MarkResult getMark(int type, String number);
    List<UsefulNumberResult> getUsefulNumber(String number);
}
