package com.example.admin.mybledemo;

import static com.blankj.utilcode.util.ThreadUtils.runOnUiThread;
import static com.blankj.utilcode.util.ThreadUtils.runOnUiThreadDelayed;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.EncryptUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.example.admin.mybledemo.data.MyEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotifyCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.utils.ByteUtils;
import cn.com.heaton.blelibrary.ble.utils.ThreadUtils;
import cn.com.heaton.blelibrary.ble.utils.UuidUtils;

public class Utils {

    private static Toast mToast;

    public static void showToast(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(MyApplication.getInstance(), text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    public static void showToast(int paramInt) {
        if (mToast == null) {
            mToast = Toast.makeText(MyApplication.getInstance(), paramInt, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(paramInt);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    public static String getUuid(String uuid128) {
        if (UuidUtils.isBaseUUID(uuid128)) {
            return "UUID: 0x" + UuidUtils.uuid128To16(uuid128, true);
        }
        return uuid128;
    }

    public static String getUuid(String uuid128, boolean lower_case) {
        if (UuidUtils.isBaseUUID(uuid128)) {
            return "UUID: 0x" + UuidUtils.uuid128To16(uuid128, lower_case);
        }
        return uuid128;
    }

    public static int dp2px(float dpValue) {
        final float scale = MyApplication.getInstance().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static void shareAPK(Activity activity) {
        PackageInfo packageInfo = getPackageInfo(activity);
        if (packageInfo != null) {
            File apkFile = new File(packageInfo.applicationInfo.sourceDir);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_STREAM, FileProvider7.getUriForFile(activity, apkFile));
            activity.startActivity(intent);
        }
    }

    private static PackageInfo getPackageInfo(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = null;
        try {
            String packageName = getPackageName(context);
            packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return packageInfo;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getPackageName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            return packageInfo.packageName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 拷贝OTA升级文件到SD卡
     */
    public static void copyOtaFile(final Context context, final String path) {
        //判断是否存在ota文件
        if (SPUtils.get(context, Constant.SP.OTA_FILE_EXIST, false)) return;
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
        File newFile = new File(path + Constant.Constance.OTA_FILE_PATH);
        copyFileToSD(context, Constant.Constance.OTA_FILE_PATH, newFile.getAbsolutePath());
        SPUtils.put(context, Constant.SP.OTA_FILE_EXIST, true);
    }

    private static void copyFileToSD(Context context, String assetPath, String strOutFileName) {
        try {
            InputStream myInput;
            OutputStream myOutput = new FileOutputStream(strOutFileName);
            myInput = context.getAssets().open(assetPath);
            byte[] buffer = new byte[1024];
            int length = myInput.read(buffer);
            while (length > 0) {
                myOutput.write(buffer, 0, length);
                length = myInput.read(buffer);
            }
            myOutput.flush();
            myInput.close();
            myOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class SPUtils {

        /**
         * 保存在手机里面的文件名
         */
        public static final String FILE_NAME = "share_data";

        /**
         * 保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法
         *
         * @param context
         * @param key
         * @param object
         */
        public static void put(Context context, String key, Object object) {
            SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            if (object instanceof String) {
                editor.putString(key, (String) object);
            } else if (object instanceof Integer) {
                editor.putInt(key, (Integer) object);
            } else if (object instanceof Boolean) {
                editor.putBoolean(key, (Boolean) object);
            } else if (object instanceof Float) {
                editor.putFloat(key, (Float) object);
            } else if (object instanceof Long) {
                editor.putLong(key, (Long) object);
            } else {
                editor.putString(key, object == null ? null : String.valueOf(object));
            }
            SharedPreferencesCompat.apply(editor);
        }

        /**
         * 得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值
         *
         * @param context
         * @param key
         * @param defaultObject
         * @return
         */
        public static <T> T get(Context context, String key, T defaultObject) {
            SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
            if (defaultObject instanceof String) {
                return (T) sp.getString(key, (String) defaultObject);
            } else if (defaultObject instanceof Integer) {
                return (T) Integer.valueOf(sp.getInt(key, (Integer) defaultObject));
            } else if (defaultObject instanceof Boolean) {
                return (T) Boolean.valueOf(sp.getBoolean(key, (Boolean) defaultObject));
            } else if (defaultObject instanceof Float) {
                return (T) Float.valueOf(sp.getFloat(key, (Float) defaultObject));
            } else if (defaultObject instanceof Long) {
                return (T) Long.valueOf(sp.getLong(key, (Long) defaultObject));
            } else {
                return (T) sp.getString(key, (String) defaultObject);
            }
        }

        /**
         * 创建一个解决SharedPreferencesCompat.apply方法的一个兼容类
         *
         * @author zhy
         */
        private static class SharedPreferencesCompat {
            private static final Method sApplyMethod = findApplyMethod();

            /**
             * 反射查找apply的方法
             *
             * @return
             */
            @SuppressWarnings({"unchecked", "rawtypes"})
            private static Method findApplyMethod() {
                try {
                    Class clz = SharedPreferences.Editor.class;
                    return clz.getMethod("apply");
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                return null;
            }

            /**
             * 如果找到则使用apply执行，否则使用commit
             *
             * @param editor
             */
            public static void apply(SharedPreferences.Editor editor) {
                try {
                    if (sApplyMethod != null) {
                        sApplyMethod.invoke(editor);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                editor.commit();
            }
        }
    }

    public static boolean isMacAddress(String macAddress) {
        if (macAddress == null) return false;
        return macAddress.toLowerCase().matches("([0-9a-f]{12})");
    }

    private static final String TAG = "createOpenDoorData";
    private static byte[] data = new byte[]{};

    /**
     * 生成开门数据
     *
     * @param macAddress
     * @return
     */
    public static String createOpenDoorData(String macAddress) {
        String data = "";
        String y, mon, d, h, m, timeStr;
        Calendar c = Calendar.getInstance();
        int year, month, day, hours, minutes;
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH) + 1;
        day = c.get(Calendar.DAY_OF_MONTH);
        hours = c.get(Calendar.HOUR_OF_DAY);
        minutes = c.get(Calendar.MINUTE);
        y = String.valueOf(year);
        mon = time_0(month);
        d = time_0(day);
        h = time_0(hours);
        m = time_0(minutes);
        Log.d(TAG, "year:" + year + ",mon:" + mon + ",day:" + d + ",hour:" + h + ",minute:" + m);
        timeStr = y.substring(2) + mon + d + h + m;
        String key = macAddress + timeStr + "55AA5A5AA5";
        String content = timeStr + macAddress.substring(6);
        String param = "30" + cryptByDes(key, content).substring(0, 16) + timeStr + "FA34DD0001";
        int crc = 0;
        for (int i = 0; i < param.length() - 1; i += 2) {
            crc ^= Integer.parseInt(param.substring(i, i + 2), 16);
        }
        data = param + Integer.toString(crc, 16);
        Log.d(TAG, "content:" + content);
        Log.d(TAG, "key:" + key);
        Log.d(TAG, "data:" + data);
        sendOpenDoorMessage("content:" + content,Color.BLACK);
        sendOpenDoorMessage("key:" + key,Color.BLACK);
        sendOpenDoorMessage("data:" + data,Color.BLACK);
        return data.toUpperCase();
    }

    private static String time_0(int timeValue) {
        return timeValue > 10 ? String.valueOf(timeValue) : "0" + timeValue;
    }

    // key:0000000000 + mac 后六位
    private static String cryptByDes(String key, String content) {
        String crypt = EncryptUtils.encrypt3DES2HexString(ByteUtils.hexStr2Bytes(content), ByteUtils.hexStr2Bytes(key), "DESede/ECB/PKCS5Padding", null);
        Log.d("crypt", crypt);
        return crypt;
    }

    public static void openDoor(String macAddress, List<BleRssiDevice> devices) {
        try {
            for (BleRssiDevice d : devices) {
                if (d.getBleName() != null && d.getBleName().equals(macAddress)) {
                    sendOpenDoorMessage("命中目标，开始开锁", Color.GREEN);
                    data = ByteUtils.hexStr2Bytes(createOpenDoorData(macAddress));
                    Ble<BleDevice> ble = Ble.getInstance();
                    if (ble.isScanning()) {
                        ble.stopScan();
                    }
                    ble.connect(d, connectCallback);
                    sendOpenDoorMessage("开始连接蓝牙：" + d.getBleName(), Color.GREEN);
                }
            }
        } catch (Exception e) {
            sendOpenDoorMessage("openDoor step1:" + Log.getStackTraceString(e), Color.RED);
            e.printStackTrace();
        }
    }

    private static final BleConnectCallback<BleDevice> connectCallback = new BleConnectCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(BleDevice device) {
            sendOpenDoorMessage("onConnectionChanged: " + device.getConnectionState() + Thread.currentThread().getName());
            Log.e(TAG, "onConnectionChanged: " + device.getConnectionState() + Thread.currentThread().getName());
        }

        @Override
        public void onConnectFailed(BleDevice device, int errorCode) {
            super.onConnectFailed(device, errorCode);
            sendOpenDoorMessage("连接异常，异常状态码:" + errorCode, Color.RED);
            Utils.showToast("连接异常，异常状态码:" + errorCode);
        }

        @Override
        public void onConnectCancel(BleDevice device) {
            super.onConnectCancel(device);
            Log.e(TAG, "onConnectCancel: " + device.getBleName());
        }

        @Override
        public void onServicesDiscovered(BleDevice device, BluetoothGatt gatt) {
            super.onServicesDiscovered(device, gatt);
            try {
                for (BluetoothGattService g : gatt.getServices()) {//轮询蓝牙下的服务
                    String uuid = g.getUuid().toString().toUpperCase();
                    Log.d(TAG, "uuid:" + uuid);
                    sendOpenDoorMessage("轮询蓝牙服务uuid：" + uuid, Color.parseColor("#f43e06"));
                    sendOpenDoorMessage("=>服务类型：" + g.getType() + ",uuid是否包含FFF0:" + (uuid.contains("FFF0")), Color.parseColor("#f43e06"));
                    if (g.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY && uuid.contains("FFF0")) {
                        sendOpenDoorMessage("==>轮询写入特征值，特征值数量：" + g.getCharacteristics().size(), Color.parseColor("#f43e06"));
                        for (BluetoothGattCharacteristic bc : g.getCharacteristics()) {//轮询特征值
                            boolean canRead = (bc.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0;
                            boolean canWrite = (bc.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
                            sendOpenDoorMessage("===>轮询写入特征值：" + bc.getUuid() + " ,canRead: " + canRead + " ,canWrite: " + canWrite);
                            if (canWrite) {
                                sendOpenDoorMessage("====>开始写入数据,deviceName：" + device.getBleName() + ",data: " + ByteUtils.bytes2HexStr(data) + ",uuid:" + g.getUuid() + ",CharacteristicsUUid:" + bc.getUuid(), Color.parseColor("#f43e06"));
                                sendOpenDoorMessage("====>开始写入数据,Thread name：" + Thread.currentThread().getName(), Color.parseColor("#f43e06"));
                                //开始写入
                                runOnUiThreadDelayed(() -> {
                                    boolean writeResult = Ble.getInstance().writeByUuid(
                                            device,
                                            data,
                                            g.getUuid(),
                                            bc.getUuid(),
                                            new BleWriteCallback<BleDevice>() {
                                                @Override
                                                public void onWriteSuccess(BleDevice device, BluetoothGattCharacteristic characteristic) {
                                                    sendOpenDoorMessage("写入特征成功:" + device.getBleName(), Color.GREEN);
                                                }

                                                @Override
                                                public void onWriteFailed(BleDevice device, int failedCode) {
                                                    super.onWriteFailed(device, failedCode);
                                                    sendOpenDoorMessage("写入特征失败:" + failedCode, Color.RED);
                                                }
                                            });
                                    sendOpenDoorMessage("=====>写入特征返回：" + writeResult, Color.BLUE);
                                }, 1000);
                                runOnUiThreadDelayed(() -> {
                                    sendOpenDoorMessage("开始设置通知");
                                    Ble.getInstance().enableNotifyByUuid(
                                            device,
                                            true,
                                            g.getUuid(),
                                            bc.getUuid(),
                                            new BleNotifyCallback<BleDevice>() {
                                                @Override
                                                public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
                                                    sendOpenDoorMessage("收到" + device.getBleName() + "的回复：" + ByteUtils.toHexString(characteristic.getValue()), Color.DKGRAY,12);
                                                    runOnUiThread(() -> {
                                                        String value = ByteUtils.bytes2HexStr(characteristic.getValue()).toLowerCase();
                                                        if (Constant.OPEN_DOOR_SUCCESS.equals(value)) {
                                                            ToastUtils.showShort(String.format("收到设备通知数据: %s", "开门成功"));
                                                            sendOpenDoorMessage(String.format("收到设备通知数据: %s", "开门成功"), Color.GREEN);
//                                                        AppUtils.exitApp();
                                                        } else if (Constant.OPEN_DOOR_FAILURE.equals(value)) {
                                                            ToastUtils.showShort(String.format("收到设备通知数据: %s", "开门失败"));
                                                            sendOpenDoorMessage(String.format("收到设备通知数据: %s", "开门失败"), Color.RED);
                                                        } else {
                                                            sendOpenDoorMessage(String.format("收到设备通知数据: %s", ByteUtils.bytes2HexStr(characteristic.getValue()).toLowerCase()), Color.DKGRAY,12);
                                                            Log.d(TAG, String.format("收到设备通知数据: %s", ByteUtils.bytes2HexStr(characteristic.getValue()).toLowerCase()));
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onNotifySuccess(BleDevice device) {
                                                    super.onNotifySuccess(device);
                                                    sendOpenDoorMessage("订阅通知成功", Color.GREEN);
                                                }

                                                @Override
                                                public void onNotifyFailed(BleDevice device, int failedCode) {
                                                    super.onNotifyFailed(device, failedCode);
                                                    sendOpenDoorMessage(device.getBleName() + " 设置通知失败：" + failedCode, Color.RED);
                                                }

                                                @Override
                                                public void onNotifyCanceled(BleDevice device) {
                                                    super.onNotifyCanceled(device);
                                                    sendOpenDoorMessage(device.getBleName() + " 取消设置通知", Color.RED);
                                                }
                                            });
                                }, 500);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                sendOpenDoorMessage("openDoor onError:" + Log.getStackTraceString(e), Color.RED);
                e.printStackTrace();
            }
        }

        @Override
        public void onReady(BleDevice device) {
            super.onReady(device);
            //连接成功后，设置通知
//            Ble.getInstance().enableNotify(device, true, new BleNotifyCallback<BleDevice>() {
//                @Override
//                public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
//                    UUID uuid = characteristic.getUuid();
//                    BleLog.e(TAG, "onChanged==uuid:" + uuid.toString());
//                    sendOpenDoorMessage("onChanged==uuid:" + uuid);
//                    BleLog.e(TAG, "onChanged==data:" + ByteUtils.toHexString(characteristic.getValue()));
//                    runOnUiThread(() -> {
//                        String value = ByteUtils.bytes2HexStr(characteristic.getValue()).toLowerCase();
//                        if (Constant.OPEN_DOOR_SUCCESS.equals(value)) {
//                            ToastUtils.showShort(String.format("收到设备通知数据: %s", "开门成功"));
//                            sendOpenDoorMessage(String.format("收到设备通知数据: %s", "开门成功"));
////                            AppUtils.exitApp();
//                        } else if (Constant.OPEN_DOOR_FAILURE.equals(value)) {
//                            ToastUtils.showShort(String.format("收到设备通知数据: %s", "开门失败"));
//                            sendOpenDoorMessage(String.format("收到设备通知数据: %s", "开门失败"));
//                        } else {
//                            sendOpenDoorMessage(String.format("收到设备通知数据: %s", ByteUtils.bytes2HexStr(characteristic.getValue()).toLowerCase()));
//                            Log.d(TAG, String.format("收到设备通知数据: %s", ByteUtils.bytes2HexStr(characteristic.getValue()).toLowerCase()));
////                                ToastUtils.showShort(String.format("收到设备通知数据: %s", ByteUtils.toHexString(characteristic.getValue())));
//                        }
//                    });
//                }
//
//                @Override
//                public void onNotifySuccess(BleDevice device) {
//                    super.onNotifySuccess(device);
//                    sendOpenDoorMessage("通知成功：" + device.getBleName());
//                    BleLog.e(TAG, "onNotifySuccess: " + device.getBleName());
//                }
//            });
        }
    };

    public static void sendOpenDoorMessage(String message) {
        EventBus.getDefault().post(new MyEvent(message));
    }

    public static void sendOpenDoorMessage(String message, int color) {
        EventBus.getDefault().post(new MyEvent(message, color));
    }

    public static void sendOpenDoorMessage(String message, int color, int textSize) {
        EventBus.getDefault().post(new MyEvent(message, color, textSize));
    }


}
