package de.danoeh.antennapod.activity;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.OpmlFeedQueuer;
import de.danoeh.antennapod.asynctask.OpmlImportWorker;
import de.danoeh.antennapod.opml.OpmlElement;
import de.danoeh.antennapod.util.StorageUtils;

public class OpmlImportActivity extends SherlockActivity {
	private static final String TAG = "OpmlImportActivity";

	public static final String IMPORT_DIR = "import/";

	private TextView txtvPath;
	private Button butStart;
	private String importPath;

	private OpmlImportWorker importWorker;

	private static ArrayList<OpmlElement> readElements;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.opml_import);

		txtvPath = (TextView) findViewById(R.id.txtvPath);
		butStart = (Button) findViewById(R.id.butStartImport);

		butStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				checkFolderForFiles();
			}

		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		StorageUtils.checkStorageAvailability(this);
		setImportPath();
	}

	private void setImportPath() {
		File importDir = getExternalFilesDir(IMPORT_DIR);
		boolean success = true;
		if (!importDir.exists()) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Import directory doesn't exist. Creating...");
			success = importDir.mkdir();
			if (!success) {
				Log.e(TAG, "Could not create directory");
			}
		}
		if (success) {
			txtvPath.setText(importDir.toString());
			importPath = importDir.toString();
		} else {
			txtvPath.setText(R.string.opml_directory_error);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return false;
		}
	}
	
	/** Looks at the contents of the import directory and decides what to do. */
	private void checkFolderForFiles() {
		File dir = new File(importPath);
		if (dir.isDirectory()) {
			File[] fileList = dir.listFiles();
			if (fileList.length == 1) {
				if (AppConfig.DEBUG) Log.d(TAG, "Found one file, choosing that one.");
				startImport(fileList[0]);
			} else if (fileList.length > 1) {
				Log.w(TAG,
						"Import directory contains more than one file.");
				askForFile(dir);
			} else {
				Log.e(TAG, "Import directory is empty");
				Toast toast = Toast
						.makeText(this, R.string.opml_import_error_dir_empty,
								Toast.LENGTH_LONG);
				toast.show();
			}
	}
	}

	private void startImport(File file) {
		
			if (file != null) {
				importWorker = new OpmlImportWorker(this, file) {

					@Override
					protected void onPostExecute(ArrayList<OpmlElement> result) {
						super.onPostExecute(result);
						if (result != null) {
							if (AppConfig.DEBUG)
								Log.d(TAG, "Parsing was successful");
							readElements = result;
							startActivityForResult(new Intent(
									OpmlImportActivity.this,
									OpmlFeedChooserActivity.class), 0);
						} else {
							if (AppConfig.DEBUG)
								Log.d(TAG, "Parser error occured");
						}
					}
				};
				importWorker.executeAsync();
			}
		}
	
	
	/** Asks the user to choose from a list of files in a directory and returns his choice. */
	private void askForFile(File dir) {
		final File[] fileList = dir.listFiles();
		String[] fileNames = dir.list();
		
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(R.string.choose_file_to_import_label);
		dialog.setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (AppConfig.DEBUG) Log.d(TAG, "Dialog was cancelled");
				dialog.dismiss();
			}});
		dialog.setItems(fileNames, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (AppConfig.DEBUG) Log.d(TAG, "File at index " + which + " was chosen");
				dialog.dismiss();
				startImport(fileList[which]);
			}
		});
		dialog.create().show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Received result");
		if (resultCode == RESULT_CANCELED) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Activity was cancelled");
		} else {
			int[] selected = data
					.getIntArrayExtra(OpmlFeedChooserActivity.EXTRA_SELECTED_ITEMS);
			if (selected != null && selected.length > 0) {
				OpmlFeedQueuer queuer = new OpmlFeedQueuer(this, selected) {

					@Override
					protected void onPostExecute(Void result) {
						super.onPostExecute(result);
						finish();
					}

				};
				queuer.executeAsync();
			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG, "No items were selected");
			}
		}
	}

	public static ArrayList<OpmlElement> getReadElements() {
		return readElements;
	}

}
