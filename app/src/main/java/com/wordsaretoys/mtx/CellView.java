package com.wordsaretoys.mtx;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * displays a grid of cells in optimal arragement
 */
public class CellView extends View {

	static String TAG = "CellView";
	
	static float Gap = 4f;

	// brushes for drawing cells
	Paint fillBrush;
	
	// cell state array
	int[] cells = new int[0];
	
	// row and column count
	int rows, cols;
	
	// cell dimensions
	float cellWidth, cellHeight;
	
	// cell drawing rect
	RectF rect = new RectF();
	
	// useful colors
	int cellBackColor;

	/**
	 * ctor, used for XML instantiation
	 */
	public CellView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		Resources res = getResources();
		
		cellBackColor = res.getColor(R.color.Gray);
		
		fillBrush = new Paint();
		fillBrush.setStyle(Style.FILL);
	}

	/**
	 * handle resize of view
	 */
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		rearrange();
	}

	/**
	 * handle cell draw
	 * override in subclass
	 */
	protected void onDrawCell(Canvas canvas, int index, RectF rect) {}
	
	/**
	 * handle drawing of view
	 */
	protected void onDraw(Canvas canvas) {
		
		canvas.drawColor(Color.WHITE);
		
		for (int i = 0; i < cells.length; i++) {

			int r = i / cols;
			int c = i % cols;
			
			float top = Gap + r * (cellHeight + 2f * Gap);
			float left = Gap + c * (cellWidth + 2f * Gap);
			rect.set(left, top, left + cellWidth, top + cellHeight);
		
			// draw cell background
			fillBrush.setColor(cellBackColor);
			canvas.drawRect(rect, fillBrush);
			
			// call cell draw proc
			onDrawCell(canvas, i, rect);
		}
	}

	/**
	 * return row corresponding to y coordinate
	 */
	protected int getRow(float y) {
		return (int)(y / (cellHeight + 2f * Gap));		
	}
	
	/**
	 * return col corresponding to x coordinate
	 */
	protected int getCol(float x) {
		return (int)(x / (cellWidth + 2f * Gap));
	}

	/**
	 * recalculate cell arrangement
	 */
	public void rearrange() {
		// find optimal cell grid arrangement
		float w = getWidth(), h = getHeight();
		float diff = Integer.MAX_VALUE;
		for (int r = 1; r <= cells.length; r++) {
			// any remainder requires an additional column
			int c = (int) Math.ceil((float)cells.length / r);
			// max size available in each cell
			float cw = w / c - 2 * Gap;
			float ch = h / r - 2 * Gap;
			float d = Math.abs(cw - ch);
			// we want the cell to be as "square" as possible
			boolean t0 = d < diff;
			// and wider than it is tall (if permissible)
			boolean t1 = (cw >= ch) || (cells.length == 1);
			// and we can't allow "empty" rows
			boolean t2 = r * c - cells.length < c;
			// if criteria match, store off params
			if (t0 && t1 && t2) {
				diff = d;
				cols = c;
				rows = r;
				cellWidth = cw;
				cellHeight = ch;
			}
		}
		// redraw from new params
		postInvalidate();
	}	
}