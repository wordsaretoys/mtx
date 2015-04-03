package com.wordsaretoys.mtx;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * allows user to play back scale notes
 */
public class PlayFragment extends Fragment {

	static String TAG = "PlayFragment";
	
	// main layout
	View mainView;
	
	// scale playing view
	PlayView scaleView;

	// selection spinners
	Spinner spinVoice, spinRange;
	
	// scale object
	Scale scale;

	// voice pool for playback
	Synthesizer synthesizer;

	// current octave range
	int octaveLo, octaveHi;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		mainView = inflater.inflate(R.layout.play_fragment, container);
		
		scaleView = (PlayView) mainView.findViewById(R.id.scaleView);
		scaleView.setListener(new PlayView.Listener() {
			
			@Override
			public void onCellUp(int cell) {
				synthesizer.stopTone(cell);
			}
			
			@Override
			public void onCellDown(int cell) {
				synthesizer.playTone(cell);
			}
		});
		
		spinVoice = (Spinner) mainView.findViewById(R.id.spinVoice);
		ArrayAdapter<CharSequence> adaptVoice = new ArrayAdapter<CharSequence>(
				getActivity(), 
				R.layout.spin_textview, 
				Lookup.VoiceNames); 
		adaptVoice.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinVoice.setAdapter(adaptVoice);
		spinVoice.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				synthesizer.setInstrument(
						Lookup.VoiceSource[pos], 
						Lookup.VoiceAttack[pos], 
						Lookup.VoiceRelease[pos]);
				synthesizer.generateBuffers(scale, octaveLo, octaveHi);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		spinVoice.setSelection(0);
		
		spinRange = (Spinner) mainView.findViewById(R.id.spinRange);
		ArrayAdapter<CharSequence> adaptRange = new ArrayAdapter<CharSequence>(
				getActivity(), 
				R.layout.spin_textview, 
				Lookup.OctaveRangeNames); 
		adaptRange.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinRange.setAdapter(adaptRange);
		spinRange.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				octaveLo = Lookup.OctaveRangeLo[pos];
				octaveHi = Lookup.OctaveRangeHi[pos];
				scaleView.setOctaveRange(octaveLo, octaveHi);
				synthesizer.generateBuffers(scale, octaveLo, octaveHi);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		spinRange.setSelection(2);	// C4-C6
		
		synthesizer = new Synthesizer();
		if (savedInstanceState != null) {
			synthesizer.restore(savedInstanceState);
		}
		
		setHasOptionsMenu(true);
		
		return mainView;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (outState != null) {
			synthesizer.backup(outState);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		synthesizer.release();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.play, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {

		case R.id.action_louder:
			synthesizer.makeLouder();
			break;
			
		case R.id.action_softer:
			synthesizer.makeSofter();
			break;
			
		default:
			return false;
		}
		return true;
	}
	
	/**
	 * set scale object
	 */
	public void setScale(Scale s) {
		scaleView.setScale(scale = s);
	}

	/**
	 * refresh fragment view/synth
	 */
	public void refresh() {
		scaleView.rearrange();
		synthesizer.generateBuffers(scale, octaveLo, octaveHi);
	}
}
