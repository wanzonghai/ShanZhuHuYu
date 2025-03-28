package com.ad;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import com.ad.JSPluginUtil;
import com.ad.MsgTools;
import com.anythink.core.api.ATAdInfo;
import com.anythink.core.api.ATAdStatusInfo;
import com.anythink.core.api.ATNetworkConfirmInfo;
import com.anythink.core.api.ATShowConfig;
import com.anythink.core.api.AdError;
import com.anythink.rewardvideo.api.ATRewardVideoAd;
import com.anythink.rewardvideo.api.ATRewardVideoExListener;
import com.anythink.rewardvideo.api.ATRewardVideoListener;
import com.cocos.lib.CocosJavascriptJavaBridge;
import com.cocos.lib.CocosSensorHandler;
import com.cocos.lib.JsbBridgeWrapper;

import org.json.JSONObject;


public class RewardVideoHelper {
    private CocosSensorHandler mSensorHandler;
    private static final String TAG = RewardVideoHelper.class.getSimpleName();

    ATRewardVideoAd kfRewardVideoAd;
    String kfPlacementId;
    Activity kfActivity;
    JsbBridgeWrapper jbw;

    boolean isReady = false;

    public RewardVideoHelper() {
        MsgTools.printMsg(TAG + ": " + this);

        kfActivity = JSPluginUtil.getActivity();
        jbw = JsbBridgeWrapper.getInstance();
    }

    public void load(final String placementId) {
        this.initVideo(placementId);
    }

    private void initVideo(final String placementId) {
        MsgTools.printMsg("初始化广告");

        RewardVideoHelper rvh = this;
        kfPlacementId = placementId;

        kfRewardVideoAd = new ATRewardVideoAd(kfActivity, placementId);

        //设置广告监听
        kfRewardVideoAd.setAdListener(new ATRewardVideoListener() {
            @Override
            public void onRewardedVideoAdLoaded() {
            }

            @Override
            public void onRewardedVideoAdFailed(AdError adError) {
                //注意：禁止在此回调中执行广告的加载方法进行重试，否则会引起很多无用请求且可能会导致应用卡顿
                jbw.dispatchEventToScript("getRewardVideoFail","0");
                MsgTools.printMsg("激励视频广告加载失败err"+adError.toString());
            }

            @Override
            public void onRewardedVideoAdPlayStart(ATAdInfo adInfo) {
                //建议在此回调中调用load进行广告的加载，方便下一次广告的展示（不需要调用isAdReady()）
                MsgTools.printMsg("开始播放视频广告");
                kfRewardVideoAd.load();
                JSPluginUtil.pause();
            }

            @Override
            public void onRewardedVideoAdPlayEnd(ATAdInfo atAdInfo) {
//                rvh.resume();
            }

            @Override
            public void onRewardedVideoAdPlayFailed(AdError adError, ATAdInfo atAdInfo) {
                JSPluginUtil.resume();
                jbw.dispatchEventToScript("getRewardVideoFail","1");

            }

            @Override
            public void onRewardedVideoAdClosed(ATAdInfo atAdInfo) {
                JSPluginUtil.resume();
            }

            @Override
            public void onReward(ATAdInfo atAdInfo) {
                //建议在此回调中下发奖励
                JSPluginUtil.resume();
                MsgTools.printMsg("广告已经获得奖励了");
                jbw.dispatchEventToScript("getRewardVideo");
            }

            @Override
            public void onRewardedVideoAdPlayClicked(ATAdInfo atAdInfo) {
            }
        });

        kfRewardVideoAd.load();
    }


    public void showVideo(final String scenario) {
//        MsgTools.printMsg("显示广告");
        Log.d(TAG, "showVideo: 显示广告");
        if (this.isAdReady()) {
            ATShowConfig showConfig = new ATShowConfig.Builder()
                    .scenarioId(scenario)
                    .build();
            kfRewardVideoAd.show(kfActivity, showConfig);
        } else {
            Log.d(TAG, "showVideo: 没准备");
            kfRewardVideoAd.load();
        }
    }

    public boolean isAdReady() {
        MsgTools.printMsg("video isAdReady: " + kfPlacementId);

        try {
            if (kfRewardVideoAd != null) {
                boolean isAdReady = kfRewardVideoAd.isAdReady();
                MsgTools.printMsg("video isAdReady: " + kfPlacementId + ", " + isAdReady);
                return isAdReady;
            } else {
                MsgTools.printMsg("video isAdReady error, you must call loadRewardedVideo first " + kfPlacementId);
            }
            MsgTools.printMsg("video isAdReady, end: " + kfPlacementId);
        } catch (Throwable e) {
            MsgTools.printMsg("video isAdReady, Throwable: " + e.getMessage());
            return isReady;
        }
        return isReady;
    }

    public String checkAdStatus() {
        MsgTools.printMsg("video checkAdStatus: " + kfPlacementId);

        if (kfRewardVideoAd != null) {
            ATAdStatusInfo atAdStatusInfo = kfRewardVideoAd.checkAdStatus();
            boolean loading = atAdStatusInfo.isLoading();
            boolean ready = atAdStatusInfo.isReady();
            ATAdInfo atTopAdInfo = atAdStatusInfo.getATTopAdInfo();

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("isLoading", loading);
                jsonObject.put("isReady", ready);
                jsonObject.put("adInfo", atTopAdInfo);

                return jsonObject.toString();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return "";
    }


}
