/**
 * Copyright (C) 2011 matsumo All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.matsumo.adfuta;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AdFutaActivity extends Activity {
	private SharedPreferences pref;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panel);

		pref = PreferenceManager.getDefaultSharedPreferences(this);
		Button bb = (Button)findViewById(R.id.button1);
		bb.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intents = new Intent(AdFutaActivity.this, AdFutaService.class);
				if(AdFutaService.isRunning(AdFutaActivity.this)){
					stopService(intents);
					Toast.makeText(AdFutaActivity.this, "停止しました", Toast.LENGTH_LONG).show();
				}else{
					startService(intents);
					Toast.makeText(AdFutaActivity.this, "開始しました", Toast.LENGTH_LONG).show();
				}
			}
		});
		CheckBox cb1 = (CheckBox)findViewById(R.id.checkBox1);
		cb1.setChecked(pref.getBoolean("autoStart", false));
		cb1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Editor edit = pref.edit();
				edit.putBoolean("autoStart", isChecked);
				edit.commit();
			}
		});
    }
}