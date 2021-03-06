package de.danoeh.antennapod.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.DownloadlistAdapter;
import de.danoeh.antennapod.asynctask.DownloadObserver;
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.service.DownloadService;
import de.danoeh.antennapod.storage.DownloadRequester;

/** Shows all running downloads in a list */
public class DownloadActivity extends SherlockListActivity implements
		ActionMode.Callback, DownloadObserver.Callback {

	private static final String TAG = "DownloadActivity";
	private static final int MENU_SHOW_LOG = 0;
	private static final int MENU_CANCEL_ALL_DOWNLOADS = 1;
	private DownloadlistAdapter dla;
	private DownloadRequester requester;

	private ActionMode mActionMode;
	private DownloadStatus selectedDownload;
	private DownloadObserver downloadObserver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (AppConfig.DEBUG)
			Log.d(TAG, "Creating Activity");
		requester = DownloadRequester.getInstance();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	protected void onPause() {
		super.onPause();
		unbindService(mConnection);
		if (downloadObserver != null) {
			downloadObserver.unregisterCallback(DownloadActivity.this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (AppConfig.DEBUG)
			Log.d(TAG, "Trying to bind service");
		bindService(new Intent(this, DownloadService.class), mConnection, 0);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (AppConfig.DEBUG)
			Log.d(TAG, "Stopping Activity");
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int position, long id) {
				DownloadStatus selection = dla.getItem(position);
				if (selection != null && mActionMode != null) {
					mActionMode.finish();
				}
				dla.setSelectedItemIndex(position);
				selectedDownload = selection;
				mActionMode = startActionMode(DownloadActivity.this);
				return true;
			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_SHOW_LOG, Menu.NONE,
				R.string.show_download_log).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(Menu.NONE, MENU_CANCEL_ALL_DOWNLOADS, Menu.NONE,
				R.string.cancel_all_downloads_label).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case MENU_SHOW_LOG:
			startActivity(new Intent(this, DownloadLogActivity.class));
			break;
		case MENU_CANCEL_ALL_DOWNLOADS:
			requester.cancelAllDownloads(this);
			break;
		}
		return true;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		if (!selectedDownload.isDone()) {
			menu.add(Menu.NONE, R.id.cancel_download_item, Menu.NONE,
					R.string.cancel_download_label).setIcon(
					R.drawable.navigation_cancel);
		}
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		boolean handled = false;
		switch (item.getItemId()) {
		case R.id.cancel_download_item:
			requester.cancelDownload(this, selectedDownload.getFeedFile()
					.getDownloadId());
			handled = true;
			break;
		}
		mActionMode.finish();
		return handled;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		mActionMode = null;
		selectedDownload = null;
		dla.setSelectedItemIndex(DownloadlistAdapter.SELECTION_NONE);
	}

	private DownloadService downloadService = null;
	boolean mIsBound;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			downloadService = ((DownloadService.LocalBinder) service)
					.getService();
			if (AppConfig.DEBUG)
				Log.d(TAG, "Connection to service established");
			dla = new DownloadlistAdapter(DownloadActivity.this, 0,
					downloadService.getDownloadObserver().getStatusList());
			setListAdapter(dla);
			downloadObserver = downloadService.getDownloadObserver();
			downloadObserver.registerCallback(DownloadActivity.this);
		}

		public void onServiceDisconnected(ComponentName className) {
			downloadService = null;
			mIsBound = false;
			Log.i(TAG, "Closed connection with DownloadService.");
		}
	};

	@Override
	public void onProgressUpdate() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				dla.notifyDataSetChanged();

			}
		});
	}

	@Override
	public void onFinish() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Observer has finished, clearing adapter");
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				dla.clear();
				dla.notifyDataSetInvalidated();
			}
		});

	}
}
