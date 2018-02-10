package com.android.calendar.hb.legalholiday;

public class LegalHolidayResponse {

    private int retCode;
    private LegalHolidayResponseBody body;

    public int getRetCode() {
        return retCode;
    }

    public void setRetCode(int retCode) {
        this.retCode = retCode;
    }

    public LegalHolidayResponseBody getBody() {
        return body;
    }

    public void setBody(LegalHolidayResponseBody body) {
        this.body = body;
    }

}
