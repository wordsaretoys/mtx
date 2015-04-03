package com.wordsaretoys.mtx;

import java.util.ArrayList;

import android.os.Bundle;

/**
 * represents an n-tone equal temperament scale
 */
public class Scale {

	static int OctaveLo = 0;
	static int OctaveMid = 4;
	static int OctaveHi = 9;
	
	/**
	 * represents a single tone in scale
	 */
	class Tone {
		// steps to next tone
		int interval;
	}

	// scale database id
	long id;
	
	// scale name
	String name;
	
	// list of tones
	ArrayList<Tone> tones;
	
	// multi-octave pitch array
	float[] pitches;

	// true if data changed
	boolean dirty;
	
	/**
	 * ctor
	 */
	public Scale() {
		tones = new ArrayList<Tone>();
		clear();
	}
	
	public void clear() {
		id = -1;
		name = "New Scale";
		tones.clear();
		addTone();
		dirty = false;
	}
	
	/**
	 * appends a 1-step tone to the scale
	 */
	public void addTone() {
		Tone tone = new Tone();
		tone.interval = 1;
		tones.add(tone);
		generatePitches();
		dirty = true;
	}
	
	/**
	 * remove the specified tone from the scale
	 */
	public void removeTone(int i) {
		tones.remove(i);
		generatePitches();
		dirty = true;
	}
	
	/**
	 * get tone count
	 */
	public int getToneCount() {
		return tones.size();
	}
	
	/**
	 * get frequency of specified scale degree
	 * within "middle" octave
	 */
	public float getFrequency(int degree) {
		return getFrequency(OctaveMid, degree);
	}

	/**
	 * get frequency of specified scale degree 
	 * within specified octave 
	 */
	public float getFrequency(int octave, int degree) {
		int index = (octave - OctaveLo) * tones.size() + degree;
		return pitches[index];
	}
	
	/**
	 * get tone interval
	 */
	public int getInterval(int i) {
		return tones.get(i).interval;
	}
	
	/**
	 * increment tone interval
	 */
	public void nextStep(int i) {
		tones.get(i).interval++;
		generatePitches();
		dirty = true;
	}

	/**
	 * decrement tone interval
	 */
	public void prevStep(int i) {
		tones.get(i).interval--;
		generatePitches();
		dirty = true;
	}
	
	/**
	 * generate pitch array
	 */
	public void generatePitches() {
		// is the pitch array correctly sized?
		int len = tones.size() * (OctaveHi - OctaveLo + 1);
		if (pitches == null || pitches.length != len) {
			pitches = new float[len];
		}

		// interval sum gives us total number of tones
		int sum = 0;
		for (int i = 0; i< tones.size(); i++) {
			sum += tones.get(i).interval;
		}
		
		// generate scale root
		float root = (float) Math.pow(2f, 1f / sum);
		
		// for each octave
		for (int o = OctaveLo; o <= OctaveHi; o++) {
			int on = sum * (o - OctaveMid);
			int oi = tones.size() * (o - OctaveLo);
			// for each scale degree within the octave 
			for (int i = 0, step = 0; i < tones.size(); i++) {
				Tone tone = tones.get(i);
				// lock zero to the octave start
				pitches[i + oi] = (float)(440f * Math.pow(root, step + on - 0.75f * sum));
				// sum the next interval to get scale degree
				step += tone.interval;
			}
		}
	}
	
	/**
	 * store scale data to a bundle
	 */
	public void backup(Bundle b) {
		int[] steps = new int[tones.size()];
		for (int i = 0; i < tones.size(); i++) {
			steps[i] = tones.get(i).interval;
		}
		Bundle s = new Bundle();
		s.putIntArray("steps", steps);
		s.putString("name", name);
		s.putLong("id", id);
		s.putBoolean("dirty", dirty);
		b.putBundle("scale", s);
	}
	
	/**
	 * restore scale data from a bundle
	 */
	public void restore(Bundle b) {
		b = b.getBundle("scale");
		int[] steps = b.getIntArray("steps"); 
		tones.clear();
		for (int i = 0; i < steps.length; i++) {
			Tone tone = new Tone();
			tone.interval = steps[i];
			tones.add(tone);
		}
		generatePitches();
		name = b.getString("name");
		id = b.getLong("id");
		dirty = b.getBoolean("dirty");
	}
	
	/**
	 * load interval data from string
	 * of format "n,n,n,...,n" where
	 * n is any integer > 0
	 */
	public void load(String steps) {
		String [] s = steps.split(",");
		tones.clear();
		for (int i = 0; i < s.length; i++) {
			Tone tone = new Tone();
			tone.interval = Integer.parseInt(s[i]);
			tones.add(tone);
		}
		generatePitches();
	}
	
	/**
	 * return interval data as string
	 * of format "n,n,n,...,n" where
	 * n is any integer > 0
	 */
	public String toString() {
		String s = "";
		for (int i = 0; i < tones.size(); i++) {
			if (i > 0) {
				s += ",";
			}
			s += tones.get(i).interval;
		}
		return s;
	}
}
