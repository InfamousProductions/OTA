/*
 * Copyright (C) 2014 Matt Booth (Kryten2k35).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.infamous.ota.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toolbar;
import com.infamous.ota.R;
import com.infamous.ota.utils.Preferences;
import com.infamous.ota.utils.Utils;

public class AboutActivity extends Activity implements OnClickListener{
	@SuppressLint("NewApi") @Override
	protected void onCreate(Bundle savedInstanceState) {
		Context context = this;
		setTheme(Preferences.getTheme(context));
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ota_about);
		

		if(Utils.isLollipop()){
			Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_about);
			setActionBar(toolbar);
			toolbar.setTitle(getResources().getString(R.string.app_name));
		}
		
		Typeface typeFace = Typeface.createFromAsset(getAssets(),"fonts/Roboto-Thin.ttf");
		
		TextView aboutTitle = (TextView) findViewById(R.id.about_tv_about_title);
		TextView donateTitle = (TextView) findViewById(R.id.about_tv_donate_title);
		TextView creditsTitle = (TextView) findViewById(R.id.about_tv_credits_title);
		TextView creditsSummary = (TextView) findViewById(R.id.about_tv_credits_summary);
		Button donateButton = (Button) findViewById(R.id.about_btn_donate);
		
		aboutTitle.setTypeface(typeFace);
		donateTitle.setTypeface(typeFace);
		creditsTitle.setTypeface(typeFace);
		
		String openHTML = "";
		if(Utils.isLollipop()){
			openHTML = "<font color='#009688'>";
		} else {
			openHTML = "<font color='#33b5e5'>";
		}
        String closeHTML = "</font>";
        String newLine = "<br />";
        String creditsText =
                openHTML + "Matt Booth" + closeHTML + " - For the source" + newLine +
                openHTML + "Jamison904" + closeHTML + " - Making this Infamous" + newLine +
                openHTML + "Mr Impossible"+ closeHTML + " - All the Infamous updates and hard work" + newLine;  
        creditsSummary.setText(Html.fromHtml(creditsText));
        
        donateButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {		
		String url = "http://goo.gl/Gw2Lf9";
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		startActivity(intent);
	}
}
