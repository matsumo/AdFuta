/**
 * Copyright (C) 2011 matsumo All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.matsumo.adfuta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AdFutaService extends Service {
	public static final String ACTION_FROM_STATUS_BAR = "com.matsumo.adfuta.ACTION_FROM_STATUS_BAR";
	private static final int GET_CURRENT_APP_INTERVAL = 500;
	private WindowManager wm;
	private NotificationManager notificationManager;
	private FrameLayout mFrame;
	private View mView;
	private TextView mText, mTextActivity;
	private LinearLayout mButton;
	private LayoutParams mParams;
	private View mBanner;
	private boolean isEditing = false;
	private Handler mHandler;
	private BroadcastReceiver mReceiver;
	
	static private HashMap<String, BannerInfo> bannerInfo = new HashMap<String, BannerInfo>(50);
	private class ScreenReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
	    		mHandler.removeCallbacks(getCurrentAppTask);
//	    		System.out.println("ACTION_SCREEN_OFF");
	        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
	    		mHandler.removeCallbacks(getCurrentAppTask);
	    		mHandler.postDelayed(getCurrentAppTask, GET_CURRENT_APP_INTERVAL);
//	    		System.out.println("ACTION_SCREEN_ON");
	        }
	    }
	}

	@Override
	public void onCreate() {
		super.onCreate();
//		Toast.makeText(getApplicationContext(), "onCreate", Toast.LENGTH_LONG).show();
		am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
		notificationManager = (NotificationManager) getSystemService(Activity.NOTIFICATION_SERVICE);
		mHandler = new Handler();

//		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		loadBannerInfo();

		mView = LayoutInflater.from(this).inflate(R.layout.main, null);
		mParams = new LayoutParams();
		// 全画面
		mParams.width = LayoutParams.FILL_PARENT;
		mParams.height = LayoutParams.FILL_PARENT;
		// SystemAlert
		mParams.type = LayoutParams.TYPE_SYSTEM_OVERLAY/*TYPE_SYSTEM_ALERT*/;
		// 透過
		mParams.format = PixelFormat.TRANSLUCENT;
		// 裏はぼやける
//		params.flags = LayoutParams.FLAG_BLUR_BEHIND;
		mParams.flags = /*LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |*/
//					   LayoutParams.FLAG_LAYOUT_IN_SCREEN |
					   LayoutParams.FLAG_LAYOUT_NO_LIMITS |
					   LayoutParams.FLAG_NOT_TOUCH_MODAL |
					   LayoutParams.FLAG_NOT_FOCUSABLE;
		mParams.gravity = Gravity.TOP|Gravity.LEFT;

        mFrame = (FrameLayout)mView.findViewById(R.id.frameLayout1);
        mBanner = mView.findViewById(R.id.Pawn);
        mBanner.setOnTouchListener(new OnTouchListener() { 
        	private int ox, oy;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				FrameLayout.LayoutParams par = (FrameLayout.LayoutParams) v.getLayoutParams();
				switch (event.getAction()) {
				case MotionEvent.ACTION_MOVE:
					par.topMargin = (int) event.getRawY() - oy/*(v.getHeight()/2)*/;
					par.leftMargin = 0/*(int) event.getRawX() - (v.getWidth() / 2)*/;
					v.setLayoutParams(par);
					mText.setText(String.format("(%d,%d)", par.leftMargin, par.topMargin));
					break;
				case MotionEvent.ACTION_UP:
//					par.height = 40;
//					par.width = 40;
					par.topMargin = (int) event.getRawY() - oy/*(v.getHeight()/2)*/;
					par.leftMargin = 0/*(int) event.getRawX() - (v.getWidth() / 2)*/;
					v.setLayoutParams(par);
					break;
				case MotionEvent.ACTION_DOWN:
//					par.height = 60;
//					par.width = 60;
//					v.setLayoutParams(par);
					oy = (int)(event.getRawY() - par.topMargin);
					ox = (int)(event.getRawX() - par.leftMargin);
					break;
				}
				return true;
			}
		});
        mButton = (LinearLayout)mView.findViewById(R.id.linearLayout1);
        mText = (TextView)mView.findViewById(R.id.textView1);
        mTextActivity = (TextView)mView.findViewById(R.id.textView2);
        mButton.setVisibility(View.GONE);
        mFrame.setVisibility(View.GONE);
        // Set
        ((Button)mView.findViewById(R.id.button1)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(currentApp != null){
					FrameLayout.LayoutParams par = (FrameLayout.LayoutParams) mBanner.getLayoutParams();
					bannerInfo.put(currentApp, new BannerInfo(par.leftMargin, par.topMargin, 320, 50, 0));
					saveBannerInfo();
					Toast.makeText(getApplicationContext(), String.format("set [%s] to (%d,%d)", currentApp, par.leftMargin, par.topMargin), Toast.LENGTH_SHORT).show();
				}

				mParams.type = LayoutParams.TYPE_SYSTEM_OVERLAY;
				wm.updateViewLayout(mView, mParams);
				mButton.setVisibility(View.GONE);
				isEditing = false;
//				currentApp = "";
			}
		});
        // Clear
        ((Button)mView.findViewById(R.id.button2)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(currentApp != null && bannerInfo.containsKey(currentApp)){
					bannerInfo.remove(currentApp);
					Toast.makeText(getApplicationContext(), String.format("remove [%s]", currentApp), Toast.LENGTH_SHORT).show();
				}
				
				mParams.type = LayoutParams.TYPE_SYSTEM_OVERLAY;
				wm.updateViewLayout(mView, mParams);
				mButton.setVisibility(View.GONE);
				isEditing = false;
//				currentApp = "";
			}
		});

		wm.addView(mView, mParams);
		setNotifyIcon(R.drawable.icon, getText(R.string.app_name).toString(), true);
		mHandler.postDelayed(getCurrentAppTask, GET_CURRENT_APP_INTERVAL);
		
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		mReceiver = new ScreenReceiver();
		registerReceiver(mReceiver, filter);
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		//normal
//		Toast.makeText(getApplicationContext(), "onStart()", Toast.LENGTH_SHORT).show();
		String action = null;
		if(intent != null) action = intent.getAction();
		if(action != null && action.equals(ACTION_FROM_STATUS_BAR)){
			// load current settings
			int x=0, y=0;
			if(currentApp != null && bannerInfo.containsKey(currentApp)){
				BannerInfo info = bannerInfo.get(currentApp);
				y = info.top;
				x = info.left;
			}else{
				y = mView.getHeight() - mBanner.getHeight();
			}
			FrameLayout.LayoutParams par = (FrameLayout.LayoutParams) mBanner.getLayoutParams();
			par.topMargin = y;
			par.leftMargin = x;
			mBanner.setLayoutParams(par);
			mText.setText(String.format("(%d,%d)", x, y));
			mTextActivity.setText(currentApp == null ? "???" : currentApp);

			mButton.setVisibility(View.VISIBLE);
			mParams.type = LayoutParams.TYPE_SYSTEM_ALERT;
			wm.updateViewLayout(mView, mParams);
			mFrame.setVisibility(View.VISIBLE);
			isEditing = true;
		}
	}

	@Override
	public void onDestroy() {
//		Toast.makeText(getApplicationContext(), "onDestroy", Toast.LENGTH_LONG).show();
		mHandler.removeCallbacks(getCurrentAppTask);
		wm.removeView(mView);

		notificationManager.cancelAll();
		stopForeground(true);
		unregisterReceiver(mReceiver);
//		mView = null;
		super.onDestroy();
	}

	class AdFutaBinder extends Binder {
		AdFutaService getService() {
			return AdFutaService.this;
		}
	}
	@Override
	public IBinder onBind(Intent intent) {
		return new AdFutaBinder();
	}
	
	@Override
	public void onRebind(Intent intent) {
//		Toast toast = Toast.makeText(getApplicationContext(), "onRebind()", Toast.LENGTH_SHORT);
//		toast.show();
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
//		Toast toast = Toast.makeText(getApplicationContext(), "onUnbind()", Toast.LENGTH_SHORT);
//		toast.show();
		return true; // 再度クライアントから接続された際に onRebind を呼び出させる場合は true を返す
	}

	/**
	 * Uses the given context to determine whether the service is already running.
	 */
	public static boolean isRunning(Context context) {
		ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
		List<RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

		for (RunningServiceInfo serviceInfo : services) {
			ComponentName componentName = serviceInfo.service;
			String serviceName = componentName.getClassName();
			if (serviceName.equals(AdFutaService.class.getName())) {
				return true;
			}
		}

		return false;
	}

	private void setNotifyIcon(int id, String status, boolean first){
		Intent intent = new Intent(this, AdFutaService.class);
		intent.setAction(ACTION_FROM_STATUS_BAR);
		PendingIntent contentIntent =
				PendingIntent.getService(this, 0, intent, 0);
		Notification notification = new Notification(
				id,
				getText(R.string.app_name),
				System.currentTimeMillis());
		notification.setLatestEventInfo(
				this,
				status,
				"タップすると設定画面を表示します",
				contentIntent);
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		startForeground(0, notification);
		notificationManager.notify(0, notification);
	}
	
	private static final String BANNERINFO_FILENAME = "bannerInfo.dat";
	private static final String BANNERINFO_SD_FILENAME = "/sdcard/bannerInfo.dat";
	private boolean loadBannerInfo(){
		try {
		    FileInputStream fis = openFileInput(BANNERINFO_FILENAME);
//			FileInputStream fis = new FileInputStream(new File(BANNERINFO_SD_FILENAME));
			ObjectInputStream ois = new ObjectInputStream(fis);
		    bannerInfo = (HashMap<String, BannerInfo>) ois.readObject();
/*			bannerInfo.clear();
			int c = ois.readInt();
			for(int i=0; i<c; i++){
				String k = (String)ois.readObject();
				BannerInfo v = (BannerInfo)ois.readObject();
				bannerInfo.put(k, v);
			}*/
		    ois.close();
		    return true;
		} catch (Exception e) {
            e.printStackTrace();
		}
		return false;
	}
	private boolean saveBannerInfo(){
		try {
		    FileOutputStream fos = openFileOutput(BANNERINFO_FILENAME, MODE_PRIVATE);
//			FileOutputStream fos = new FileOutputStream(new File(BANNERINFO_SD_FILENAME), true);
		    ObjectOutputStream oos = new ObjectOutputStream(fos);
		    oos.writeObject(bannerInfo);
/*		    oos.writeInt(bannerInfo.size());
		    for (String key : bannerInfo.keySet()) {
		    	oos.writeObject(key);
		    	oos.writeObject(bannerInfo.get(key));
		    }*/
		    oos.close();
			return true;
		} catch (Exception e) {
            e.printStackTrace();
		}
		return false;
	}

	/*
	 * This logic got from https://github.com/esmasui/underdevelopment/tree/master/ActivityInfoExample
	 * Special Thanks to esmasui!!
	 */
	private String currentApp = null;
    private ActivityManager am;
    private String getCurrentApp() {
        List<RunningAppProcessInfo> appProcesses = am.getRunningAppProcesses();
//        System.out.println("runningAppProcesses:" + appProcesses);

        List<RunningTaskInfo> runningTasks = am.getRunningTasks(32);

        for (RunningAppProcessInfo ai : appProcesses) {
            if (ai.importance != RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
                continue;

            boolean found = false;
            ComponentName topActivity = null;
            for (RunningTaskInfo task : runningTasks) {
                String pkgName = task.topActivity.getPackageName();
                if (ai.processName.equals(pkgName)) {
                    found = true;
                    topActivity = task.topActivity;
                    break;
                }
            }

            if (!found)
                continue;

/*            StringBuilder b = new StringBuilder();
            b.append("Package: ");
            b.append('\n');
            b.append('\t');
            b.append(topActivity.getPackageName());
            b.append('\n');
            b.append('\n');
            b.append("Activity: ");
            b.append('\n');
            b.append('\t');
            b.append(topActivity.getClassName());

            Toast.makeText(getApplicationContext(), b.toString(), Toast.LENGTH_SHORT).show();

            break;*/
            return topActivity.getClassName();
        }
        return null;
    }

	private Runnable getCurrentAppTask = new Runnable() {
		public void run(){
//			System.out.println("getCurrentAppTask");
//			String prevApp = currentApp;
			currentApp = getCurrentApp();
			if(!isEditing /*&& currentApp != null && prevApp != null && prevApp.compareTo(currentApp) != 0*/){
				if(currentApp != null && bannerInfo.containsKey(currentApp)){
					BannerInfo info = bannerInfo.get(currentApp);
					FrameLayout.LayoutParams par = (FrameLayout.LayoutParams) mBanner.getLayoutParams();
					par.topMargin = info.top;
					par.leftMargin = info.left;
					mBanner.setLayoutParams(par);
					mFrame.setVisibility(View.VISIBLE);
				}else{
					mFrame.setVisibility(View.GONE);
				}
			}else{
				mTextActivity.setText(currentApp == null ? "???" : currentApp);
			}
			mHandler.postDelayed(getCurrentAppTask, GET_CURRENT_APP_INTERVAL);
		}
	};
}
