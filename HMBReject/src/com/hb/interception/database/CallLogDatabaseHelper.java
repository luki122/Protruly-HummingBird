package com.hb.interception.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hb.interception.util.BlackUtils;
import com.hb.interception.util.SimUtils;

import android.content.Context;
import android.database.Cursor;

public class CallLogDatabaseHelper {

	public static List<CallLogEntity> queryCallLogs(Cursor cursor,
			Context context) {
		Map<String, CallLogEntity> temMap = new HashMap<String, CallLogEntity>();
		String number;
		long id;
		int type;
		long date;
		long duration;
		String area;
		int reject;
		String name;
		String lable;
		int slotid;
		String mark; //标记
		String black_name; //黑名单备注
		int  user_mark; //-1是用户标记， 大于0是网络标记
		CallLogEntity callLogEntity = null;
		CallLogItem callLogItem = null;
		do {

			id = cursor.getLong(cursor.getColumnIndex("_id"));
			number = cursor.getString(cursor.getColumnIndex("number"))
					.replace("-", "").replace(" ", "");
			if (number.startsWith("+86")) {
				number = number.substring(3);
			}
			name = cursor.getString(cursor.getColumnIndex("name"));
			area = cursor.getString(cursor.getColumnIndex("geocoded_location"));
			date = cursor.getLong(cursor.getColumnIndex("date"));
			type = cursor.getInt(cursor.getColumnIndex("type"));
			duration = cursor.getLong(cursor.getColumnIndex("duration"));
			reject = cursor.getInt(cursor.getColumnIndex("reject"));
			mark = cursor.getString(cursor.getColumnIndex("mark"));
			black_name = cursor.getString(cursor.getColumnIndex("black_name"));
			user_mark = cursor.getInt(cursor.getColumnIndex("user_mark"));
		    int subid = Integer.valueOf(cursor.getInt(cursor.getColumnIndex("sub_id")));

			slotid = SimUtils.getSlotbyId(context, subid);
			callLogEntity = temMap.get(number);
			if (callLogEntity == null) {
				callLogEntity = new CallLogEntity();				
			    callLogEntity.setName(name);				
				callLogEntity.setSlotId(slotid);
				callLogEntity.setLastCallDate(date);
				callLogEntity.setDBPhomeNumber(number);
				callLogEntity.setArea(area);
				callLogEntity.setReject(reject);
				callLogEntity.setMark(mark);
				callLogEntity.setBlackName(black_name);
				callLogEntity.setUserMark(user_mark);			
				temMap.put(number, callLogEntity);
			}
			callLogItem = new CallLogItem();
			callLogItem.setId(id);
			callLogItem.setmType(type);
			callLogItem.setCallTime(date);
			callLogItem.setDuratation(duration);
			callLogEntity.addCallLogItem(callLogItem);

		} while (cursor.moveToNext());
		cursor.close();
		List<CallLogEntity> callLogEntities = new ArrayList<CallLogEntity>(
				temMap.values());
		Collections.sort(callLogEntities);
		return callLogEntities;
	}

}
