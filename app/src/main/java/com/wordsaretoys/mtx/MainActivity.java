package com.wordsaretoys.mtx;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity implements Callback {

	// messages
	final static int M_OPENSCALE = 0;
	final static int M_SAVESCALE = 1;
	final static int M_DELETESCALE = 2;
	final static int M_NEWSCALE = 3;
	final static int M_OPENDIALOG = 4;
	
	// scale under construction
	Scale scale;
	
	// scale editing fragment
	EditFragment editFragment;
	
	// scale playback fragment
	PlayFragment playFragment;
	
	// message passing thing
	static Handler handler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		handler = new Handler(this);
		Storage.Me.onCreate(this);

		int selectedTab = 0;
		
		scale = new Scale();
		if (savedInstanceState != null) {
			scale.restore(savedInstanceState);
			selectedTab = savedInstanceState.getInt("selectedTab");
		} else {
			Storage.Me.read(0, scale);
		}
		getActionBar().setTitle(scale.name);
		
		editFragment = (EditFragment) 
				getFragmentManager().findFragmentById(R.id.editFragment);
		editFragment.setScale(scale);
		playFragment = (PlayFragment) 
				getFragmentManager().findFragmentById(R.id.playFragment);
		playFragment.setScale(scale);
		
		// create the action bar tabs
		Tab playTab = getActionBar().newTab()
			.setText(R.string.menuModePlay)
			.setTabListener(new TabListener() {
			
			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft) {
				playFragment.refresh();
				ft.hide(editFragment);
				ft.show(playFragment);
			}

			public void onTabReselected(Tab tab, FragmentTransaction ft) {}
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {}
		});
		getActionBar().addTab(playTab, selectedTab == 0);
		
		Tab editTab = getActionBar().newTab()
				.setText(R.string.menuModeEdit)
				.setTabListener(new TabListener() {
				
				@Override
				public void onTabSelected(Tab tab, FragmentTransaction ft) {
					ft.hide(playFragment);
					ft.show(editFragment);
				}

				public void onTabReselected(Tab tab, FragmentTransaction ft) {}
				public void onTabUnselected(Tab tab, FragmentTransaction ft) {}
			});
		getActionBar().addTab(editTab, selectedTab == 1);
		getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		scale.backup(outState);
		outState.putInt("selectedTab", 
			getActionBar().getSelectedTab().getPosition());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case R.id.create:
			if (scale.dirty) {
				Dialogs.AysDialog.create(
					M_NEWSCALE, R.string.dlgDiscardTitle)
						.show(getFragmentManager(), "dialog");
			} else {
				takeAction(M_NEWSCALE, null);
			}
			break;
		
		case R.id.open:
			if (scale.dirty) {
				Dialogs.AysDialog.create(
					M_OPENDIALOG, R.string.dlgDiscardTitle)
						.show(getFragmentManager(), "dialog");
			} else {
				takeAction(M_OPENDIALOG, null);
			}
			break;
			
		case R.id.save:
			if (scale.id == -1) {
				new Dialogs.SaveDialog()
					.show(getFragmentManager(), "dialog");
			} else {
				Storage.Me.write(scale);
				invalidateOptionsMenu();
			}
			break;
			
		case R.id.delete:
			Dialogs.AysDialog.create(
				M_DELETESCALE, R.string.dlgDeleteTitle)
					.show(getFragmentManager(), "dialog");
			break;
			
		default:
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.delete).setVisible(scale.id != -1);
		menu.findItem(R.id.save).setVisible(scale.dirty);
		return true;
	}
	
	@Override
	public boolean handleMessage(Message msg) {
		
		switch(msg.what) {
		case M_OPENSCALE:
			
			long id = (Long) msg.obj;
			Storage.Me.read(id, scale);
			onScaleChanged();
			break;
			
		case M_SAVESCALE:
			
			scale.name = (String) msg.obj;
			Storage.Me.write(scale);
			onScaleChanged();
			break;
			
		case M_DELETESCALE:

			Storage.Me.delete(scale.id);
			scale.clear();
			onScaleChanged();
			break;
			
		case M_NEWSCALE:
			scale.clear();
			onScaleChanged();
			break;
			
		case M_OPENDIALOG:
			new Dialogs.OpenDialog()
				.show(getFragmentManager(), "dialog");
			break;
			
		}
		
		return true;
	}
	
	/**
	 * send an action message
	 */
	static void takeAction(int msg, Object obj) {
		handler.sendMessage(
			Message.obtain(handler, msg, obj));
	}
	
	/**
	 * handle a change to the scale (new, open)
	 */
	void onScaleChanged() {
		getActionBar().setTitle(scale.name);
		editFragment.refresh();
		playFragment.refresh();
		invalidateOptionsMenu();
	}
	
}
