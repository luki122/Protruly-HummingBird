/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.dialer.calllog;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.dialer.R;

/**
 * Encapsulates the views that are used to display the details of a phone call in the call log.
 */
public final class PhoneCallDetailsViews {
    public final TextView nameView;
    public final TextView numberTextView;
    public final TextView datetime;
    public final ImageView sim_icon;
    public final View devider;
    
    private PhoneCallDetailsViews(
    		TextView nameView, 
            TextView numberTextView,
            ImageView sim_icon,
            TextView datetime,
            View devider) {
        this.nameView = nameView;
        this.numberTextView=numberTextView;
        this.sim_icon=sim_icon;
        this.datetime=datetime;
        this.devider=devider;
    }

    /**
     * Create a new instance by extracting the elements from the given view.
     * <p>
     * The view should contain three text views with identifiers {@code R.id.name},
     * {@code R.id.date}, and {@code R.id.number}, and a linear layout with identifier
     * {@code R.id.call_types}.
     */
    public static PhoneCallDetailsViews fromView(View view) {
    	return new PhoneCallDetailsViews(
    			(TextView) view.findViewById(R.id.name),
                (TextView) view.findViewById(R.id.number),
                (ImageView) view.findViewById(R.id.sim_icon),
                (TextView) view.findViewById(R.id.datetime),
                view.findViewById(R.id.devider));
    }

    public static PhoneCallDetailsViews createForTest(Context context) {
    	return new PhoneCallDetailsViews(
                new TextView(context),
                new TextView(context),
                new ImageView(context),
                new TextView(context),
                new View(context));
    }
}
