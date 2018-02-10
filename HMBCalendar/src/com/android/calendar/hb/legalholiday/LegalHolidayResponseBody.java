package com.android.calendar.hb.legalholiday;

import java.util.List;

public class LegalHolidayResponseBody {

    private List<Integer> holidayList;
    private List<Integer> workdayList;

    public List<Integer> getHolidayList() {
        return holidayList;
    }

    public void setHolidayList(List<Integer> holidayList) {
        this.holidayList = holidayList;
    }

    public List<Integer> getWorkdayList() {
        return workdayList;
    }

    public void setWorkdayList(List<Integer> workdayList) {
        this.workdayList = workdayList;
    }

}
