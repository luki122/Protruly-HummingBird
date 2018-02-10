/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import com.android.systemui.R;

class WifiIcons {
    //ShenQianfeng modify begin
    //Original:
    /*
    static final int[][] WIFI_SIGNAL_STRENGTH = {
            { R.drawable.stat_sys_wifi_signal_0,
              R.drawable.stat_sys_wifi_signal_1,
              R.drawable.stat_sys_wifi_signal_2,
              R.drawable.stat_sys_wifi_signal_3,
              R.drawable.stat_sys_wifi_signal_4 },
            { R.drawable.stat_sys_wifi_signal_0_fully,
              R.drawable.stat_sys_wifi_signal_1_fully,
              R.drawable.stat_sys_wifi_signal_2_fully,
              R.drawable.stat_sys_wifi_signal_3_fully,
              R.drawable.stat_sys_wifi_signal_4_fully }
        };
        */
    //Modify to:
    static final int[][] WIFI_SIGNAL_STRENGTH = {

        { R.drawable.hmb_wifi_signal_exclamation_0,
          R.drawable.hmb_wifi_signal_exclamation_1,
          R.drawable.hmb_wifi_signal_exclamation_2,
          R.drawable.hmb_wifi_signal_exclamation_3,
          R.drawable.hmb_wifi_signal_exclamation_4 },
        { R.drawable.hmb_wifi_signal_0,
          R.drawable.hmb_wifi_signal_1,
          R.drawable.hmb_wifi_signal_2,
          R.drawable.hmb_wifi_signal_3,
          R.drawable.hmb_wifi_signal_4 }
    };           
    //ShenQianfeng modify end
    
    static final int[][] QS_WIFI_SIGNAL_STRENGTH = {
            { R.drawable.ic_qs_wifi_0,
              R.drawable.ic_qs_wifi_1,
              R.drawable.ic_qs_wifi_2,
              R.drawable.ic_qs_wifi_3,
              R.drawable.ic_qs_wifi_4 },
            { R.drawable.ic_qs_wifi_full_0,
              R.drawable.ic_qs_wifi_full_1,
              R.drawable.ic_qs_wifi_full_2,
              R.drawable.ic_qs_wifi_full_3,
              R.drawable.ic_qs_wifi_full_4 }
        };

    static final int QS_WIFI_NO_NETWORK = R.drawable.ic_qs_wifi_no_network;
    static final int WIFI_NO_NETWORK = R.drawable.stat_sys_wifi_signal_null;

    static final int WIFI_LEVEL_COUNT = WIFI_SIGNAL_STRENGTH[0].length;

    /// M: [WIFI StatusBar Active Icon] add icons for feature @ {
    static final int[][] WIFI_SIGNAL_STRENGTH_INOUT = {
        
        //ShenQianfeng modify begin
        //Original:
        /*
        { R.drawable.stat_sys_wifi_signal_0_fully,
          R.drawable.stat_sys_wifi_signal_0_fully,
          R.drawable.stat_sys_wifi_signal_0_fully,
          R.drawable.stat_sys_wifi_signal_0_fully }, 

        { R.drawable.stat_sys_wifi_signal_1_fully,
          R.drawable.stat_sys_wifi_signal_1_fully_in,
          R.drawable.stat_sys_wifi_signal_1_fully_out,
          R.drawable.stat_sys_wifi_signal_1_fully_inout },

        { R.drawable.stat_sys_wifi_signal_2_fully,
          R.drawable.stat_sys_wifi_signal_2_fully_in,
          R.drawable.stat_sys_wifi_signal_2_fully_out,
          R.drawable.stat_sys_wifi_signal_2_fully_inout },

        { R.drawable.stat_sys_wifi_signal_3_fully,
          R.drawable.stat_sys_wifi_signal_3_fully_in,
          R.drawable.stat_sys_wifi_signal_3_fully_out,
          R.drawable.stat_sys_wifi_signal_3_fully_inout },

        { R.drawable.stat_sys_wifi_signal_4_fully,
          R.drawable.stat_sys_wifi_signal_4_fully_in,
          R.drawable.stat_sys_wifi_signal_4_fully_out,
          R.drawable.stat_sys_wifi_signal_4_fully_inout }
          */
        //Modify to:
        { R.drawable.hmb_wifi_signal_actvity_0,
            R.drawable.hmb_wifi_signal_actvity_0,
            R.drawable.hmb_wifi_signal_actvity_0,
            R.drawable.hmb_wifi_signal_actvity_0 }, 

       { R.drawable.hmb_wifi_signal_actvity_1_none,
        R.drawable.hmb_wifi_signal_actvity_1_in,
        R.drawable.hmb_wifi_signal_actvity_1_out,
        R.drawable.hmb_wifi_signal_actvity_1_inout },


        { R.drawable.hmb_wifi_signal_actvity_2_none,
          R.drawable.hmb_wifi_signal_actvity_2_in,
          R.drawable.hmb_wifi_signal_actvity_2_out,
          R.drawable.hmb_wifi_signal_actvity_2_inout },

       { R.drawable.hmb_wifi_signal_actvity_3_none,
          R.drawable.hmb_wifi_signal_actvity_3_in,
          R.drawable.hmb_wifi_signal_actvity_3_out,
          R.drawable.hmb_wifi_signal_actvity_3_inout },

          { R.drawable.hmb_wifi_signal_actvity_4_none,
              R.drawable.hmb_wifi_signal_actvity_4_in,
              R.drawable.hmb_wifi_signal_actvity_4_out,
              R.drawable.hmb_wifi_signal_actvity_4_inout },

        //ShenQianfeng modify end
          
    };
    /// @ }
}
