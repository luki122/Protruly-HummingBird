package com.android.launcher3.theme;

import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

/**
 * Created by antino on 17-3-14.
 */
public interface IIconGetter {
    Drawable getIconDrawable(ResolveInfo info,UserHandle user);
    Drawable getIconDrawable(ActivityInfo info,UserHandle user);
    Drawable getIconDrawable(String pkg,UserHandle user);
    Bitmap getIcon(ResolveInfo info,UserHandle user);
    Bitmap getIcon(ActivityInfo info,UserHandle user);
    Bitmap getIcon(String pkg,UserHandle user);
    Bitmap normalizeIcons(Bitmap bitmap);
    Integer getColor(String name);
}
