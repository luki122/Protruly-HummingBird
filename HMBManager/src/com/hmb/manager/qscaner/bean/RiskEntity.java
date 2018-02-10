package com.hmb.manager.qscaner.bean;

import tmsdk.common.module.qscanner.QScanResultEntity;


public class RiskEntity {

    public static final int RISK_TYPE_APP = 0;
    public static final int RISK_TYPE_APK = 1;

    public int riskType;
    public String packageName;
    public String softName;
    public String version;
    public int versionCode;
    public String path;
    public int scanResult;
    public String virusName;
    public String virusDiscription;
    public String virusUrl;

    public RiskEntity() {
    }

    public RiskEntity(int type, QScanResultEntity entity) {
        riskType = type;
        packageName = entity.packageName;
        softName = entity.softName;
        version = entity.version;
        versionCode = entity.versionCode;
        path = entity.path;
        scanResult = entity.scanResult;
        virusName = entity.virusName;
        virusDiscription = entity.virusDiscription;
        virusUrl = entity.virusUrl;
    }
}