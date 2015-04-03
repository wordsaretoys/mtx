package com.wordsaretoys.mtx;

import java.util.Arrays;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * displays a microtonal scale with multi-touch
 * capability for real-time chording/playing
 */
public class PlayView extends CellView {

	static String TAG = "PlayView";
	
	/**
	 * callbacks for cell touch
	 */
	public interface Listener {
		public void onCellDown(int cell);
		public void onCellUp(int cell);
	}

	
	// brushes for drawing cells
	Paint textBrush;
	
	// client listener
	Listener listener;
	
	// useful colors
	int cellPressColor;

	// drawing parameters
	float txcent, box1;
	
	// used for no-alloc text manipulation
	StringBuilder stringer;
	char[] chars;

	// the scale to display
	Scale scale;
	
	// octave range to display
	int octaveLo, octaveHi;
	
	/**
	 * ctor, used for XML instantiation
	 */
	public PlayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		Resources res = getResources();
		
		cellPressColor = res.getColor(R.color.HalfBlue);
		
		textBrush = new Paint();
		textBrush.setTypeface(Typeface.SANS_SERIF);
		textBrush.setAntiAlias(true);
		textBrush.setTextAlign(Align.CENTER);

		stringer = new StringBuilder();
		chars = new char[256];
	}
	
	/**
	 * set display scale
	 */
	public void setScale(Scale s) {
		scale = s;
	}
	
	/**
	 * set client listener
	 */
	public void setListener(Listener l) {
		listener = l;
	}
	
	/**
	 * set octave range
	 */
	public void setOctaveRange(int lo, int hi) {
		octaveLo = lo;
		octaveHi = hi;
		// octave range may be changed at any point
		// (unlike scale reference) so rearrange it 
		if (getWidth() > 0) {
			rearrange();
		}
	}
	
	@Override
	protected void onDrawCell(Canvas canvas, int index, RectF rect) {
		int degree = index % scale.getToneCount();
		int octave = index / scale.getToneCount() + octaveLo;
		
		// color box around degree
		fillBrush.setColor(Color.RED);
		canvas.drawRect(rect.left, rect.top, 
				rect.left + box1 * 2, rect.top + box1 * 2, 
				fillBrush);
		
		// scale degree
		stringer.setLength(0);
		stringer.append(degree + 1)
				.getChars(0, stringer.length(), chars, 0);
		textBrush.setColor(Color.WHITE);
		canvas.drawText(chars, 0, stringer.length(), 
				rect.left + box1, rect.top + txcent + box1, textBrush);

		float ty = rect.bottom - 2 * textBrush.descent();
		float tx = rect.centerX();
		textBrush.setColor(Color.BLACK);

		// frequency
		float freq = scale.getFrequency(octave, degree);
		freq = (float)(Math.round(freq * 100f) / 100f);
		stringer.setLength(0);
		stringer.append(freq)
				.append(" Hz")
				.getChars(0, stringer.length(), chars, 0);
		canvas.drawText(chars, 0, stringer.length(),
				tx, ty, textBrush);
		
		// if pressed, blend pressed color
		if (cells[index] != -1) {
			fillBrush.setColor(cellPressColor);
			canvas.drawRect(rect, fillBrush);
		}
	}
	
	/**
	 * handle touch events
	 */
	public boolean onTouchEvent(MotionEvent e) {

		int cmd = e.getActionMasked();
		int idx = e.getActionIndex();
		int id = e.getPointerId(idx);

		boolean down = (cmd == MotionEvent.ACTION_DOWN) ||
			(cmd == MotionEvent.ACTION_POINTER_DOWN);
		boolean up = (cmd == MotionEvent.ACTION_UP) ||
				(cmd == MotionEvent.ACTION_POINTER_UP);
		
		// if it's a pointer coming down
		if (down) {
			// where did it land?
			int c = getCol(e.getX(idx));
			int r = getRow(e.getY(idx));
			int i = cols * r + c;
			if (i >= 0 && i < cells.length && cells[i] == -1) {
				// store it off and inform client
				cells[i] = id;
				listener.onCellDown(i);
				// redraw 
				postInvalidate();
			}
		} else if (up) {
			// pointer coming up
			// which cell did it originate in?
			for (int i = 0; i < cells.length; i++) {
				if (cells[i] == id) {
					// reset cell and inform client
					cells[i] = -1;
					listener.onCellUp(i);
					break;
				}
			}
			// redraw 
			postInvalidate();
		} else if (cmd == MotionEvent.ACTION_CANCEL) {
			// handle as a just-in-case
			resetCells();
		}
		return true;
	}
	
	/**
	 * recalculate cell arrangement
	 * call if scale tone count or view dimensions change
	 */
	public void rearrange() {
		// make sure all previous states are handled
		resetCells();
		
		// how many cells?
		int tc = scale.getToneCount();
		int oc = octaveHi - octaveLo + 1;
		// always create extra cell for first note in next octave
		cells = new int[tc * oc + 1];
		Arrays.fill(cells, -1);

		super.rearrange();

		// generate new drawing parameters
		float sq = (float) Math.sqrt(cellWidth * cellHeight);
		textBrush.setTextSize(sq / 5f);
		txcent = -(textBrush.ascent() + textBrush.descent()) * 0.5f;
		box1 = sq / 6f;
		
		// redraw from new params
		postInvalidate();
	}
	
	/**
	 * reset cell states and inform client
	 */
	private void resetCells() {
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] != -1) {
				cells[i] = -1;
				listener.onCellUp(i);
			}
		}
		postInvalidate();
	}
}
