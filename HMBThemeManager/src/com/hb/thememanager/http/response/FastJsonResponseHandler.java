package com.hb.thememanager.http.response;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 *@des fastjson类型的回调接口
 * @param <T>
 */
public abstract class FastJsonResponseHandler<T> implements com.hb.thememanager.http.response.IResponseHandler {

    private Type mType;

    public FastJsonResponseHandler() {
        Type myclass = getClass().getGenericSuperclass();    //反射获取带泛型的class
        if (myclass instanceof Class) {
            throw new RuntimeException("Missing type parameter.");
        }
        ParameterizedType parameter = (ParameterizedType) myclass;      //获取所有泛型

//        mType = TypeUtils.unwrapOptional(parameter.getActualTypeArguments()[0]);    //将泛型转为type
        mType = parameter.getActualTypeArguments()[0];    //将泛型转为type
    }

    public final Type getType() {
        return mType;
    }

    public abstract void onSuccess(int statusCode, T response);


    @Override
    public void onProgress(long currentBytes, long totalBytes) {

    }
}