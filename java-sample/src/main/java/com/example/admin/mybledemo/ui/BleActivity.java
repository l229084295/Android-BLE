package com.example.admin.mybledemo.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.admin.mybledemo.BleRssiDevice;
import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.adapter.ScanAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleStatusCallback;
import cn.com.heaton.blelibrary.ble.model.ScanRecord;
import cn.com.heaton.blelibrary.ble.utils.Utils;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class BleActivity extends AppCompatActivity {
    private String TAG = BleActivity.class.getSimpleName();
    public static final int REQUEST_GPS = 4;
    public static final int REQUEST_CONNECT = 5;
    private LinearLayout llBlutoothAdapterTip;
    private TextView tvAdapterStates;
    private SwipeRefreshLayout swipeLayout;
    private FloatingActionButton floatingActionButton;
    private FloatingActionButton openUnitDoor;
    private RecyclerView recyclerView;
    private FilterView filterView;
    private ScanAdapter adapter;
    private List<BleRssiDevice> bleRssiDevices;
    private Ble<BleRssiDevice> ble = Ble.getInstance();
    private ObjectAnimator animator;
    private boolean isFilter = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        initView();
        initAdapter();
        initLinsenter();
        initBleStatus();
        requestPermission();

    }

    private void initAdapter() {
        bleRssiDevices = new ArrayList<>();
        adapter = new ScanAdapter(this, bleRssiDevices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.getItemAnimator().setChangeDuration(300);
        recyclerView.getItemAnimator().setMoveDuration(300);
        recyclerView.setAdapter(adapter);
    }

    private void initView() {
        llBlutoothAdapterTip = findViewById(R.id.ll_adapter_tip);
        swipeLayout = findViewById(R.id.swipeLayout);
        tvAdapterStates = findViewById(R.id.tv_adapter_states);
        recyclerView = findViewById(R.id.recyclerView);
        floatingActionButton = findViewById(R.id.floatingButton);
        openUnitDoor = findViewById(R.id.openUnitDoor);
        filterView = findViewById(R.id.filterView);
    }

    private void initLinsenter() {
        filterView.init(new FilterView.FilterListener() {
            @Override
            public void onAddressNameChanged(String addressOrName) {
                isFilter = true;
            }

            @Override
            public void onRssiChanged(int rssi) {
                isFilter = true;
            }

            @Override
            public void onCancel() {
                isFilter = false;
            }
        });
        tvAdapterStates.setOnClickListener(v -> {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(BleActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},REQUEST_CONNECT);
                    }
                }
                return;
            }
            startActivityForResult(enableBtIntent, Ble.REQUEST_ENABLE_BT);
        });
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rescan();
            }
        });
        openUnitDoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BleActivity.this, OpenDoorActivity.class);
                intent.putExtra("macAddress","21DCAA009469");
                startActivity(intent);
            }
        });
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeLayout.setRefreshing(false);
                rescan();
            }
        });

    }

    //请求权限
    public void requestPermission() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        XXPermissions.with(this)
                .permission(permissions)
                        .request(new OnPermissionCallback() {
                            @Override
                            public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                                if (allGranted) {
                                    checkBlueStatus();
                                }
                            }

                            @Override
                            public void onDenied(@NonNull List<String> permissions, boolean doNotAskAgain) {
                                OnPermissionCallback.super.onDenied(permissions, doNotAskAgain);
                                if (doNotAskAgain) {
                                    // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                } else {
                                }
                            }
                        });
    }

    //监听蓝牙开关状态
    private void initBleStatus() {
        ble.setBleStatusCallback(new BleStatusCallback() {
            @Override
            public void onBluetoothStatusChanged(boolean isOn) {
                BleLog.i(TAG, "onBluetoothStatusOn: 蓝牙是否打开>>>>:" + isOn);
                llBlutoothAdapterTip.setVisibility(isOn?View.GONE:View.VISIBLE);
                if (isOn){
                    checkGpsStatus();
                }else {
                    if (ble.isScanning()) {
                        ble.stopScan();
                    }
                }
            }
        });
    }

    //检查蓝牙是否支持及打开
    private void checkBlueStatus() {
        if (!ble.isSupportBle(this)) {
            com.example.admin.mybledemo.Utils.showToast(R.string.ble_not_supported);
            finish();
        }
        if (!ble.isBleEnable()) {
            llBlutoothAdapterTip.setVisibility(View.VISIBLE);
        }else {
            checkGpsStatus();
        }
    }

    private void checkGpsStatus(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Utils.isGpsOpen(BleActivity.this)){
            new AlertDialog.Builder(BleActivity.this)
                    .setTitle("提示")
                    .setMessage("为了更精确的扫描到Bluetooth LE设备,请打开GPS定位")
                    .setPositiveButton("确定", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent,REQUEST_GPS);
                    })
                    .setNegativeButton("取消", null)
                    .create()
                    .show();
        }else {
//            ble.startScan(scanCallback);
        }
    }

    private void rescan() {
        if (ble != null && !ble.isScanning()) {
            bleRssiDevices.clear();
            adapter.notifyDataSetChanged();
            ble.startScan(scanCallback);
        }
    }

    private BleScanCallback<BleRssiDevice> scanCallback = new BleScanCallback<BleRssiDevice>() {
        @Override
        public void onLeScan(final BleRssiDevice device, int rssi, byte[] scanRecord) {
            synchronized (ble.getLocker()) {
                for (int i = 0; i < bleRssiDevices.size(); i++) {
                    BleRssiDevice rssiDevice = bleRssiDevices.get(i);
                    if (TextUtils.equals(rssiDevice.getBleAddress(), device.getBleAddress())){
                        if (rssiDevice.getRssi() != rssi && System.currentTimeMillis()-rssiDevice.getRssiUpdateTime() >1000L){
                            rssiDevice.setRssiUpdateTime(System.currentTimeMillis());
                            rssiDevice.setRssi(rssi);
                            adapter.notifyItemChanged(i);
                        }
                        return;
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    device.setScanRecord(ScanRecord.parseFromBytes(scanRecord));
                }
                device.setRssi(rssi);
                bleRssiDevices.add(device);
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            startBannerLoadingAnim();
        }

        @Override
        public void onStop() {
            super.onStop();
            stopBannerLoadingAnim();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "onScanFailed: "+errorCode);
        }
    };

    public void startBannerLoadingAnim() {
        floatingActionButton.setImageResource(R.drawable.ic_loading);
        animator = ObjectAnimator.ofFloat(floatingActionButton, "rotation", 0, 360);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setDuration(800);
        animator.setInterpolator(new LinearInterpolator());
        animator.start();
    }

    public void stopBannerLoadingAnim() {
        floatingActionButton.setImageResource(R.drawable.ic_bluetooth_audio_black_24dp);
        if (animator!=null){
            animator.cancel();
        }
        floatingActionButton.setRotation(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_introduced:
                startActivity(new Intent(BleActivity.this, IntroducedActivity.class));
                break;
            case R.id.openDoor1:
                intent = new Intent(BleActivity.this, OpenDoorActivity.class);
                intent.putExtra("macAddress","FE19EC001407");
                startActivity(intent);
                break;
            case R.id.openDoor2:
                intent = new Intent(BleActivity.this, OpenDoorActivity.class);
                intent.putExtra("macAddress","21DCAA009469");
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == Ble.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
        } else if (requestCode == Ble.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            ble.startScan(scanCallback);
        }else if (requestCode == REQUEST_GPS){

        }else if (requestCode == REQUEST_CONNECT){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Ble.REQUEST_ENABLE_BT);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
