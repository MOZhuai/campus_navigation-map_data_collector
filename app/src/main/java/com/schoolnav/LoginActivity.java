package com.schoolnav;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.schoolnav.constant.RequsetSetting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private final static String TAG = LoginActivity.class.getSimpleName();

    private String resInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Button loginBtn = (Button) findViewById(R.id.login_btn);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView loginAcc = (TextView)findViewById(R.id.login_input_info);
                TextView showMsg = (TextView)findViewById(R.id.msg_show);
                //个人信息访问, 后台请求
                String acc = loginAcc.getText().toString();
                String url = "http://" + RequsetSetting.IP + ":8080/mobileapp/examinee/existAcc/" + acc;
                OkHttpClient okHttpClient = new OkHttpClient();
                final Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();
                Call call = okHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d(TAG, "onFailure: " + e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        resInfo = response.body().string();
                        Log.d(TAG, "onResponse: " + resInfo);

                        try {
                            // 解析json
                            JSONObject jo_extend = new JSONObject(resInfo);
                            if(jo_extend.getInt("code") == 100){
                                LoginActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        showMsg.setTextColor(0xff00ff00);
                                        showMsg.setText("登录成功");
                                    }
                                });
                                String extend = jo_extend.getString("extend");
                                JSONObject jo_examinee = new JSONObject(extend);
                                String examinee = jo_examinee.getString("examinee");
                                JSONObject jo_info = new JSONObject(examinee);
                                // 本地数据存储
                                Log.d(TAG, "SharedPreferences edit.");
                                SharedPreferences info = getSharedPreferences("examineeInfo", MODE_PRIVATE);
                                SharedPreferences.Editor editor = info.edit();
                                editor.putInt("id", jo_info.getInt("id"));
                                editor.putString("eid", jo_info.getString("eid"));
                                editor.putString("name", jo_info.getString("name"));
                                editor.putInt("gender", jo_info.getInt("gender"));
                                editor.commit();
                                // 跳转到主页面
                                Log.d(TAG, "intent -> MainActivity");
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                            } else {
                                LoginActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        showMsg.setTextColor(0xffff0000);
                                        showMsg.setText("用户不存在");
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });
    }
}