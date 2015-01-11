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

package com.infamous.ota.receivers;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.infamous.ota.RomUpdate;
import com.infamous.ota.activities.AvailableActivity;
import com.infamous.ota.activities.MainActivity;
import com.infamous.ota.tasks.LoadUpdateManifest;
import com.infamous.ota.utils.Constants;
import com.infamous.ota.utils.Preferences;
import com.infamous.ota.utils.Utils;

public class AppReceiver extends BroadcastReceiver implements Constants{

	public final String TAG = this.getClass().getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		long mDownloadID = Preferences.getDownloadID(context);

		if(DEBUGGING)
			Log.v(TAG, "Receiving " + mDownloadID);

		if(action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)){
			long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
			if (id != mDownloadID) {
				if(DEBUGGING)
					Log.v(TAG, "Ignoring unrelated download " + id);
				return;
			}

			DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
			DownloadManager.Query query = new DownloadManager.Query();
			query.setFilterById(id);
			Cursor cursor = downloadManager.query(query);

			// it shouldn't be empty, but just in case
			if (!cursor.moveToFirst()) {
				if(DEBUGGING)
					Log.e(TAG, "Empty row");
				return;
			}

			int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
			if (DownloadManager.STATUS_SUCCESSFUL != cursor.getInt(statusIndex)) {
				if(DEBUGGING)
					Log.w(TAG, "Download Failed");
				Preferences.setDownloadFinished(context, false);
				AvailableActivity.invalidateMenu();
				return;
			} else {
				if(DEBUGGING)
					Log.v(TAG, "Download Succeeded");
				Preferences.setDownloadFinished(context, true);
				AvailableActivity.setupProgress(context.getResources());
				AvailableActivity.invalidateMenu();
				return;
			}
		}

		if(action.equals(DownloadManager.ACTION_NOTIFICATION_CLICKED)){
			long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
			if (id != mDownloadID) {
				if(DEBUGGING)
					Log.v(TAG, "Ignoring unrelated download " + id);
				return;
			} else {
				Intent i = new Intent(context, MainActivity.class);
				context.startActivity(i);
			}
		}

		if(action.equals(MANIFEST_CHECK_BACKGROUND)){
			if(DEBUGGING)
				Log.d(TAG, "Receiving background check confirmation");

			boolean updateAvailable = RomUpdate.getUpdateAvailability(context);
			String filename = RomUpdate.getFilename(context);

			if(updateAvailable){
				Utils.setupNotification(context, filename);
				Utils.scheduleNotification(context, !Preferences.getBackgroundService(context));
			}
		}

		if(action.equals(START_UPDATE_CHECK)) {
			if (DEBUGGING)
				Log.d(TAG, "Update check started");
			new LoadUpdateManifest(context, false).execute();
		}

		if(action.equals(Intent.ACTION_BOOT_COMPLETED)){
			if(DEBUGGING) {
				Log.d(TAG, "Boot received");
			}
			boolean backgroundCheck = Preferences.getBackgroundService(context);
			if(backgroundCheck){
				if(DEBUGGING)
					Log.d(TAG, "Starting background check alarm");
				Utils.scheduleNotification(context, !Preferences.getBackgroundService(context));
			}
		}
	}
}


