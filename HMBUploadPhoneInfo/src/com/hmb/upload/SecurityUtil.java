package com.hmb.upload;

/**
 * 加密解密
 */
public class SecurityUtil {

	
	public static String myEnCode(String str) {
		String rstStr = "";
		if (str != null && str.length()>0) {
			int j = 0;
			char[] charArray = str.toCharArray();
			for (int i = 0; i < charArray.length; i++) {
				charArray[i] = (char) (charArray[i] ^ (666 + j));
				if(j++ > 10000){
					j = 0;
				}
			}
			rstStr = new String(charArray);
		}

		return rstStr;
	}
	/**z
	 * 字符串加密
	 * @param str 待加密字符串
	 * @return 加密后的字符串
	 */
	public static String encrypt(String str) {		
		return  myEnCode(str);
	}

	/**
	 * 字符串解密
	 * @param str 待解密字符串
	 * @return 解密后的字符串
	 */
	public static String decrypt(String str) {		
		return myEnCode(str);
	}
	
	

 
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//String str = "ˡʹ˸˸˭˼ʂʛʀ宄烬斉ʗʖʊʅʈˍ˟˄˔ˊʒʋʃʃʆʁʚʕ˕˝ʏʙʆʟʊʊ˳˵˷˷˲˰˰˴˼˼˼˿ˮˡˬʼʤʰʦʦʧ˷ˬ˥˨˩˶˹ʩʯʲ˽˚˃ʊʗʐʕ˜ˈˇʊʎʅ˂ʉʊʋ˞ʒʝʞ˛ʆʒʓ˖ʃʓʋ˞ˑ˜ʉͥͳͱ̤̪ͪͫͫ̽̈́ͥͥͿ͹ͫͽ̽͸Ͷͼ͸̷̵̡̻̪̮̬̩̳̮̖̗̜̗̗̘́̑̒̓̋͗";
		//String str="ˡʹ˾˲˺˦ʂʛ˙˞ʈʇ˂˂˞ˀˉˎ˥ˉʌʕʒ˘˟˖˝ʗʚʕ˕˖˞˞ːʟʄʝ˱˳˱ˡ˨˧ʵʾʻʽʯʦʚʨʼʼʹʾʼ˱ˮ˷ʷʹ˭˷˫˹˰˿ʨʺʒʒʋʌʊˇ˜˅ʞ˚˄˘˂˝ˌʒ";
		//  String str="ˡʹ˾˲˺˦ʂʛ˙˞ʈʇ˂˂˞ˀˉˎ˥ˉʌʕʒ˘˟˖˝ʗʚʕ˕˖˞˞ːʟʄʝ˱˳˱ˡ˨˧ʵʾʻʽʯʦʚʨʼʼʹʾʼ˱ˮ˷ʷʹ˭˷˫˹˰˿ʨʺʒʒʋʌʊˇ˜˅ʞ˚˄˘˂˝ˌʒ";//body无参数
//		  String str="ޒस़वयढभॠ॰ॱॸॵॲॳख़ॹॴॿहदिॽ॰्एुॖॆौॅउॉड़फ़ॄॄूृॊऒझऐॅ॑ेॅफ़ॗॗॹ॔क़क़जअॶॳॲ९०ळणवऻठथथंबणपॲ५॰॥ॺ१ॸ१ॺतइॷॾभिसअयगऎॆय़ॖचॄोघऎघमँऋक॓ैृउ";
//		  String stt="{\"version\":\"v3.3.0\",\"deviceId\":\"imei\",\"model\":\"123\",\"systemVersion\":\"an5.1\",\"body\":{\"pageNum\": 0,\"pageSize\":2}}";
//		String enStr0 = encrypt(str);
//		String deStr1 =decrypt(enStr0);
//		
//		System.out.println(" enStr0:" + enStr0);
//		System.out.println(" deStr1:" + deStr1);
	}

}
