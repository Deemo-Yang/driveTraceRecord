package com.example.drivetracerecord;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.SpatialRelationUtil;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.amap.api.track.AMapTrackClient;
import com.amap.api.track.query.entity.DriveMode;
import com.amap.api.track.query.entity.HistoryTrack;
import com.amap.api.track.query.entity.Point;
import com.amap.api.track.query.entity.Track;
import com.amap.api.track.query.model.HistoryTrackRequest;
import com.amap.api.track.query.model.HistoryTrackResponse;
import com.amap.api.track.query.model.QueryTerminalRequest;
import com.amap.api.track.query.model.QueryTerminalResponse;
import com.amap.api.track.query.model.QueryTrackRequest;
import com.amap.api.track.query.model.QueryTrackResponse;
import util.Constants;
import util.DbAdapter;
import util.SimpleOnTrackListener;
import util.PathRecord;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 轨迹查询示例
 * 1、演示如何查询终端最近上报的轨迹点
 * 2、演示如何查询终端最近上报的轨迹点及其所属轨迹信息，分别绘制出每条轨迹下的所有轨迹点
 */
public class TrackSearchActivity extends Activity {

    private AMapTrackClient aMapTrackClient;

    private TextureMapView textureMapView;
    private List<Polyline> polylines = new LinkedList<>();
    private List<Marker> endMarkers = new LinkedList<>();

    private long trackId;
    private List<LatLng> tracePoints = new LinkedList<>();
    private SmoothMoveMarker smoothMarker;

    private static final int START_STATUS=0;

    private static final int MOVE_STATUS=1;

    private static final int PAUSE_STATUS=2;
    private static final int FINISH_STATUS=3;

    private int mMarkerStatus=START_STATUS;

    private ImageView back_btn;
    private Button playBackbtn;

    private DbAdapter mDataBaseHelper;
    private List<PathRecord> mAllRecord = new ArrayList<>();
    private List<PathRecord> mRecordList;
    private int mRecordItemId;

    private TextView mDisplayDistance;
    private TextView mDisplayAveSpeed;
    private TextView mDisplayTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_search);
        aMapTrackClient = new AMapTrackClient(getApplicationContext());

        textureMapView = findViewById(R.id.activity_track_search_map);
        textureMapView.onCreate(savedInstanceState);

        mDisplayTime = findViewById(R.id.dis_all_time);
        mDisplayAveSpeed = findViewById(R.id.dis_ave_speed);
        mDisplayDistance = findViewById(R.id.dis_all_dis);

        back_btn = findViewById(R.id.back_btn);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(TrackSearchActivity.this, HomePage.class);
                startActivity(intent);
            }
        });

        playBackbtn = findViewById(R.id.play_back);
        playBackbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playBack();
            }
        });

        Intent recordIntent = getIntent();
        if (recordIntent != null) {
            trackId = recordIntent.getLongExtra("trackID",-1);
            mRecordItemId = recordIntent.getIntExtra("recordID", -1);
        }
        Log.d("TrackIDReceive", String.valueOf(trackId));

        mDataBaseHelper = new DbAdapter(this);
        mDataBaseHelper.open();
        searchAllRecordFromDB();
        mRecordList = mAllRecord;
        PathRecord item;
        item = mAllRecord.get(mRecordList.size()-mRecordItemId);
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        Double tDis = Double.parseDouble(item.getDistance())/1000;
        Double tAveSpeed = tDis/Double.parseDouble(item.getDuration());
        tAveSpeed = tAveSpeed*3600;
        mDisplayDistance.setText(String.valueOf(decimalFormat.format(tDis)));
        int tDuration,tHour,tMinteu,tSecond;

        tDuration = (int)Double.parseDouble(item.getDuration())/1;
        if (tDuration > 60 && tDuration < 3600) {
            tMinteu = tDuration/60;
            tSecond = tDuration%60;
            mDisplayTime.setText(tMinteu+ "分" + tSecond + "秒");
        }
        else if (tDuration >= 3600) {
            tHour = tDuration/3600;
            tMinteu = (tDuration - tHour * 3600)/60;
            tSecond = (tDuration - tHour * 3600)-tMinteu*60;
            mDisplayTime.setText(tHour+ "小时"+tMinteu+ "分"+tSecond+ "秒");
        }
        else {
            mDisplayTime.setText(tDuration + "秒");
        }
//		mDisplayTime.setText(item.getDuration() + "s");
        mDisplayAveSpeed.setText(decimalFormat.format(tAveSpeed) + "km/h");

        clearTracksOnMap();
        // 先查询terminal id，然后用terminal id查询轨迹
        // 查询符合条件的所有轨迹，并分别绘制
        aMapTrackClient.queryTerminal(new QueryTerminalRequest(Constants.SERVICE_ID, Constants.TERMINAL_NAME), new SimpleOnTrackListener() {
            @Override
            public void onQueryTerminalCallback(final QueryTerminalResponse queryTerminalResponse) {
                if (queryTerminalResponse.isSuccess()) {
                    if (queryTerminalResponse.isTerminalExist()) {
                        long tid = queryTerminalResponse.getTid();
                        // 搜索最近12小时以内上报的属于某个轨迹的轨迹点信息，散点上报不会包含在该查询结果中
                        QueryTrackRequest queryTrackRequest = new QueryTrackRequest(
                                Constants.SERVICE_ID,
                                tid,
                                trackId,    // 轨迹id，不指定，查询所有轨迹，注意分页仅在查询特定轨迹id时生效，查询所有轨迹时无法对轨迹点进行分页
                                System.currentTimeMillis() - 12 * 60 * 60 * 1000,
                                System.currentTimeMillis(),
                                0,      // 不启用去噪
                                1,   // 绑路
                                0,      // 不进行精度过滤
                                DriveMode.DRIVING,  // 当前仅支持驾车模式
                                1,     // 距离补偿
                                5000,   // 距离补偿，只有超过5km的点才启用距离补偿
                                1,  // 结果应该包含轨迹点信息
                                1,  // 返回第1页数据，但由于未指定轨迹，分页将失效
                                100    // 一页不超过100条
                        );
                        aMapTrackClient.queryTerminalTrack(queryTrackRequest, new SimpleOnTrackListener() {
                            @Override
                            public void onQueryTrackCallback(QueryTrackResponse queryTrackResponse) {
                                if (queryTrackResponse.isSuccess()) {
                                    List<Track> tracks =  queryTrackResponse.getTracks();
                                    if (tracks != null && !tracks.isEmpty()) {
                                        boolean allEmpty = true;
                                        for (Track track : tracks) {
                                            List<Point> points = track.getPoints();
                                            if (points != null && points.size() > 0) {
                                                allEmpty = false;
                                                drawTrackOnMap(points);
                                            }
                                        }
                                        if (allEmpty) {
                                            Toast.makeText(TrackSearchActivity.this,
                                                    "所有轨迹都无轨迹点，请尝试放宽过滤限制，如：关闭绑路模式", Toast.LENGTH_SHORT).show();
                                        } else {
                                            StringBuilder sb = new StringBuilder();
                                            sb.append("共查询到").append(tracks.size()).append("条轨迹，每条轨迹行驶距离分别为：");
                                            for (Track track : tracks) {
                                                sb.append(track.getDistance()).append("m,");
                                            }
                                            sb.deleteCharAt(sb.length() - 1);
                                            Toast.makeText(TrackSearchActivity.this, sb.toString(), Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(TrackSearchActivity.this, "未获取到轨迹", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(TrackSearchActivity.this, "查询历史轨迹失败，" + queryTrackResponse.getErrorMsg(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(TrackSearchActivity.this, "Terminal不存在", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showNetErrorHint(queryTerminalResponse.getErrorMsg());
                }
            }
        });

    }

    private void showNetErrorHint(String errorMsg) {
        Toast.makeText(this, "网络请求失败，错误原因: " + errorMsg, Toast.LENGTH_SHORT).show();
    }

    private void drawTrackOnMap(List<Point> points) {
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.width(40);
        polylineOptions.setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.grasp_trace_line));
        if (points.size() > 0) {
            // 起点
            Point p = points.get(0);
            LatLng latLng = new LatLng(p.getLat(), p.getLng());
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            endMarkers.add(textureMapView.getMap().addMarker(markerOptions));
        }
        if (points.size() > 1) {
            // 终点
            Point p = points.get(points.size() - 1);
            LatLng latLng = new LatLng(p.getLat(), p.getLng());
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            endMarkers.add(textureMapView.getMap().addMarker(markerOptions));
        }
        for (Point p : points) {
            LatLng latLng = new LatLng(p.getLat(), p.getLng());
            polylineOptions.add(latLng);
            tracePoints.add(latLng);
            boundsBuilder.include(latLng);
        }
        Polyline polyline = textureMapView.getMap().addPolyline(polylineOptions);
        polylines.add(polyline);
        textureMapView.getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 30));
    }

    private void clearTracksOnMap() {
        for (Polyline polyline : polylines) {
            polyline.remove();
        }
        for (Marker marker : endMarkers) {
            marker.remove();
        }
        endMarkers.clear();
        polylines.clear();
    }


    private void playBack() {
        LatLngBounds bounds = new LatLngBounds(tracePoints.get(0), tracePoints.get(tracePoints.size() - 2));
        textureMapView.getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
        smoothMarker = new SmoothMoveMarker(textureMapView.getMap());
        smoothMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.car));

        LatLng drivePoint = tracePoints.get(0);
        Pair<Integer, LatLng> pair = SpatialRelationUtil.calShortestDistancePoint(tracePoints, drivePoint);
        tracePoints.set(pair.first, drivePoint);
        List<LatLng> subList = tracePoints.subList(pair.first, tracePoints.size());

        smoothMarker.setPoints(subList);
        // 设置滑动的总时间
        smoothMarker.setTotalDuration(15);
        // 开始滑动
        switch (mMarkerStatus) {
            case 0: {
                smoothMarker.startSmoothMove();
                mMarkerStatus = MOVE_STATUS;
                playBackbtn.setText("暂停");break;
            }
            case 1: {
                smoothMarker.stopMove();
                mMarkerStatus = PAUSE_STATUS;
                playBackbtn.setText("继续");break;
            }
            case 2: {
                smoothMarker.startSmoothMove();
                mMarkerStatus = MOVE_STATUS;
                playBackbtn.setText("暂停");break;
            }
            case 3: {
                smoothMarker.setPoints(tracePoints);
                smoothMarker.startSmoothMove();
                mMarkerStatus = MOVE_STATUS;
                playBackbtn.setText("暂停");break;
            }
            default:break;
        }

        smoothMarker.setMoveListener(
                new SmoothMoveMarker.MoveListener() {
                    @Override
                    public void move(double distance) {
                        if (distance == 0 ) {
                            mMarkerStatus = FINISH_STATUS;
                            playBackbtn.setText("开始回放");
                        }
                    }
                }
        );


    }

    private void searchAllRecordFromDB() {
        mAllRecord = mDataBaseHelper.queryRecordAll();
    }

    @Override
    protected void onPause() {
        super.onPause();
        textureMapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        textureMapView.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        textureMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        textureMapView.onDestroy();
    }
}

