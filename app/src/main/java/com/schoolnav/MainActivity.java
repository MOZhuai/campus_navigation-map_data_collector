package com.schoolnav;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWEngineInitListener;
import com.baidu.mapapi.walknavi.adapter.IWRoutePlanListener;
import com.baidu.mapapi.walknavi.model.WalkRoutePlanError;
import com.baidu.mapapi.walknavi.params.WalkNaviLaunchParam;
import com.baidu.mapapi.walknavi.params.WalkRouteNodeInfo;
import com.schoolnav.constant.RequsetSetting;
import com.schoolnav.utils.TimeFormatUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private MapView mMapView;
    private BaiduMap mBaiduMap;
    /*导航起终点Marker，可拖动改变起终点的坐标*/
    private Marker mStartMarker;
    private Marker mEndMarker;

    private String bx;
    private String by;

    private LatLng startPt = null;
    private LatLng endPt = null;

    private LocationClient mLocationClient;

    private WalkNaviLaunchParam walkParam;

    private static boolean isPermissionRequested = false;

    private BitmapDescriptor bdStart = BitmapDescriptorFactory
            .fromResource(R.drawable.icon_start);
    private BitmapDescriptor bdEnd = BitmapDescriptorFactory
            .fromResource(R.drawable.icon_end);
    private MarkerOptions ooA = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
        mMapView = (MapView) findViewById(R.id.mapview);
        Log.e(TAG, "mylocation end, start load schedule");
        loadSchedule();
        initMapStatus();
        initMyLocation();
        // 加载考试时间表

        /*普通步行导航入口*/
        Button walkBtn = (Button) findViewById(R.id.btn_walknavi_normal);
        walkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                walkParam.extraNaviMode(0);
                startWalkNavi();
//                Intent intent = new Intent(MainActivity.this, ArActivity.class);
//                startActivity(intent);
            }
        });

        setEndAndStart();

    }

    public void loadSchedule(){
        // 本地数据读取
        Log.d(TAG, "SharedPreferences read.");
        SharedPreferences info = getSharedPreferences("examineeInfo", MODE_PRIVATE);
        String acc = info.getString("eid", "");
        // 请求
        String url = "http://" + RequsetSetting.IP + ":8080/mobileapp/examinfo/getByAcc/" + acc;
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
                String resInfo = response.body().string();
                Log.d(TAG, "onResponse: " + resInfo);
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "runOnUiThread!");
                        try {
                            // 解析json
                            JSONObject jo_extend = new JSONObject(resInfo);
                            String extend = jo_extend.getString("extend");
                            JSONObject jo_examineeSubjects = new JSONObject(extend);
                            JSONArray jo_info = jo_examineeSubjects.getJSONArray("examineeSubjects");
                            showSubject(jo_info);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });
    }

    public void showSubject(JSONArray jo) throws JSONException {
        Log.e(TAG, "showSubject!");
        for (int i = 0; i < jo.length(); i++) {
            JSONObject jsonObject = jo.getJSONObject(i);
            // 课程信息
            String subject = jsonObject.getString("s");
            Log.e(TAG, "subject:" + subject);
            JSONObject jo_subject = new JSONObject(subject);
            // 科目
            String name = jo_subject.getString("name");
            // 时间
            String startTime = jo_subject.getString("startTime");
            if(TimeFormatUtils.isToday(startTime) == 0){
                continue;
            }
            String endTime = jo_subject.getString("endTime");
            // 楼层信息
            String building = jsonObject.getString("b");
            JSONObject jo_builidng = new JSONObject(building);
            String bname = jo_builidng.getString("bname");
            // 教室信息
            int room = jsonObject.getInt("room");
            // 座位号
            int seat = jsonObject.getInt("seat");
            // 输出
            TextView nameTextView = new TextView(getApplicationContext());
            nameTextView.setText(name);
            nameTextView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT,1.5f));

            TextView startTimeTextView = new TextView(getApplicationContext());
            startTimeTextView.setText(TimeFormatUtils.formatDate(startTime));
            startTimeTextView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT,3f));

            TextView endTimeTextView = new TextView(getApplicationContext());
            endTimeTextView.setText(TimeFormatUtils.formatDate(endTime));
            endTimeTextView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT,3f));

            TextView buildingTextView = new TextView(getApplicationContext());
            buildingTextView.setText(bname + room);
            buildingTextView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT,2f));

            TextView seatTextView = new TextView(getApplicationContext());
            seatTextView.setText("" + seat);
            seatTextView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT,1f));

            TableRow tb = new TableRow(getApplicationContext());
            tb.addView(nameTextView);
            tb.addView(startTimeTextView);
            tb.addView(endTimeTextView);
            tb.addView(buildingTextView);
            tb.addView(seatTextView);

            TableLayout tl = (TableLayout)findViewById(R.id.subject_tl);
            tl.addView(tb);
        }
    }

    public void setEndAndStart(){
        // 本地数据读取
        SharedPreferences info = getSharedPreferences("examineeInfo", MODE_PRIVATE);
        String acc = info.getString("eid", "");

        String url = "http://" + RequsetSetting.IP + ":8080/mobileapp/examinfo/getNextSubject/" + acc;
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
                String resInfo = response.body().string();
                try {
                    // 解析json
                    JSONObject jo_extend = new JSONObject(resInfo);
                    Button lookdoc = (Button)findViewById(R.id.btn_walknavi_normal);
                    TextView msgNavi = (TextView)findViewById(R.id.msg_navi_normal);
                    if(jo_extend.getInt("code") == 100){
                        String extend = jo_extend.getString("extend");
                        JSONObject jo_nextEs = new JSONObject(extend);
                        String nextEs = jo_nextEs.getString("nextEs");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                try {
                                    JSONObject jo_subject = new JSONObject(nextEs);
                                    String subject = jo_subject.getString("s");
                                    JSONObject jo_sName = new JSONObject(subject);
                                    String sName = jo_sName.getString("name");
                                    msgNavi.setText("下一门考试科目：" + sName);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                lookdoc.setEnabled(true);
                            }
                        });
                        JSONObject jo_building = new JSONObject(nextEs);
                        String building = jo_building.getString("b");
                        JSONObject jo_info = new JSONObject(building);
                        String ssmap = jo_info.getString("ssmap");
                        String bname = jo_info.getString("bname");
                        String targetPosition = jo_nextEs.getString("position");

                        // 存储mapid,mapname
                        SharedPreferences mapInfo = getSharedPreferences("examineeInfo", MODE_PRIVATE);
                        SharedPreferences.Editor editor = mapInfo.edit();
                        editor.putString("mapId", ssmap);
                        editor.putString("mapName", bname);
                        editor.putString("targetPosition", targetPosition);
                        editor.commit();

                        bx = jo_info.getString("x");
                        by = jo_info.getString("y");

//                        startPt = new LatLng(30.320791,120.360952);
                        endPt = new LatLng(Double.parseDouble(by),Double.parseDouble(bx));
                        /*构造导航终点参数对象*/
//                        WalkRouteNodeInfo walkStartNode = new WalkRouteNodeInfo();
//                        walkStartNode.setLocation(startPt);
                        WalkRouteNodeInfo walkEndNode = new WalkRouteNodeInfo();
                        walkEndNode.setLocation(endPt);
                        walkParam = new WalkNaviLaunchParam().endNodeInfo(walkEndNode);

                        /* 初始化起终点Marker */
                        initOverlay();
                    } else {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                lookdoc.setEnabled(false);
                                msgNavi.setText("没有待考科目");
                            }
                        });
//                        initOverlay();
                        Log.e(TAG, "NO SUBJECT");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public class MyLocationListener extends BDAbstractLocationListener {
        private int mapStatusFlag = 0;
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null){
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection())
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .build();

            /**
             * 初始化导航起点Marker
             */
            if(null == startPt){
                startPt = new LatLng(location.getLatitude(), location.getLongitude());
                WalkRouteNodeInfo walkStartNode = new WalkRouteNodeInfo();
                walkStartNode.setLocation(startPt);
                ooA = new MarkerOptions().position(startPt).icon(bdStart)
                        .zIndex(9).draggable(true);
                mStartMarker = (Marker) (mBaiduMap.addOverlay(ooA));
                mStartMarker.setDraggable(true);
                walkParam.startNodeInfo(walkStartNode);
            }
            if(null != startPt && null != endPt && mapStatusFlag != 3){
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(new LatLng((startPt.latitude + endPt.latitude) * 0.5, (startPt.longitude + endPt.longitude) * 0.5)).zoom(19);
                mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                mapStatusFlag ++;
            }

            mBaiduMap.setMyLocationData(locData);
        }

    }

    /**
     * 初始化我的位置
     */
    public void initMyLocation(){
        //定位初始化
        mLocationClient = new LocationClient(this);

        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);

        //设置locationClientOption
        mLocationClient.setLocOption(option);
        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        mLocationClient.start();
    }

    /**
     * 初始化地图状态
     */
    private void initMapStatus(){
        mBaiduMap = mMapView.getMap();
        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(new LatLng(30.320409, 120.360754)).zoom(18);
        mBaiduMap.setMyLocationEnabled(true);
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }

    /**
     * 初始化导航起终点Marker
     */
    public void initOverlay() {

//        MarkerOptions ooA = new MarkerOptions().position(startPt).icon(bdStart)
//                .zIndex(9).draggable(true);
//        mStartMarker = (Marker) (mBaiduMap.addOverlay(ooA));
//        mStartMarker.setDraggable(true);

        MarkerOptions ooB = new MarkerOptions().position(endPt).icon(bdEnd)
                .zIndex(5);
        mEndMarker = (Marker) (mBaiduMap.addOverlay(ooB));
        mEndMarker.setDraggable(true);

        mBaiduMap.setOnMarkerDragListener(new BaiduMap.OnMarkerDragListener() {
            public void onMarkerDrag(Marker marker) {
            }

            public void onMarkerDragEnd(Marker marker) {
                if(marker == mStartMarker){
                    startPt = marker.getPosition();
                }else if(marker == mEndMarker){
                    endPt = marker.getPosition();
                }

                WalkRouteNodeInfo walkStartNode = new WalkRouteNodeInfo();
                walkStartNode.setLocation(startPt);
                WalkRouteNodeInfo walkEndNode = new WalkRouteNodeInfo();
                walkEndNode.setLocation(endPt);
                walkParam = new WalkNaviLaunchParam().startNodeInfo(walkStartNode).endNodeInfo(walkEndNode);

            }

            public void onMarkerDragStart(Marker marker) {
            }
        });
    }

    /**
     * 开始步行导航
     */
    private void startWalkNavi() {
        Log.d(TAG, "startWalkNavi");
        try {
            WalkNavigateHelper.getInstance().initNaviEngine(this, new IWEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    Log.d(TAG, "WalkNavi engineInitSuccess");
                    routePlanWithWalkParam();
                }

                @Override
                public void engineInitFail() {
                    Log.d(TAG, "WalkNavi engineInitFail");
                    WalkNavigateHelper.getInstance().unInitNaviEngine();
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "startBikeNavi Exception");
            e.printStackTrace();
        }
    }

    /**
     * 发起步行导航算路
     */
    private void routePlanWithWalkParam() {
        WalkNavigateHelper.getInstance().routePlanWithRouteNode(walkParam, new IWRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d(TAG, "WalkNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {

                Log.d(TAG, "onRoutePlanSuccess");

                // 算路成功, 跳转至步行导航界面
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, WNaviGuideActivity.class);
                startActivity(intent);

            }

            @Override
            public void onRoutePlanFail(WalkRoutePlanError error) {
                Log.d(TAG, "WalkNavi onRoutePlanFail");
            }

        });
    }

    /**
     * Android6.0之后需要动态申请权限
     */
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !isPermissionRequested) {

            isPermissionRequested = true;

            ArrayList<String> permissionsList = new ArrayList<>();

            String[] permissions = {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    Manifest.permission.WRITE_SETTINGS,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
            };

            for (String perm : permissions) {
                if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(perm)) {
                    permissionsList.add(perm);
                    // 进入到这里代表没有权限.
                }
            }

            if (permissionsList.isEmpty()) {
                return;
            } else {
                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), 0);
            }
        }
    }

    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 我的位置监听
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        // 地图数据
        mMapView.onDestroy();
        bdStart.recycle();
        bdEnd.recycle();
    }



}