package com.hmb.manager.tms;

import android.app.Service;
import android.app.hb.CodeNameInfo;
import android.app.hb.ITMSManager;
import android.app.hb.ITrafficCorrectListener;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hmb.manager.netmanager.DataManagerManager;
import com.hmb.manager.netmanager.TrafficCorrectionWrapper;

import tmsdk.bg.module.network.ProfileInfo;
import tmsdk.common.ITMSApplicaionConfig;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.location.LocationManager;
import tmsdk.bg.module.network.ITrafficCorrectionListener;
import tmsdk.bg.module.network.CodeName;
/**
 * 
 * @author luolaigang
 *
 */
public class TmsService extends Service {
	private static final String TAG = "TmsService";

	ITMSManager.Stub stup = new ITMSManager.Stub() {
		
		@Override
		public String getLocation(String mPhoneNumber) throws RemoteException {
			Log.d(TAG, "getLocation() -> mPhoneNumber = "+mPhoneNumber);
			LocationManager mLocationManager = ManagerCreatorC.getManager(LocationManager.class);
			String location = mLocationManager.getLocation(mPhoneNumber);
			return location;
		}

		@Override
		public List<CodeNameInfo> getAllProvinces() throws RemoteException{
			ArrayList<CodeName> provinces = TrafficCorrectionWrapper.getInstance().getAllProvinces();
			return getCodeNameInfos(provinces);
		}

		@Override
		public List<CodeNameInfo> getCities(String provinceCode) throws RemoteException{
			return getCodeNameInfos(TrafficCorrectionWrapper.getInstance().getCities(provinceCode));
		}

		@Override
		public List<CodeNameInfo> getCarries() throws RemoteException{
			return getCodeNameInfos(TrafficCorrectionWrapper.getInstance().getCarries());
		}
		
		@Override
		public List<CodeNameInfo> getBrands(String carryId) throws RemoteException{
			return getCodeNameInfos(TrafficCorrectionWrapper.getInstance().getBrands(carryId));
		}

		@Override
		public int setConfig(int simIndex, String provinceId, String cityId, String carryId, String brandId, int closingDay)
				throws RemoteException{
			return TrafficCorrectionWrapper.getInstance().setConfig(simIndex, provinceId, cityId, carryId, brandId, closingDay);
		}

		@Override
		public int startCorrection(int simIndex) throws RemoteException {
			DataManagerManager.getInstance().mCorrectStates[simIndex] = true;
			return TrafficCorrectionWrapper.getInstance().startCorrection(simIndex);
		}

		@Override
		public int analysisSMS(int simIndex, String queryCode, String queryPort, String smsBody) throws RemoteException{
			return TrafficCorrectionWrapper.getInstance().analysisSMS(simIndex, queryCode, queryPort, smsBody);
		}

		@Override
		public int[] getTrafficInfo(int simIndex) throws RemoteException{
			int[] tranfic = TrafficCorrectionWrapper.getInstance().getTrafficInfo(simIndex);
			Log.d(TAG, "getTrafficInfo>>>>>> " + simIndex + ">tranfic>>>" + Arrays.toString(tranfic));
			return tranfic;
		}

		@Override
		public void trafficCorrectListener(final ITrafficCorrectListener listener) throws RemoteException{
			Log.d(TAG, "process id is>>>>>> " + android.os.Process.myPid());
			DataManagerManager.setDualPhoneInfoFetcher(DataManagerManager.getIMSI(TmsService.this, false));
			boolean mBresult = TMSDKContext.init(TmsService.this, TmsSecureService.class, new ITMSApplicaionConfig() {
				@Override
				public HashMap<String, String> config(
						Map<String, String> src) {
					HashMap<String, String> ret = new HashMap<String, String>(src);
					return ret;
				}
			});
			Log.v(TAG, "initTMSDK() -> spend =  + " + mBresult);
			TrafficCorrectionWrapper.getInstance().init(TmsService.this);
			TrafficCorrectionWrapper.getInstance().setTrafficCorrectionListener(new ITrafficCorrectionListener() {
				@Override
				public void onNeedSmsCorrection(int simIndex, String queryCode, String queryPort) {
					Log.e(TAG, "onNeedSmsCorrection--simIndex:[" + simIndex + "]--queryCode:[" + queryCode + "]queryPort:["
							+ queryPort + "]");
					DataManagerManager.getInstance().mCorrectStates[simIndex] = false;
					try {
						String code = DataManagerManager.getInstance().getOperatorCode(TmsService.this, simIndex);
						if (TextUtils.equals(code, queryPort)) {
							listener.onNeedSmsCorrection(simIndex, queryCode, queryPort);
						} else {
							if (DataManagerManager.getInstance().isNetworkAvailable(TmsService.this)) {
								TMSDKContext.onImsiChanged();
								Log.v(TAG, "startCorrection -> onImsiChanged =  + " + code);
							}
						}
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onTrafficInfoNotify(int simIndex, int trafficClass, int subClass, int kBytes) {
					Log.e(TAG, "onTrafficNotify->>" + simIndex + ">subClass>>" + subClass + ">kBytes>>" + kBytes);
					try {
						listener.onTrafficInfoNotify(simIndex, trafficClass, subClass, kBytes);
						DataManagerManager.getInstance().mCorrectStates[simIndex] = false;
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onError(int simIndex, int errorCode) {
					Log.e(TAG, "onError--errorCode:" + errorCode );
					DataManagerManager.getInstance().mCorrectStates[simIndex] = false;
					try {
						listener.onError(simIndex, errorCode);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onProfileNotify(int simIndex, ProfileInfo profileInfo) {
					super.onProfileNotify(simIndex, profileInfo);
					Log.e(TAG, "onProfileNotify>>>>>" + simIndex + ">>>>" + DataManagerManager.getInstance().mCorrectStates[simIndex]);
					try {
						if (DataManagerManager.getInstance().mCorrectStates[simIndex]) {
							startCorrection(simIndex);
						}
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onCorrectionResult(int simIndex, int retCode) {
					super.onCorrectionResult(simIndex, retCode);
					Log.e(TAG, "onCorrectionResult>>>>>" + simIndex + ">>>>" + retCode);
				}
			});
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return stup;
	}

	private List<CodeNameInfo> getCodeNameInfos(ArrayList<CodeName> codeNames) {

		List<CodeNameInfo> codeNameInfos = new ArrayList<CodeNameInfo>();
		if (codeNames == null) {
			return codeNameInfos;
		}
		for (int i = 0; i < codeNames.size(); i++) {
			CodeName codeName = codeNames.get(i);
			CodeNameInfo codeNameInfo = new CodeNameInfo(codeName.mCode, codeName.mName);
			codeNameInfos.add(codeNameInfo);
		}
		return codeNameInfos;
	}

}