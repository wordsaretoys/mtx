package com.wordsaretoys.mtx;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

/**
 * all dialogs
 */
public class Dialogs {

	/**
	 * open scale dialog fragment
	 */
	public static class OpenDialog extends DialogFragment {
		
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
			ListView v = (ListView) inflater
					.inflate(R.layout.open_dialog, container, false);
			v.setAdapter(Storage.Me.getAdapter());
			v.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
					MainActivity.takeAction(
						MainActivity.M_OPENSCALE, Long.valueOf(id));
					dismiss();
				}
				
			});
			getDialog().setTitle(R.string.dlgOpenTitle);
			return v;
		}
		
	}

	/**
	 * save scale dialog fragment
	 */
	public static class SaveDialog extends DialogFragment {

		public Dialog onCreateDialog(Bundle state) {
			AlertDialog.Builder builder = 
					new AlertDialog.Builder(getActivity());
			
			final EditText editName = (EditText) LayoutInflater
				.from(getActivity())
				.inflate(R.layout.save_dialog, null);
			builder
				.setView(editName)
				.setTitle(R.string.dlgSaveTitle)
				.setNegativeButton(R.string.dlgCancel, null)
				.setPositiveButton(R.string.dlgSave, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String name = editName.getText().toString();
						MainActivity.takeAction(
							MainActivity.M_SAVESCALE, name);
						dismiss();
					}
				});
			return builder.create();
		}
	}

	/**
	 * are you sure dialog fragment
	 */
	public static class AysDialog extends DialogFragment {

		public static AysDialog create(int action, int title) {
			AysDialog dlg = new AysDialog();
			Bundle b = new Bundle();
			b.putInt("action", action);
			b.putInt("title", title);
			dlg.setArguments(b);
			return dlg;
		}
		
		public Dialog onCreateDialog(Bundle state) {
			AlertDialog.Builder builder = 
				new AlertDialog.Builder(getActivity());
			
			builder
				.setTitle(getArguments().getInt("title"))
				.setNegativeButton(R.string.dlgNo, null)
				.setPositiveButton(R.string.dlgYes, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						MainActivity.takeAction(
							getArguments().getInt("action"), null);
						dismiss();
					}
				});
			return builder.create();
		}
	}
	
}
