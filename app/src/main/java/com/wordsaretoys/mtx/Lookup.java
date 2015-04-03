package com.wordsaretoys.mtx;

/**
 * lookup tables for populating controls/objects
 */
public class Lookup {

	//
	// octave range lookups
	//
	
	static String[] OctaveRangeNames = {
        "C2 - C4", "C3 - C5", "C4 - C6",
        "C2 - C5", "C3 - C6", "C4 - C7"
	};
	
	static int[] OctaveRangeLo = {
		2, 3, 4,
		2, 3, 4
	};
	
	static int[] OctaveRangeHi = {
		3, 4, 5,
		4, 5, 6
	};
	
	//
	// instrument voice lookups
	//
	
	static String[] VoiceNames = {
		"Electric Piano",
		"Vaguely Brass",
		"Oh Ah",
		"Buzz",
		"Pure Tone",
		"Little Voice",
		"Oomph"
	};
	
	static float[] VoiceAttack = {
		0.05f,
		0.1f,
		0.1f,
		0.05f,
		0.1f,
		0.01f,
		0.01f
	};
	
	static float[] VoiceRelease = {
		0.01f,
		0.1f,
		0.1f,
		0.001f,
		0.001f,
		0.1f,
		0.001f
	};
	
	static float[][] VoiceSource = {
		{0, 1, 0, 0.25f, 0, 0, 0, 0.3f},
		{0, 1, 0, 0, 0.3f, 0, 0.9f},
		{0, 0.4f, 0, 1f, 0, 0.5f},
		{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0.5f, 0, 0, 0.75f},
		{0, 1},
		{0, 0.1f, 0, 1, 0, 0, 0, 0.25f, 0, 0, 0, 0, 0, 0.3f},
		{0, 1, 0, 0, 0, 0.5f, 0, 0, 0.25f, 0, 0.15f}
	};
}
