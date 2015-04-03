package com.wordsaretoys.mtx;

import android.app.Fragment;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

/**
 * allows user to edit scale notes
 */
public class EditFragment extends Fragment {

	static String TAG = "EditFragment";
	
	// main layout
	View mainView;
	
	// scale editing view
	EditView scaleView;

	// selection spinner
	Spinner spinScale;

	// scale object
	Scale scale;

	// edit action mode, if available
	ActionMode editMode;

	// edit action mode listener
	EditListener editListener;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		mainView = inflater.inflate(R.layout.edit_fragment, container);

		scaleView = (EditView) mainView.findViewById(R.id.scaleView);
		scaleView.setListener(new EditView.Listener() {
			
			@Override
			public void onCellDown(int cell) {
				editListener.selected = cell;
				
				if (editMode == null) {
					editMode = getActivity().startActionMode(editListener);
				} else {
					editMode.invalidate();
				}
			}
		});

		setHasOptionsMenu(true);

		editListener = new EditListener();
		
		if (savedInstanceState != null) {
			editListener.selected = savedInstanceState.getInt("selected");
		}
		
		return mainView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("selected",
			(editMode == null) ? -1 : editListener.selected);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (editListener.selected != -1) {
			scaleView.setSelected(editListener.selected);
			editMode = getActivity().startActionMode(editListener);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.edit, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		
		case R.id.action_add_tone:
			scale.addTone();
			scaleView.rearrange();
			getActivity().invalidateOptionsMenu();
			break;
			
		default:
			return false;
		}
		return true;
	}
	
	@Override
	public void onHiddenChanged(boolean hidden) {
		if (hidden && editMode != null) {
			editMode.finish();
			editMode = null;
		}
	}
	
	/**
	 * set scale object
	 */
	public void setScale(Scale s) {
		scaleView.setScale(scale = s);
	}
	
	/**
	 * refresh view (new scale)
	 */
	public void refresh() {
		scaleView.rearrange();
	}
	
	/**
	 * action mode callback for editing
	 */
	class EditListener implements ActionMode.Callback {

		int selected = -1;
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = getActivity().getMenuInflater();
			inflater.inflate(R.menu.edit_action, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			if (mode != null) {
				// can't decrement interval == 1
				int step = scale.getInterval(selected);
				menu.findItem(R.id.action_prev_step).setVisible(step > 1);
				// can't remove last tone
				int tones = scale.getToneCount();
				menu.findItem(R.id.action_del_tone).setVisible(tones > 1);
			}
			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch(item.getItemId()) {
			
			case R.id.action_next_step:
				scale.nextStep(selected);
				mode.invalidate();
				scaleView.postInvalidate();
				getActivity().invalidateOptionsMenu();
				break;
			
			case R.id.action_prev_step:
				scale.prevStep(selected);
				mode.invalidate();
				scaleView.postInvalidate();
				getActivity().invalidateOptionsMenu();
				break;
				
			case R.id.action_del_tone:
				scale.removeTone(selected);
				selected = -1;
				mode.finish();
				scaleView.rearrange();
				getActivity().invalidateOptionsMenu();
				break;
				
			default:
				return false;
			}
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			editMode = null;
			scaleView.setSelected(-1);
			scaleView.postInvalidate();
		}
	}
}
