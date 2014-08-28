/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.reminder;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class ReminderCheckService extends Service {

	boolean notification_on = false;
	public static final String ACTION_SNOOZE = "snooze";
	public static final String ACTION_NOTIFY = "notify";

	public boolean debug_mode = false;

	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		debug_mode = intent.getBooleanExtra("debug_mode", false);
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(ReminderCheckService.this);
		notification_on = sp.getBoolean("notification_service", false);
		long now = System.currentTimeMillis();

		if (ACTION_SNOOZE.equals(intent.getAction())) {
			// Cancel the notification
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancel(OpacClient.NOTIF_ID);

			Intent i = new Intent(ReminderCheckService.this,
					ReminderCheckService.class);
			i.putExtra("count", intent.getIntExtra("count", 1));
			i.putExtra("for", intent.getLongExtra("for", now));
			if (intent.hasExtra("account"))
				i.putExtra("account", intent.getLongExtra("account", 1));
			PendingIntent sender = PendingIntent.getService(
					ReminderCheckService.this, 0, i,
					PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

			Log.i("ReminderCheckService", "Opac App Service: Snooze");
			// Notify again in 1 day
			am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
					+ (1000 * 3600 * 24), sender);
		} else if (ACTION_NOTIFY.equals(intent.getAction())) {
			int count = intent.getIntExtra("count", 1);

			if (!notification_on)
				return START_NOT_STICKY;

			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

			NotificationCompat.Builder nb = new NotificationCompat.Builder(
					ReminderCheckService.this);
			nb.setContentInfo(getString(R.string.notif_ticker, count));
			nb.setContentTitle(getString(R.string.notif_title));
			nb.setContentText(getString(R.string.notif_ticker, count));
			nb.setTicker(getString(R.string.notif_ticker, count));
			nb.setSmallIcon(R.drawable.ic_stat_notification);
			nb.setWhen(intent.getLongExtra("for", now));
			nb.setNumber(count);
			nb.setSound(null);

			Intent snoozeIntent = new Intent(ReminderCheckService.this,
					ReminderCheckService.class);
			snoozeIntent.putExtra("count", count);
			snoozeIntent.putExtra("for", intent.getLongExtra("for", now));
			if (intent.hasExtra("account"))
				snoozeIntent.putExtra("account",
						intent.getLongExtra("account", 1));
			snoozeIntent.setAction(ACTION_SNOOZE);
			PendingIntent piSnooze = PendingIntent.getService(
					ReminderCheckService.this, 0, snoozeIntent, 0);
			nb.addAction(R.drawable.ic_action_alarms,
					getResources().getText(R.string.snooze), piSnooze);
			nb.setDeleteIntent(piSnooze);

			Intent notificationIntent = new Intent(ReminderCheckService.this,
					((OpacClient) getApplication()).getMainActivity());
			notificationIntent.putExtra("fragment", "account");
			if (intent.hasExtra("account")) {
				// If there are notifications for more than one account,
				// account menu should be opened
				notificationIntent.putExtra("account",
						intent.getLongExtra("account", 1));
			} else {
				notificationIntent.putExtra("showmenu", true);
			}
			PendingIntent contentIntent = PendingIntent.getActivity(
					ReminderCheckService.this, 0, notificationIntent, 0);
			nb.setContentIntent(contentIntent);
			nb.setAutoCancel(true);

			Notification notification = nb.build();
			mNotificationManager.notify(OpacClient.NOTIF_ID, notification);

			sp.edit().putLong("notification_last", System.currentTimeMillis())
					.commit();
		} else {
			long waittime = (1000 * 3600 * 5);
			boolean executed = false;

			if (notification_on) {
				// We do not even want to sync, if notification is turned off
				// (battery and stuff...)
				ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
				if (networkInfo != null) {
					if (sp.getBoolean("notification_service_wifionly", false) == false
							|| networkInfo.getType() == ConnectivityManager.TYPE_WIFI
							|| networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
						executed = true;
						new CheckTask().execute();
					} else {
						waittime = (1000 * 1800);
					}
				} else {
					waittime = (1000 * 1800);
				}
			} else {
				waittime = (1000 * 3600 * 12);
			}

			Intent i = new Intent(ReminderCheckService.this,
					ReminderAlarmReceiver.class);
			PendingIntent sender = PendingIntent.getBroadcast(
					ReminderCheckService.this, OpacClient.BROADCAST_REMINDER,
					i, PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
					+ waittime, sender);

			if (!executed)
				stopSelf();
		}

		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public class CheckTask extends AsyncTask<Object, Object, Boolean> {

		private boolean exception = false;

		@SuppressLint("UseSparseArrays")
		@Override
		protected Boolean doInBackground(Object... params) {
			AccountDataSource data = new AccountDataSource(
					ReminderCheckService.this);
			data.open();
			List<Account> accounts = data.getAccountsWithPassword();
			if (accounts.size() == 0)
				return false;

			Log.i("ReminderCheckService",
					"Opac App Service: ReminderCheckService started");

			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(ReminderCheckService.this);

			boolean fail = false;
			long last_sent = sp.getLong("notification_last", 0);
			long now = System.currentTimeMillis();
			long warning = Long.decode(sp.getString("notification_warning",
					"367200000"));
			// long warning = 1000 * 3600 * 24 * 90;

			// Saves times with the list of corresponding expiring books
			Map<Long, List<Map<String, String>>> expiringBooks = new HashMap<Long, List<Map<String, String>>>();

			OpacClient app = (OpacClient) getApplication();
			for (Account account : accounts) {
				Log.i("ReminderCheckService",
						"Opac App Service: " + account.toString());
				AccountData res = null;

				try {
					Library library = app.getLibrary(account.getLibrary());
					OpacApi api = app.getNewApi(library);

					if (!api.isAccountSupported(library))
						continue;

					if ((now - data.getCachedAccountDataTime(account)) < 3600 * 1000 * 2) {
						// Don't sync this account too often
						res = data.getCachedAccountData(account);
					} else {
						res = api.account(account);
						data.storeCachedAccountData(account, res);
					}
				} catch (SocketException e) {
					e.printStackTrace();
					exception = true;
					fail = true;
				} catch (InterruptedIOException e) {
					e.printStackTrace();
					exception = true;
					fail = true;
				} catch (IOException e) {
					e.printStackTrace();
					exception = true;
					fail = true;
				} catch (OpacErrorException e) {
					e.printStackTrace();
					fail = true;
				} catch (Exception e) {
					ACRA.getErrorReporter().handleException(e);
					fail = true;
				}

				if (res == null) {
					fail = true;
					res = data.getCachedAccountData(account);
				}

				// int this_account = 0;

				for (Map<String, String> item : res.getLent()) {
					if (item.containsKey(AccountData.KEY_LENT_DOWNLOAD)) {
						// Don't remind people of bringing back ebooks,
						// because ... uhm...
						if (item.get(AccountData.KEY_LENT_DOWNLOAD).startsWith(
								"http"))
							continue;
					}
					if (item.containsKey(AccountData.KEY_LENT_DEADLINE_TIMESTAMP)) {
						long expiring = Long.parseLong(item
								.get(AccountData.KEY_LENT_DEADLINE_TIMESTAMP));
						if (!expiringBooks.containsKey(expiring)) {
							expiringBooks.put(expiring,
									new ArrayList<Map<String, String>>());
						}
						item.put("account", String.valueOf(account.getId()));
						expiringBooks.get(expiring).add(item);
					}
				}

			}
			data.close();

			// Schedule notifications for all the books that we haven't notified
			// for already
			AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

			for (Entry<Long, List<Map<String, String>>> entry : expiringBooks
					.entrySet()) {
				long warningTime = entry.getKey() - warning;
				if (warningTime > last_sent) {
					// Ignore expiry dates for which we already _sent_ a
					// notification

					// We need to use different request codes so that the
					// PendingIntents won't be overwritten
					int requestCode = (int) (OpacClient.REMINDER_REQ_CODE_MIN + Math
							.abs((int) (entry.getKey() / (1000 * 3600 * 12))));

					if (warningTime < now || debug_mode) {
						// Don't schedule alarms in the past
						// In debug mode, always alarm now
						warningTime = now + 1000 * 30;
					}

					Set<Long> accountsToNotify = new HashSet<Long>();
					for (Map<String, String> item : entry.getValue()) {
						if (!accountsToNotify.contains(Long.parseLong(item
								.get("account"))))
							accountsToNotify.add(Long.parseLong(item
									.get("account")));
					}

					Intent i = new Intent(ReminderCheckService.this,
							ReminderCheckService.class);
					i.setAction(ACTION_NOTIFY);
					i.putExtra("count", entry.getValue().size());
					i.putExtra("for", entry.getKey());

					if (accountsToNotify.size() == 1)
						i.putExtra("account",
								accountsToNotify.toArray(new Long[] {})[0]);

					PendingIntent sender = PendingIntent.getService(
							ReminderCheckService.this, requestCode, i,
							PendingIntent.FLAG_UPDATE_CURRENT);
					am.cancel(sender);
					am.set(AlarmManager.RTC_WAKEUP, warningTime, sender);
					Log.d("ReminderCheckService", entry.getValue().toString());
				}
			}

			// return new Object[] { expired_new, expired_total, notified,
			// first,
			// affected_accounts, first_affected_account };
			return !fail;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			Intent i = new Intent(ReminderCheckService.this,
					ReminderAlarmReceiver.class);
			PendingIntent sender = PendingIntent.getBroadcast(
					ReminderCheckService.this, OpacClient.BROADCAST_REMINDER,
					i, PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

			if (!result || exception) {
				Log.i("ReminderCheckService", "Opac App Service: Quick repeat");
				// Try again in one hour
				am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
						+ (1000 * 3600), sender);
			}

			stopSelf();
		}

	}
}
