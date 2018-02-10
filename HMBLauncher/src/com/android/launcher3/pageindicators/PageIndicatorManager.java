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

package com.android.launcher3.pageindicators;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.android.launcher3.Launcher;
import com.android.launcher3.Workspace;

import java.util.ArrayList;

public class PageIndicatorManager {

    private PageIndicatorDots mPageIndicatorDots;
    private PageIndicatorCube mPageIndicatorCube;

    public Launcher mLauncher;

    public Launcher getmLauncher() {
        return mLauncher;
    }

    private Workspace mWorkspace;
    protected int pageIndicatorCount;

    public static boolean ONDRAGING = false;

    public PageIndicatorManager(Workspace mWorkspace, Context context, PageIndicatorDots mPageIndicatorDots, PageIndicatorCube mPageIndicatorCube, int count) {
        this.mPageIndicatorDots = mPageIndicatorDots;
        this.mPageIndicatorCube = mPageIndicatorCube;
        mLauncher = (Launcher) context;
        this.mWorkspace = mWorkspace;
        pageIndicatorCount = count;

    }

    public void updateMarker(int index){
        if(mPageIndicatorDots != null){

        }
        if(mPageIndicatorCube != null){
            mPageIndicatorCube.updateMarker(index,mWorkspace.getPageIndicatorMarkerForCube(index));
        }
    }

    public void addMarker(int index){
        if(mPageIndicatorDots !=null){
            mPageIndicatorDots.addMarker();
        }
        if(mPageIndicatorCube != null){
            mPageIndicatorCube.addMarker(index,mWorkspace.getPageIndicatorMarkerForCube(index),true);
        }
    }

    public void addMarkerIfNeed(int index){
        if(mPageIndicatorCube != null){
            mPageIndicatorCube.addMarker(index,mWorkspace.getPageIndicatorMarkerForCube(index),true);
        }
    }
    public void removeMarker(int index){
        if(mPageIndicatorDots !=null){
            mPageIndicatorDots.removeMarker();
        }
        if(mPageIndicatorCube!= null){
            mPageIndicatorCube.removeMarker(index, true);
        }
    }

    public void reSetPageIndicatorDelay(){
        mWorkspace.postDelayed(new Runnable() {
            @Override
            public void run() {
//                reSetPageIndicator();
            }
        },520);
    }
//    public synchronized void reSetPageIndicator(){
//        if (mPageIndicatorDots == null || mPageIndicatorDots.mMarkers == null) {
//            mWorkspace.noNeedToAddPageIndicatorMaker = false;
//            mWorkspace.noNeedToRemovePageIndicatorMaker = false;
//            return;
//        }
//        int workspaceSize = mWorkspace.getPageCount();
//        int circleIndicatorSize = mPageIndicatorDots.mMarkers.size();
//        int cubeIndicatorSize = mPageIndicatorCube.mMarkers.size();
//        if(mPageIndicatorDots !=null && workspaceSize != circleIndicatorSize){
//            if(workspaceSize > circleIndicatorSize){
//                for(int i = 0 ; i < workspaceSize - circleIndicatorSize;i++){
//                    int index = circleIndicatorSize+i;
//                    mPageIndicatorDots.addMarker(index,mWorkspace.getPageIndicatorMarker(index),false);
//                }
//            }else if(workspaceSize < circleIndicatorSize){
//                for(int i = 0 ; i < circleIndicatorSize - workspaceSize;i++){
//                    int index = circleIndicatorSize-i-1;
//                    mPageIndicatorDots.removeMarker(index, false);
//                }
//            }
//        }
//        if(mPageIndicatorCube !=null && workspaceSize != cubeIndicatorSize){
//            if(workspaceSize > cubeIndicatorSize){
//                for(int i = 0 ; i < workspaceSize - cubeIndicatorSize;i++){
//                    int index = cubeIndicatorSize+i;
//                    mPageIndicatorCube.addMarker(index,mWorkspace.getPageIndicatorMarker(index),false);
//                }
//            }else if(workspaceSize < cubeIndicatorSize){
//                for(int i = 0 ; i < cubeIndicatorSize - workspaceSize;i++){
//                    int index = cubeIndicatorSize-i-1;
//                    mPageIndicatorCube.removeMarker(index, false);
//                }
//            }
//        }
//        mWorkspace.noNeedToAddPageIndicatorMaker = false;
//        mWorkspace.noNeedToRemovePageIndicatorMaker = false;
//    }

    public void indicatorsAttachedToWindow(){
        if(mPageIndicatorDots !=null && mPageIndicatorCube!= null){
            mPageIndicatorDots.setMarkersCount(pageIndicatorCount);

            ArrayList<PageIndicatorCube.PageMarkerResources> markersCube = new ArrayList<PageIndicatorCube.PageMarkerResources>();
            for (int i = 0; i < pageIndicatorCount; ++i) {
                markersCube.add(mWorkspace.getPageIndicatorMarkerForCube(i));
            }
            mPageIndicatorCube.addMarkers(markersCube, false);

            setOnClickListener();
        }
    }

    public void indicatorsDetachedToWindow(){
        mPageIndicatorDots = null;
        mPageIndicatorCube = null;
    }


    public void setOnClickListener(){
        if(mPageIndicatorDots !=null && mPageIndicatorCube!= null){
            AccessibilityManager am = (AccessibilityManager)
                    mLauncher.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (!am.isTouchExplorationEnabled()) {
                return ;
            }
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    mLauncher.showOverviewMode(true);
                }
            };
            mPageIndicatorDots.setOnClickListener(listener);
        }
    }

    public boolean initialized(){
        return (mPageIndicatorDots != null && mPageIndicatorCube != null);
    }

    public void clear(){
        if(mPageIndicatorCube!= null){
            mPageIndicatorCube.clear();
        }
    }

    public void showCubeIndicator(){
        if(!ONDRAGING && mPageIndicatorDots !=null && mPageIndicatorCube!= null) {
            mPageIndicatorDots.setOnTouchListener(null);
            mWorkspace.refreshviewCaches();
            ONDRAGING = true;
            mPageIndicatorCube.initLeftRightIndicator(false);
            if(mLauncher != null && mLauncher.getDragController()!= null && !mLauncher.getDragController().isContainDropTarget(mPageIndicatorCube)){
                mLauncher.getDragController().addDropTarget(mPageIndicatorCube);
            }
        }
    }
    public void hideCubeIndicator(){
        if(mPageIndicatorDots !=null && mPageIndicatorCube!= null) {

            mPageIndicatorDots.setOnTouchListener(mCircleIndicatorTouchListener);
            ONDRAGING = false;
            if(mLauncher != null && mLauncher.getDragController()!= null){
                mLauncher.getDragController().removeDropTarget(mPageIndicatorCube);
            }

            int workspaceSize = mWorkspace.getPageCount();
            int cubeIndicatorSize = mPageIndicatorCube.mMarkers.size();
            if(mPageIndicatorCube !=null && workspaceSize != cubeIndicatorSize){
                if(workspaceSize > cubeIndicatorSize){
                    for(int i = 0 ; i < workspaceSize - cubeIndicatorSize;i++){
                        int index = cubeIndicatorSize+i;
                        mPageIndicatorCube.addMarker(index,mWorkspace.getPageIndicatorMarkerForCube(index),false);
                    }
                }else if(workspaceSize < cubeIndicatorSize){
                    for(int i = 0 ; i < cubeIndicatorSize - workspaceSize;i++){
                        int index = cubeIndicatorSize-i-1;
                        mPageIndicatorCube.removeMarker(index, false);
                    }
                }
            }
        }
    }

    public void hideAllIndicators(){
        if(mPageIndicatorDots !=null && mPageIndicatorCube!= null) {
            mPageIndicatorDots.setVisibility(View.INVISIBLE);
            mPageIndicatorDots.setOnTouchListener(null);

            mPageIndicatorCube.setVisibility(View.INVISIBLE);
            ONDRAGING = false;
            if(mLauncher.getDragController() != null){
                mLauncher.getDragController().removeDropTarget(mPageIndicatorCube);
            }
        }
    }

    private final View.OnTouchListener mCircleIndicatorTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_MOVE: {
                    break;
                }
                case MotionEvent.ACTION_DOWN: {
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    break;
                }
            }
            return false;
        }
    };
}
