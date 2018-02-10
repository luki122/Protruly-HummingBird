package com.protruly.powermanager.purebackground;

/**
 * Config for purebackground.
 */
public class Config {

	public static final long SCREEN_OFF_CLEAN_BG_TRIGGER_TIME = 3 * 60 * 1000;

	public static final long SCREEN_OFF_REPEAT_CLEAN_BG_TRIGGER_TIME = 5 * 60 * 1000;

	public static final long SCREEN_OFF_IDLE_FREEZE_TRIGGER_TIME = 10 * 60 * 1000;

	/**
	 * Auto start function is need to control the broadcast receive.
	 */
	public static final boolean isAutoStartControlReceive = true;

	/**
	 * AutoStart cache file
	 */
	public static final String cache_file_name_of_autoStart = "autoStartFile";


	/**
	 * 说明：在releaseObject()函数中，一般没有必要把一般的对象设置为null，
	 * 因为一般的对象即使设为null，也释放不了多少空间，反而会增加出现空指针的概率。
	 * 但是对于全局的context对象是一定要设置为null。经过验证，把context对象置null，可以释放百分之80的内存。
	 */
	public static final boolean SET_NULL_OF_CONTEXT = true;

	/**
	 * 自启动白名单
	 */
	public final static String[] autoStartWhiteList={
			"com.tencent.mm",
			"com.tencent.mobileqq",
			"com.immomo.momo",
			"com.sina.weibo",
			"com.ifreetalk.ftalk",
			"com.hy.minifetion",
			"com.zhihu.android",
			"com.p1.mobile.putong",
			"com.alibaba.android.rimet",

			"com.eg.android.AlipayGphone",

			"com.netease.mail",
			"com.tencent.androidqqmail",
			"cn.cj.pe",
			"com.corp21cn.mail189",
			"com.google.android.gm",
			"com.kingsoft.email",
			"com.asiainfo.android",
			"com.sina.mail",

			"sina.mobile.tianqitong",
			"com.moji.mjweather",
	};

	/**
	 * 自动清理白名单,名单内应用安装时默认不开启休眠功能
	 */
	public final static String[] autoCleanDefaultWhiteList ={
			//　社交类
			"com.tencent.mm",
			"com.tencent.mobileqq",
//			"com.immomo.momo",
//			"com.sina.weibo",
//			"com.ifreetalk.ftalk",
//			"cn.com.fetion",
//			"com.zhihu.android",
//			"com.p1.mobile.putong",
//			"com.alibaba.android.rimet",
//
//			// 支付类
//			"com.eg.android.AlipayGphone",
//
//			// 音乐类
//			"com.tencent.qqmusic",
//			"com.kugou.android",
//			"com.netease.cloudmusic",
//			"fm.xiami.main",
//			"cn.kuwo.player",
//			"com.ting.mp3.android",
//			"cmccwm.mobilemusic",

			// 购物类
//			"com.achievo.vipshop",
//			"com.taobao.taobao",
//			"com.jingdong.app.mall",
//			"com.tmall.wireless",
//			"com.suning.mobile.ebuy",
//			"com.gome.eshopnew",
//			"com.thestore.main",
//			"com.mogujie",
//			"cn.amazon.mShop.android",

			// FM
//			"fm.qingting.qtradio",
//			"com.ximalaya.ting.android",
//			"com.yibasan.lizhifm",
//			"cn.kuwo.tingshu",
//			"com.tencent.radio",
//			"com.itings.myradio",
//
//			// 办公类
//			"com.netease.mail",
//			"com.tencent.androidqqmail",
//			"cn.cj.pe",
//			"com.corp21cn.mail189",
//			"com.kingsoft.email",
//			"com.asiainfo.android",
//			"com.sina.mail",
//			"com.google.android.gm",
//
//			// 娱乐类
//			"com.hunantv.imgo.activity",
//			"com.youku.phone",
//			"com.qiyi.video",
//			"com.tencent.qqlive",
//			"com.letv.android.client",
//			"tv.danmaku.bili",
//			"com.sohu.sohuvideo",
//			"com.tudou.android",
//			"com.baidu.video",
//			"com.qihoo.video",
//			"com.duowan.mobile",
//			"air.tv.douyu.android",
//			"com.funshion.video.mobile",
//			"com.duowan.kiwi",
//
//			// 出行类
//			"com.autonavi.minimap",
//			"com.baidu.BaiduMap",
//			"com.tencent.map",
//			"com.sdu.didi.psnger",
//			"com.sogou.map.android.maps",
//			"com.sdu.didi.gsui",
//			"com.yongche.android",
//			"com.szzc.ucar.pilot",
//			"com.kuaidi.daijia.driver",
//
//			// 天气类
//			"sina.mobile.tianqitong",
//			"com.moji.mjweather",
	};

	/**
	 * 定位白名单
	 */
	public final static String[] locationWhiteList = {
			// 出行类
			"com.autonavi.minimap",
			"com.baidu.BaiduMap",
			"com.tencent.map",
			"com.sdu.didi.psnger",
			"com.sogou.map.android.maps",
			"com.sdu.didi.gsui",
			"com.szzc.ucar.pilot"
	};

	/**
	 * 系统自带黑名单
	 */
	public final static String[] blackAppList = {
			// system自带墨迹天气
			"com.moji.daling",
			// system自带QQ浏览器
			"com.android.browser"
	};

    /**
     * forbit alarm default list
     */
    public final static String[] forbitAlarmDefaultList={
            "com.sina.weibo",
            "com.autonavi.minimap",
            "com.tuniu.app.ui",
            "com.tencent.qqlive",
            "com.tencent.qqmusic",
            "com.bql.esshopping",
            "com.zhilianbao.leyaogo",
            "com.douwong.xdel",
            "cn.wps.moffice_eng"
    };
    
    
    /**
     * forbit alarm white list
     */
    public final static String[] forbitAlarmWhiteList={
            "com.tencent.mm",
            "com.tencent.mobileqq",
    };

}