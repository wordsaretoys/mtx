package com.wordsaretoys.mtx;

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
public class EditView extends CellView {

	static String TAG = "EditView";
	
	static float Gap = 4f;

	/**
	 * callbacks for cell drawing and touch
	 */
	public interface Listener {
		public void onCellDown(int cell);
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

	// selected cell
	int selected = -1;
	
	/**
	 * ctor, used for XML instantiation
	 */
	public EditView(Context context, AttributeSet attrs) {
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
	 * set scale to edit
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
	 * set selection
	 */
	public void setSelected(int cell) {
		selected = cell;
	}

	@Override
	protected void onDrawCell(Canvas canvas, int index, RectF rect) {
		// color box around degree
		fillBrush.setColor(Color.RED);
		canvas.drawRect(rect.left, rect.top, 
				rect.left + box1 * 2, rect.top + box1 * 2, 
				fillBrush);
		
		// scale degree
		stringer.setLength(0);
		stringer.append(index + 1)
				.getChars(0, stringer.length(), chars, 0);
		textBrush.setColor(Color.WHITE);
		canvas.drawText(chars, 0, stringer.length(), 
				rect.left + box1, rect.top + txcent + box1, textBrush);

		float ty = rect.bottom - 2 * textBrush.descent();
		float tx = rect.centerX();
		textBrush.setColor(Color.BLACK);

		// interval
		int steps = scale.getInterval(index);
		stringer.setLength(0);
		stringer.append(steps)
				.append(steps > 1 ? " steps" : " step")
				.getChars(0, stringer.length(), chars, 0);
		canvas.drawText(chars, 0, stringer.length(),
				tx, ty, textBrush);

		// if pressed, blend pressed color
		if (index == selected) {
			fillBrush.setColor(cellPressColor);
			canvas.drawRect(rect, fillBrush);
		}
	}
	
	/**
	 * handle touch events
	 */
	public boolean onTouchEvent(MotionEvent e) {

		int cmd = e.getActionMasked();

		// if it's a pointer coming down
		if (cmd == MotionEvent.ACTION_DOWN) {
			// where did it land?
			int c = getCol(e.getX());
			int r = getRow(e.getY());
			int i = cols * r + c;
			if (i < cells.length && selected != i) {
				// store it off and inform client
				selected = i;
				listener.onCellDown(i);
				// redraw 
				postInvalidate();
			}
		}
		return true;
	}
	
	/**
	 * recalculate cell arrangement
	 * call if scale tone count or view dimensions change
	 */
	public void rearrange() {
		// allocate new cell state array
		cells = new int[scale.getToneCount()];

		super.rearrange();
		
		// generate new drawing parameters
		float sq = (float) Math.sqrt(cellWidth * cellHeight);
		textBrush.setTextSize(sq / 5f);
		txcent = -(textBrush.ascent() + textBrush.descent()) * 0.5f;
		box1 = sq / 6f;
		
		// redraw from new params
		postInvalidate();
	}
	
}
