package com.schoolnav;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.schoolnav.R;
import com.schoolnav.MainActivity;
import com.schoolnav.constant.RequsetSetting;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

public class ArActivity extends UnityPlayerActivity {

    private final static String TAG = ArActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        // Create the UnityPlayer
        mUnityPlayer = new UnityPlayer(this);

        // 设置IP
        UnityPlayer.UnitySendMessage("SceneMaster", //gameobject的名字
                "ASSetHttpIP", //调用方法的名字
                RequsetSetting.IP);

        FrameLayout layout = (FrameLayout) findViewById(R.id.ar_FrameLayout);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layout.addView(mUnityPlayer.getView(), 0, layoutParams);
    }

}