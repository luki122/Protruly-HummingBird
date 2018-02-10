package com.hb.interception.activity;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import hb.widget.ViewPager;

public class InterceptionViewPager extends ViewPager {

	private boolean scrollble = true;

	public InterceptionViewPager(Context context) {
		super(context);
	}

	public InterceptionViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void scrollTo(int x, int y) {
		if (scrollble) {
			super.scrollTo(x, y);
		}
	}

	public boolean isScrollble() {
		return scrollble;
	}

	public void setScrollble(boolean scrollble) {
		this.scrollble = scrollble;
	}
	
	private float xDistance, yDistance, xLast, yLast,mLeft;
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (!scrollble) {
			switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				xDistance = yDistance = 0f;
				xLast = ev.getX();
				yLast = ev.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				final float curX = ev.getX();
				final float curY = ev.getY();

				xDistance += Math.abs(curX - xLast);
				yDistance += Math.abs(curY - yLast);
				xLast = curX;
				yLast = curY;
				if (xDistance > yDistance) {
					return true;
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				break;
			}
		}
		return super.dispatchTouchEvent(ev);
	}
}
