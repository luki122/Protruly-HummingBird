package com.protruly.clouddata.appdata.listener;

import org.json.JSONObject;

public abstract interface OnlineConfigureListener
{
  public abstract void onCfgChanged(JSONObject paramJSONObject);
}

