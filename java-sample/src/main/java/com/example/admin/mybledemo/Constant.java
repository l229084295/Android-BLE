package com.example.admin.mybledemo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 全局静态常量值
 * Created by jerry on 2018/10/5.
 */
public class Constant {

    public static String OPEN_DOOR_SUCCESS = "025801015a";
    public static String OPEN_DOOR_FAILURE = "0258fefe5a";

    /**
     * SharePreferences常量保存类
     */
    public interface SP {
        String OTA_FILE_EXIST = "ota_file_exist";//ota文件是否存在

        String MAC_ADDRESS_HISTORY = "mac_address_history";//蓝牙mac地址历史
    }

    //全局静态常量
    @Retention(RetentionPolicy.SOURCE)
    public @interface Constance {
        String OTA_FILE_PATH = "new_ota.bin";//ota文件路径
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Command {
        int BLE_CAR_COMMAND = 'C';
        int BLE_ORDER_COMMAND = 'O';
        int BLE_MUSIC_COMMAND = 'M';

        int TF_MUSIC_TYPE = 'T';
        int BT_MUSIC_TYPE = 'B';
        int GT_MUSIC_TYPE = 'G';
    }

}
