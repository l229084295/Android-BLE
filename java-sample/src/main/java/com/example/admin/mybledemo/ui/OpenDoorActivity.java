package com.example.admin.mybledemo.ui;

import static com.example.admin.mybledemo.ui.BleActivity.REQUEST_GPS;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.solver.widgets.analyzer.DependencyGraph;
import androidx.core.widget.NestedScrollView;

import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.SpanUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.example.admin.mybledemo.BleRssiDevice;
import com.example.admin.mybledemo.Constant;
import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.data.MyEvent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.model.ScanRecord;
import cn.com.heaton.blelibrary.ble.utils.Utils;

public class OpenDoorActivity extends AppCompatActivity {
    private final Ble<BleRssiDevice> ble = Ble.getInstance();
    private final List<BleRssiDevice> bleRssiDevices = new ArrayList<>();
    private ObjectAnimator animator;

    AutoCompleteTextView macEdit;
    TextView info;

    NestedScrollView scroll;

    private ActionBar actionBar;
    private FloatingActionButton floatingActionButton;

    String macAddress = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_door);
        initParams();
        initView();
        requestPermission();
    }

    private void initParams() {
        macAddress = getIntent().getStringExtra("macAddress");
    }

    private void initView() {
        scroll = findViewById(R.id.scroll);
        scroll.getViewTreeObserver().addOnGlobalLayoutListener(() -> scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN)));
        macEdit = findViewById(R.id.macEdit);
        macEdit.setThreshold(1);
        macEdit.setText(macAddress);
        macEdit.setAdapter(getArrayAdapter());
        macEdit.setOnItemClickListener((parent, view, position, id) -> {
            macAddress = macEdit.getText().toString();
        });
        info = findViewById(R.id.info);
        floatingActionButton = findViewById(R.id.floatingButton);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("开门");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rescan();
            }
        });
    }

    private void rescan() {
        if (ble != null && !ble.isScanning()) {
            addMessage("重新开始搜索蓝牙");
            bleRssiDevices.clear();
            ble.startScan(scanCallback);
        }
    }

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
        if (animator != null) {
            animator.cancel();
        }
        floatingActionButton.setRotation(0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ble.stopScan();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:// 点击返回图标事件
                this.finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

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
                            addMessage("蓝牙相关权限已获取",Color.GREEN);
                            checkBlueStatus();
                        }
                    }

                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean doNotAskAgain) {
                        OnPermissionCallback.super.onDenied(permissions, doNotAskAgain);
                        if (doNotAskAgain) {
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            addMessage("蓝牙相关权限被永久拒绝，请前往设置",Color.RED);
                        } else {
                        }
                    }
                });
    }

    private void checkBlueStatus() {
        if (!ble.isSupportBle(this)) {
            com.example.admin.mybledemo.Utils.showToast(R.string.ble_not_supported);
            finish();
        }
        if (!ble.isBleEnable()) {
        } else {
            checkGpsStatus();
        }
    }

    private void checkGpsStatus() {
        addMessage("检测GPS权限");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Utils.isGpsOpen(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("为了更精确的扫描到Bluetooth LE设备,请打开GPS定位")
                    .setPositiveButton("确定", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, REQUEST_GPS);
                    })
                    .setNegativeButton("取消", null)
                    .create()
                    .show();
        } else {
            addMessage("开始搜索蓝牙");
            ble.startScan(scanCallback);
        }
    }

    private final BleScanCallback<BleRssiDevice> scanCallback = new BleScanCallback<BleRssiDevice>() {
        @Override
        public void onLeScan(final BleRssiDevice device, int rssi, byte[] scanRecord) {
            synchronized (ble.getLocker()) {
                for (int i = 0; i < bleRssiDevices.size(); i++) {
                    BleRssiDevice rssiDevice = bleRssiDevices.get(i);
                    if (TextUtils.equals(rssiDevice.getBleAddress(), device.getBleAddress())) {
                        if (rssiDevice.getRssi() != rssi && System.currentTimeMillis() - rssiDevice.getRssiUpdateTime() > 1000L) {
                            rssiDevice.setRssiUpdateTime(System.currentTimeMillis());
                            rssiDevice.setRssi(rssi);
                        }
                        return;
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    device.setScanRecord(ScanRecord.parseFromBytes(scanRecord));
                }
                device.setRssi(rssi);
                addMessage("搜索到蓝牙设备，deviceName: " + device.getBleName() + ",mac: " + device.getBleAddress(), Color.parseColor("#999999"));
                if (com.example.admin.mybledemo.Utils.isMacAddress(device.getBleName())){
                    bleRssiDevices.add(device);
                }
                if (macAddress != null &&
                        macAddress.length() == 12 &&
                        com.example.admin.mybledemo.Utils.isMacAddress(macAddress)) {
                    addMacHistory(macAddress);
                    //开始开门
                    com.example.admin.mybledemo.Utils.openDoor(macAddress, bleRssiDevices);
                } else {
                    addMessage("mac地址不正确，开始自动匹配", Color.RED);
                    com.example.admin.mybledemo.Utils.openDoor(bleRssiDevices);
                }
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
            stopBannerLoadingAnim();
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MyEvent event) {
        addMessage(event.message, event.color, event.textSize);
    }

    private void addMessage(String message) {
        addMessage(message, getResources().getColor(R.color.colorPrimary), 14);
    }

    private void addMessage(String message, int color) {
        addMessage(message, color, 14);
    }

    private void addMessage(String message, int color, int textSize) {
        if (info == null) return;
        info.append(getSpan(TimeUtils.date2String(new Date(), "yyyy-dd-MM HH:mm:ss："), Color.parseColor("#333333"), 12));
        info.append("\n");
        info.append(getSpan(message, color, textSize));
        info.append("\n");
    }

    private void addMacHistory(String macAddress) {
        String macHistory = SPUtils.getInstance().getString(Constant.SP.MAC_ADDRESS_HISTORY);
        if (!macHistory.contains(macAddress.toUpperCase())) {
            macHistory += ",";
            macHistory += macAddress.toUpperCase();
            SPUtils.getInstance().put(Constant.SP.MAC_ADDRESS_HISTORY, macHistory);
        }
    }

    private ArrayAdapter<String> getArrayAdapter() {
        String macHistory = SPUtils.getInstance().getString(Constant.SP.MAC_ADDRESS_HISTORY);
        macHistory.split(",");
        return new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, macHistory.split(","));
    }

    private SpannableStringBuilder getSpan(String message, @ColorInt int color, int size) {
        SpanUtils s = new SpanUtils();
        s.append(message).setFontSize(size, true).setSpans(new ForegroundAlphaColorSpan(color));
        return s.create();
    }

    static class ForegroundAlphaColorSpan extends CharacterStyle implements UpdateAppearance {

        @ColorInt
        int mColor;

        public ForegroundAlphaColorSpan(int mColor) {
            this.mColor = mColor;
        }

        void setAlpha(int alpha) {
            mColor = Color.argb(alpha, Color.red(mColor), Color.green(mColor), Color.blue(mColor));
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            tp.setColor(mColor);
        }
    }
}
