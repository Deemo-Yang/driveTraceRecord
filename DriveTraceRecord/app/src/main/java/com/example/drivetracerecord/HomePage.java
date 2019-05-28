package com.example.drivetracerecord;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import util.DbAdapter;
import util.PathRecord;

public class HomePage extends AppCompatActivity implements View.OnClickListener{

    //UI Object
    private TextView txt_topbar;
    private TextView txt_channel;
    private TextView txt_message;
    private TextView txt_driveTimes;
    private FrameLayout ly_content;
    private List<String> permissionList = new ArrayList<>();

    private RecordAdapter mAdapter;
    private ListView mAllRecordListView;
    private DbAdapter mDataBaseHelper;
    private List<PathRecord> mAllRecord = new ArrayList<PathRecord>();
    private List<PathRecord> mRecordList;

    //Fragment Object
    private Fragment fg1,fg2;
    private FragmentManager fManager;

    private double totalDis;
    private int driveTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_home_page);
        getPermission();

        mDataBaseHelper = new DbAdapter(this);
        mDataBaseHelper.open();
        searchAllRecordFromDB();
        mRecordList = mAllRecord;
        driveTimes = mRecordList.size();
//        txt_driveTimes.setText("驾车 "+ String.valueOf(driveTimes) + " 次");

        PathRecord item;
        int i;
        int listSize = mRecordList.size();
        for (i = 0;i < mRecordList.size();i++){
            item = mRecordList.get(i);
            totalDis += Double.parseDouble(item.getDistance());
        }
        totalDis= totalDis/1000;
        fManager = getFragmentManager();
        bindViews();
        txt_channel.performClick();   //模拟一次点击，既进去后选择第一项




        Button beginDrive = findViewById(R.id.beginDrive);
        beginDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomePage.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void searchAllRecordFromDB() {
        mAllRecord = mDataBaseHelper.queryRecordAll();
    }

    /** 获取权限 */
    public void getPermission() {
        if (ContextCompat.checkSelfPermission(HomePage.this, Manifest.
                permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(HomePage.this, Manifest.
                permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission (HomePage.this, Manifest.
                permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            String [] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(HomePage.this, permissions, 1);
        } else {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String [] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "必须同意所有权限才能使用本程序",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    //UI组件初始化与事件绑定
    private void bindViews() {
        txt_topbar = findViewById(R.id.txt_topbar);
        txt_channel = findViewById(R.id.txt_channel);
        txt_message =  findViewById(R.id.txt_message);
        ly_content = findViewById(R.id.ly_content);

        txt_channel.setOnClickListener(this);
        txt_message.setOnClickListener(this);

    }

    //重置所有文本的选中状态
    private void setSelected(){
        txt_channel.setSelected(false);
        txt_message.setSelected(false);
    }

    //隐藏所有Fragment
    private void hideAllFragment(FragmentTransaction fragmentTransaction){
        if(fg1 != null)fragmentTransaction.hide(fg1);
        if(fg2 != null)fragmentTransaction.hide(fg2);
    }


    @Override
    public void onClick(View v) {
        FragmentTransaction fTransaction = fManager.beginTransaction();
        hideAllFragment(fTransaction);
        switch (v.getId()){
            case R.id.txt_channel:
                setSelected();
                txt_channel.setSelected(true);
                if(fg1 == null){
                    DecimalFormat decimalFormat = new DecimalFormat("0.00");
                    fg1 = MyFragment.newInstance(String.valueOf(decimalFormat.format(totalDis)) + "KM"+
                            "\n" + "共"+ driveTimes + "次");
                    fTransaction.add(R.id.ly_content,fg1);
                }else{
                    fTransaction.show(fg1);
                }
                break;
            case R.id.txt_message:
                setSelected();
                txt_message.setSelected(true);
                if(fg2 == null){
                    fg2 = new FragmentList();
                    fTransaction.add(R.id.ly_content,fg2);
                }else{
                    fTransaction.show(fg2);
                }
                break;
        }
        fTransaction.commit();
    }
}