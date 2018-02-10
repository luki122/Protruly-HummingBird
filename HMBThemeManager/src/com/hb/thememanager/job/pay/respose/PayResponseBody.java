package com.hb.thememanager.job.pay.respose;

public class PayResponseBody {
    public long qlcId;
    public int waresType;
    public String waresId;
    public int count;
    public long payTime;
    public int price;
    public String notifyUrl;
    public String orderId;
    
    public String merchants;	//商户号alipay
    public String publicKey;	//公钥alipay
    public String orderInfo;		//orderInfo alipay
    
    public String partnerid;		//商户号wechat
    public String prepayid;		//预支付交易会话标识wechat
    public String appid;			//appid wechat
    public String packageName; //扩展字段wecaht
    public String noncestr;		//随机字符串wechat
    public String timestamp;	//时间戳wechat
    public String sign;				//签名

	@Override
	public String toString() {
		return "PayResponseBody [qlcId=" + qlcId + ", waresType=" + waresType
				+ ", waresId=" + waresId + ", count=" + count + ", payTime="
				+ payTime + ", price=" + price + ", notifyUrl=" + notifyUrl
				+ ", orderId=" + orderId + ", merchants=" + merchants
				+ ", publicKey=" + publicKey + ", orderInfo=" + orderInfo
				+ ", partnerid=" + partnerid + ", prepayid=" + prepayid
				+ ", appid=" + appid + ", packageName=" + packageName
				+ ", noncestr=" + noncestr + ", timestamp=" + timestamp
				+ ", sign=" + sign + "]";
	}
}


