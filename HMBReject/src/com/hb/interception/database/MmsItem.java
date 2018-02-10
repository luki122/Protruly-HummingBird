package com.hb.interception.database;

public class MmsItem {
	private String number;
	private long id;
	private int type;
	private long date;
	private String body;
	private long thread_id;
	private int read;
	private String rejectName;
	private int reject;
	private int slotid;
	public String getNumber() {
		return number;
	}
	public void setNumber(String number) {
		this.number = number;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public long getDate() {
		return date;
	}
	public void setDate(long date) {
		this.date = date;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public long getThread_id() {
		return thread_id;
	}
	public void setThread_id(long thread_id) {
		this.thread_id = thread_id;
	}
	public int getRead() {
		return read;
	}
	public void setRead(int read) {
		this.read = read;
	}
	public String getRejectName() {
		return rejectName;
	}
	public void setRejectName(String rejectName) {
		this.rejectName = rejectName;
	}
	public int getReject() {
		return reject;
	}
	public void setReject(int reject) {
		this.reject = reject;
	}
	public int getSlotid() {
		return slotid;
	}
	public void setSlotid(int slotid) {
		this.slotid = slotid;
	}
	

}
