package com.example.admin.mybledemo.data;

import android.graphics.Color;

import com.example.admin.mybledemo.MyApplication;
import com.example.admin.mybledemo.R;

public class MyEvent {

    public String message;
    public int color = MyApplication.getInstance().getResources().getColor(R.color.colorPrimary);
    public int textSize = 14;

    public MyEvent(String message){
        this.message = message;
    }

    public MyEvent(String message,int color){
        this.message = message;
        this.color = color;
    }

    public MyEvent(String message,int color,int textSize){
        this.message = message;
        this.color = color;
        this.textSize = textSize;
    }

}
