package com.wordsaretoys.mtx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

/**
 * maintains a pool of low-latency tone generators 
 */
public class Synthesizer {

	static String TAG = "Synthesizer";

	static int SourceLen = 65536;
	static int SourceMod = SourceLen - 1;
	
	static float MinVolume = 0.01f;
	
	final static int Attack = 0;
	final static int Sustain = 1;
	final static int Release = 2;
	
	/**
	 * voice object
	 */
	class Voice {
		// audio track object
		AudioTrack track;
		// sound volume
		float volume;
		// tone index
		int tone;
		// envelope state
		int state;
	}

	/**
	 * tone buffer object 
	 */
	class Tone {
		// sample buffer
		short[] buffer;
		// sample rate
		int rate;
	}
	
	/**
	 * instrument timbre object
	 */
	class Timbre {
		// attack rate
		float attack;
		// release rate
		float release;
		// harmonics
		float[] harmonics;
		// harmonic sum
		float sum;
	}
	
	/**
	 * volume control handler callback
	 */
	class VolumeHandler implements Handler.Callback {
		@Override
		public boolean handleMessage(Message msg) {
			for (int i = 0; i < voices.size(); i++) {
				Voice voice = voices.get(i);
				if (voice.track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
					voice.track.setStereoVolume(voice.volume, voice.volume);
					switch (voice.state) {
					case Attack:
						voice.volume += timbre.attack;
						if (voice.volume >= maxVolume) {
							voice.volume = maxVolume;
							voice.state = Sustain;
						}
						break;
						
					case Sustain:
						break;
						
					case Release:
						voice.volume -= timbre.release;
						if (voice.volume <= MinVolume) {
							voice.volume = MinVolume;
							voice.track.pause();
							voice.track.flush();
							voice.tone = -1;
						}
						break;
					}
				}
			}
			handler.sendEmptyMessageDelayed(0, 1);
			return true;
		}
	}

	// native sample rate for all tracks
	int sampleRate;
	
	// list of pooled voices
	ArrayList<Voice> voices;
	
	// array of tones
	Tone[] tones;

	// current instrument
	Timbre timbre;
	
	// volume control handler
	Handler handler;
	
	// max volume for all voices
	float maxVolume;
	
	/**
	 * ctor
	 */
	public Synthesizer() {
		voices = new ArrayList<Voice>();
		handler = new Handler(new VolumeHandler());
		handler.sendEmptyMessage(0);
		timbre = new Timbre();
		timbre.harmonics = new float[0];
		sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
		maxVolume = 0.25f;
		// cache a few voices
		addVoice(); 
		addVoice();
		addVoice();
	}
	
	/**
	 * set instrument parameters
	 */
	public void setInstrument(float[] harmonics, float attack, float release) {
		timbre.attack = attack;
		timbre.release = release;
		timbre.harmonics = harmonics;
		// get harmonic sum
		float sum = 0;
		for (int h = 0; h < harmonics.length; h++) {
			sum += harmonics[h];
		}
		timbre.sum = sum;
	}
	
	/**
	 * generate sample buffer for a given frequency and instrument
	 */
	void generateSample(short[] buffer, float freq) {
		for (int h = 0; h < timbre.harmonics.length; h++) {
			if (timbre.harmonics[h] != 0) {
				float w = (float)((h + 1) * Math.PI / (buffer.length - 1));
				float a = timbre.harmonics[h] / timbre.sum;
				for (int i = 0; i < buffer.length; i++) {
					float s = a * (float) Math.sin(w * i);
					buffer[i] += (short)(s * 16384);
				}
			}
		}
	}
	
	/**
	 * generate tone buffers from specified
	 * scale and octave range
	 */
	public void generateBuffers(Scale scale, int octaveLo, int octaveHi) {
		// create the array of tone buffers
		int tc = scale.getToneCount();
		int oc = octaveHi - octaveLo + 1;
		// always include the first note of the next octave
		tones = new Tone[tc * oc + 1];
		// for each tone
		for (int t = 0; t < tones.length; t++) {
			tones[t] = new Tone();
			int degree = t % tc;
			int octave = t / tc + octaveLo;
			// find buffer length required to fit 1 sample period
			// at the specified tone frequency
			float freq = scale.getFrequency(octave, degree);
			int len = (int)(sampleRate / freq);
			tones[t].buffer = new short[len];
			// generate a sample for this buffer
			generateSample(tones[t].buffer, freq);
			// find adjusted playback rate to compensate
			tones[t].rate = sampleRate * (len - 1) / len;
		}
	}
	
	/**
	 * play a single tone, allocating a new voice
	 * when necessary.  
	 */
	public void playTone(int tone) {
		Voice voice = null;
		
		// find the last inactive voice
		for (int i = 0; i < voices.size();  i++) {
			Voice v = voices.get(i);
			if (v.tone == -1) {
				voice = v;
			}
		}
		
		// can't find one? make a new one. can't make one? get out.
		if (voice == null && (voice = addVoice()) == null) {
			return;
		}
		
		// set up the track with buffer, loop points
		short[] buffer = tones[tone].buffer;
		voice.track.write(buffer, 0, buffer.length);
		voice.track.setLoopPoints(0, buffer.length - 1, -1);
		voice.track.setPlaybackRate(tones[tone].rate);
		// set voice state 
		voice.tone = tone;
		voice.volume = MinVolume;
		voice.state = Attack;
		// and start playing it
		voice.track.play();
	}
	
	
	/**
	 * stop playing the indicated tone
	 */
	public void stopTone(int tone) {
		Voice voice = null;

		// find the voice playing this tone
		for (int i = 0; i < voices.size();  i++) {
			Voice v = voices.get(i);
			if (v.tone == tone && v.state != Release) {
				voice = v;
				break;
			}
		}
		
		if (voice == null) {
			return;
		}
		
		voice.state = Release;
	}

	/**
	 * release audio resources
	 */
	public void release() {
		for (int i = 0; i < voices.size(); i++) {
			voices.get(i).track.release();
		}
	}
	
	/**
	 * next loudness up
	 */
	public void makeLouder() {
		maxVolume = Math.min(1, maxVolume * 1.5f);
	}

	/**
	 * prev loudness
	 */
	public void makeSofter() {
		maxVolume = Math.max(MinVolume, maxVolume * 0.667f);
	}
	
	/**
	 * backup state
	 */
	public void backup(Bundle state) {
		state.putFloat("maxVolume", maxVolume);
	}
	
	/**
	 * restore state
	 */
	public void restore(Bundle state) {
		maxVolume = state.getFloat("maxVolume");
	}

	/**
	 * add a voice to the list
	 */
	Voice addVoice() {
		Voice voice = new Voice();
		voice.track = new AudioTrack(
				AudioManager.STREAM_MUSIC,
				sampleRate,
				AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				2048,
				AudioTrack.MODE_STATIC);
		if (voice.track.getState() == AudioTrack.STATE_UNINITIALIZED) {
			voice = null;
		} else {
			voice.track.setStereoVolume(1, 1);
			voice.tone = -1;
			voices.add(voice);
		}
		return voice;
	}
	
	/**
	 * debugging tool
	 * NOTE: requires write external storage permission
	 */
	void dumpBuffer(short[] buffer, String filename) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < buffer.length; i++) {
			sb.append(buffer[i]).append('\n');
		}
		File sharedDir = Environment.getExternalStorageDirectory();
		File dir = new File(sharedDir, "Android/data/com.wordsaretoys.mtx");
		if (!dir.isDirectory()) {
			dir.mkdir();
		}
		File file = new File(dir, filename);
		try {
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(sb.toString());
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
