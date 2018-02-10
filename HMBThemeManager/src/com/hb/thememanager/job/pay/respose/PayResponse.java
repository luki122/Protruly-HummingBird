package com.hb.thememanager.job.pay.respose;

import com.hb.thememanager.http.response.Response;

public class PayResponse extends Response {
	public PayResponseBody body;

	@Override
	public String toString() {
		return "PayResponse [body=" + body + "]";
	}
}
