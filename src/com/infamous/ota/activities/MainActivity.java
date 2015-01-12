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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toolbar;

import com.infamous.ota.R;
import com.infamous.ota.RomUpdate;
import com.infamous.ota.tasks.LoadUpdateManifest;
import com.infamous.ota.utils.Constants;
import com.infamous.ota.utils.Preferences;
import com.infamous.ota.utils.Utils;

public class MainActivity extends Activity implements Constants{

	public final String TAG = this.getClass().getSimpleName();

	private Context mContext;

	private Builder mCompatibilityDialog;

	private boolean isLollipop;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if(action.equals(MANIFEST_LOADED)){
				// Reloads layouts to reflect the updated manifest information
				updateDonateLinkLayout();
				updateRomInformation();
				updateRomUpdateLayouts();
				updateWebsiteLayout();
			} 
		}
	};

	@SuppressLint({ "InflateParams", "NewApi" })
	@Override
	public void onCreate(Bundle savedInstanceState) {         

		mContext = this;
		setTheme(Preferences.getTheme(mContext));
		isLollipop = Utils.isLollipop();
		
		super.onCreate(savedInstanceState);              
		setContentView(R.layout.ota_main);

		if(isLollipop){
			Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
			setActionBar(toolbar);
			toolbar.setTitle(getResources().getString(R.string.app_name));
		} else {		
			// Custom ActionBar view
			ActionBar actionBar = getActionBar();
			actionBar.setTitle(R.string.app_name);
			LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, 
					LayoutParams.WRAP_CONTENT, 
					Gravity.END | 
					Gravity.CENTER_VERTICAL);
			View actionbarView = LayoutInflater.from(this).inflate(R.layout.ota_main_actionbar_top, null);
			actionBar.setCustomView(actionbarView, layoutParams);
			actionBar.setDisplayShowCustomEnabled(true);
		}
		
		createDialogs();

		// Check the correct build prop values are installed
		// Also executes the manifest/update check
		if(!Utils.isConnected(mContext)){
			Builder notConnectedDialog = new Builder(mContext);
			notConnectedDialog.setTitle(R.string.main_not_connected_title)
			.setMessage(R.string.main_not_connected_message)
			.setPositiveButton(R.string.ok, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((Activity) mContext).finish();					
				}
			})
			.show();
		} else {
			new CompatibilityTask(mContext).execute();
		}

		// Has the download already completed?
		Utils.setHasFileDownloaded(mContext);

		// Update the layouts
		updateDonateLinkLayout();
		updateRomInformation();
		updateRomUpdateLayouts();
		updateWebsiteLayout();
		
	}

	@Override
	public void onStart(){
		super.onStart();
		this.registerReceiver(mReceiver, new IntentFilter(MANIFEST_LOADED));
	}

	@Override
	public void onStop(){
		super.onStop();
		this.unregisterReceiver(mReceiver);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		if(isLollipop)
			getMenuInflater().inflate(R.menu.ota_menu_main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		if(isLollipop)
			switch (item.getItemId()) {
			case R.id.menu_info:
				openHelp(null);
				return true;
			case R.id.menu_settings:
				openSettings(null);
				return true;
			}
			return false;
	}

	private void createDialogs(){
		// Compatibility Dialog
		mCompatibilityDialog = new AlertDialog.Builder(mContext);
		mCompatibilityDialog.setCancelable(false);
		mCompatibilityDialog.setTitle(R.string.main_not_compatible_title);
		mCompatibilityDialog.setMessage(R.string.main_not_compatible_message);
		mCompatibilityDialog.setPositiveButton(R.string.ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				MainActivity.this.finish();				
			}
		});
	}

	private void updateRomUpdateLayouts(){
		View updateAvailable;
		View updateNotAvailable;
		updateAvailable = (CardView) findViewById(R.id.layout_main_update_available);
		updateNotAvailable = (CardView) findViewById(R.id.layout_main_no_update_available);
		updateAvailable.setVisibility(View.GONE);
		updateNotAvailable.setVisibility(View.GONE);

		TextView updateAvailableSummary = (TextView) findViewById(R.id.main_tv_update_available_summary);
		TextView updateNotAvailableSummary = (TextView) findViewById(R.id.main_tv_no_update_available_summary);

		// Update is available
		if(RomUpdate.getUpdateAvailability(mContext)){
			updateAvailable.setVisibility(View.VISIBLE);
			
			if(Preferences.getDownloadFinished(mContext)){ //  Update already finished?
				String htmlColorOpen = "";
				if(isLollipop){
					htmlColorOpen = "<font color='#009688'>";
				} else {
					htmlColorOpen = "<font color='#33b5e5'>";
				}	
				String htmlColorClose = "</font>";
				String updateSummary = RomUpdate.getFilename(mContext)
						+ "<br />"
						+ htmlColorOpen
						+ getResources().getString(R.string.main_download_completed_details)
						+ htmlColorClose;
				updateAvailableSummary.setText(Html.fromHtml(updateSummary));
			} else {
				String htmlColorOpen = "";
				if(isLollipop){
					htmlColorOpen = "<font color='#009688'>";
				} else {
					htmlColorOpen = "<font color='#33b5e5'>";
				}
				String htmlColorClose = "</font>";
				String updateSummary = RomUpdate.getFilename(mContext)
						+ "<br />"
						+ htmlColorOpen
						+ getResources().getString(R.string.main_tap_to_download)
						+ htmlColorClose;
				updateAvailableSummary.setText(Html.fromHtml(updateSummary));
				
			}
		} else {
			updateNotAvailable.setVisibility(View.VISIBLE);

			boolean is24 = DateFormat.is24HourFormat(mContext);
			Date now = new Date();
			Locale locale = Locale.getDefault();
			String time = "";

			if(is24){
				time = new SimpleDateFormat("d, MMMM HH:mm", locale).format(now);
			} else {
				time = new SimpleDateFormat("d, MMMM hh:mm a", locale).format(now);
			}    

			Preferences.setUpdateLastChecked(this, time);
			String lastChecked = getString(R.string.main_last_checked);
			updateNotAvailableSummary.setText(lastChecked + " " + time);
		}
	}

	private void updateDonateLinkLayout() {
		CardView donateLink = (CardView) findViewById(R.id.layout_main_dev_donate_link);				
		donateLink.setVisibility(View.GONE);

		if(!RomUpdate.getDonateLink(mContext).trim().equals("null")){
			donateLink.setVisibility(View.VISIBLE);
		}
	}

	private void updateWebsiteLayout() {
		CardView webLink = (CardView) findViewById(R.id.layout_main_dev_website);
		TextView webLinkSummary = (TextView) findViewById(R.id.tv_main_dev_link_summary);
		webLink.setVisibility(View.GONE);

		if(!RomUpdate.getWebsite(mContext).trim().equals("null")){
			webLink.setVisibility(View.VISIBLE);
			webLinkSummary.setText(RomUpdate.getWebsite(mContext));
		}
	}

	private void updateRomInformation(){
		String htmlColorOpen = "";
		if(isLollipop){
			htmlColorOpen = "<font color='#009688'>";
		} else {
			htmlColorOpen = "<font color='#33b5e5'>";
		}
		String htmlColorClose = "</font>";

		//ROM name
		TextView romName = (TextView) findViewById(R.id.tv_main_rom_name);        
		String romNameTitle = getApplicationContext().getResources().getString(R.string.main_rom_name) + " ";
		String romNameActual = Utils.getProp("ro.ota.romname");        
		romName.setText(Html.fromHtml(romNameTitle + htmlColorOpen + romNameActual + htmlColorClose));

		//ROM version
		TextView romVersion = (TextView) findViewById(R.id.tv_main_rom_version);        
		String romVersionTitle = getApplicationContext().getResources().getString(R.string.main_rom_version) + " ";
		String romVersionActual = Utils.getProp("ro.ota.version");        
		romVersion.setText(Html.fromHtml(romVersionTitle + htmlColorOpen + romVersionActual + htmlColorClose));

		//ROM date
		TextView romDate = (TextView) findViewById(R.id.tv_main_rom_date);        
		String romDateTitle = getApplicationContext().getResources().getString(R.string.main_rom_build_date) + " ";
		String romDateActual = Utils.getProp("ro.build.date");
		romDate.setText(Html.fromHtml(romDateTitle + htmlColorOpen + romDateActual + htmlColorClose));

		//ROM android version
		TextView romAndroid = (TextView) findViewById(R.id.tv_main_android_version);        
		String romAndroidTitle = getApplicationContext().getResources().getString(R.string.main_android_verison) + " ";
		String romAndroidActual = Utils.getProp("ro.build.version.release");       
		romAndroid.setText(Html.fromHtml(romAndroidTitle + htmlColorOpen + romAndroidActual + htmlColorClose));

		//ROM developer
		TextView romDeveloper = (TextView) findViewById(R.id.tv_main_rom_developer);
		boolean showDevName = !RomUpdate.getDeveloper(this).equals("null");
		romDeveloper.setVisibility(showDevName? View.VISIBLE : View.GONE);

		String romDeveloperTitle = getApplicationContext().getResources().getString(R.string.main_rom_developer) + " ";
		String romDeveloperActual = RomUpdate.getDeveloper(this);       
		romDeveloper.setText(Html.fromHtml(romDeveloperTitle + htmlColorOpen + romDeveloperActual + htmlColorClose));

	}

	public void openCheckForUpdates(View v){
		this.recreate();
	}

	public void openDownload(View v){
		Intent intent = new Intent(mContext, AvailableActivity.class);
		startActivity(intent);
	}

	public void openDonationPage(View v){
		String url = RomUpdate.getDonateLink(mContext);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		startActivity(intent);
	}

	public void openWebsitePage(View v){
		String url = RomUpdate.getWebsite(mContext);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		startActivity(intent);
	}

	public void openSettings(View v){
		Intent intent = new Intent(mContext, SettingsActivity.class);
		startActivity(intent);		
	}

	public void openHelp (View v){
		Intent intent = new Intent(mContext, AboutActivity.class);
		startActivity(intent);
	}
	
	public class CompatibilityTask extends AsyncTask<Void, Boolean, Boolean> implements Constants{

		public final String TAG = this.getClass().getSimpleName();

		private Context mContext;
		private String mPropName;

		public CompatibilityTask(Context context){
			mContext = context;
			mPropName = mContext.getResources().getString(R.string.prop_name);
		}

		@Override
		protected Boolean doInBackground(Void... v) {
			return Utils.doesPropExist(mPropName);
		}

		@Override
		protected void onPostExecute(Boolean result) {

			if(result){
				if(DEBUGGING)
					Log.d(TAG, "Prop found");
				new LoadUpdateManifest(mContext, true).execute();
			} else {
				if(DEBUGGING)
					Log.d(TAG, "Prop not found");
				mCompatibilityDialog.show();
			}
			super.onPostExecute(result);
		}
	} 
}