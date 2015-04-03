package com.wordsaretoys.mtx;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * interface to the persisent scale data
 */
public enum Storage {

	Me;
	
	// debug cert 
	private static final X500Principal DEBUG_DN = 
			new X500Principal("CN=Android Debug,O=Android,C=US");

	static String DbName = "mtx";
	static int SchemaVersion = 1;
	
	// table names
	static String T_SCALE = "scale";
	
	// field names
	static String F_ID = "id";
	static String F_NAME = "name";
	static String F_STEPS = "steps";
	
	// table-field definitions
	static String[] TF_SCALE = {
		F_ID, F_NAME, F_STEPS
	};
	static String[] TY_SCALE = {
		"integer primary key", "text", "text"
	};
	
	/**
	 * database helper class
	 * assists with database creation and opening
	 */
	class DatabaseHelper extends SQLiteOpenHelper {
		
		public DatabaseHelper(Context context) {
			super(context, "mtx", null, SchemaVersion);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.beginTransaction();
			db.execSQL(getTableCreateString(T_SCALE, TF_SCALE, TY_SCALE));
			db.execSQL("INSERT INTO " + T_SCALE + " VALUES(0, 'Major', '2,2,1,2,2,2,1');");
			db.execSQL("INSERT INTO " + T_SCALE + " VALUES(1, 'Natural Minor', '2,1,2,2,1,2,2');");
			db.execSQL("INSERT INTO " + T_SCALE + " VALUES(2, 'Pentatonic Major', '2,2,3,2,3');");
			db.execSQL("INSERT INTO " + T_SCALE + " VALUES(3, 'Pentatonic Minor', '3,2,2,3,2');");
			db.setTransactionSuccessful();
			db.endTransaction();
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
		
	}

	/**
	 * listview adapter for retrieving scale names
	 */
	class ScaleAdapter extends BaseAdapter {

		Cursor cursor;
		int colId, colName;
		
		/**
		 * ctor, creates scale table cursor
		 */
		public ScaleAdapter() {
			cursor = dbHelper.getReadableDatabase()
					.query(T_SCALE, TF_SCALE, null, null, null, null, F_NAME);
			colId = cursor.getColumnIndex(F_ID);
			colName = cursor.getColumnIndex(F_NAME);
		}
		
		@Override
		public int getCount() {
			return cursor.getCount();
		}

		@Override
		public Object getItem(int position) {
			cursor.moveToPosition(position);
			return cursor.getString(colName);
		}

		@Override
		public long getItemId(int position) {
			cursor.moveToPosition(position);
			return cursor.getLong(colId);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater
						.from(context)
						.inflate(android.R.layout.simple_list_item_1, null); 
			}
			((TextView)convertView).setText((String)getItem(position));
			return convertView;
		}
		
	}
	
	// database helper object
	DatabaseHelper dbHelper;
	
	// activity context
	Context context;
	
	/**
	 * creates/opens database
	 */
	public void onCreate(Context context) {
		this.context = context;
		dbHelper = new DatabaseHelper(context);
	}
	
	/**
	 * return a new scale spinner adapter
	 */
	public ScaleAdapter getAdapter() {
		return new ScaleAdapter();
	}
	
	/**
	 * write a scale to the 'base
	 */
	public void write(Scale scale) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		db.beginTransaction();
		try {
			values.put(F_NAME, scale.name);
			values.put(F_STEPS, scale.toString());
			if (scale.id == -1) {
				scale.id = db.insert(T_SCALE, null, values);
			} else {
				String[] args = { String.valueOf(scale.id) };
				db.update(T_SCALE, values, F_ID + "= ?", args);
			}
			db.setTransactionSuccessful();
			scale.dirty = false;
		} catch (SQLiteException e) {
			Toast.makeText(context, 
				R.string.toastSaveFailed, Toast.LENGTH_LONG).show();
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}
	
	/**
	 * read a scale from the 'base
	 */
	public void read(long id, Scale scale) {
		String[] args = { String.valueOf(id) };
		Cursor cursor = dbHelper
			.getReadableDatabase()
			.query(T_SCALE, TF_SCALE, F_ID + "=?", args, null, null, null);
		if (cursor.moveToFirst()) {
			scale.id = id;
			scale.name = cursor.getString(cursor.getColumnIndex(F_NAME));
			scale.load(cursor.getString(cursor.getColumnIndex(F_STEPS)));
		} else {
			scale.id = -1;
			scale.name = "";
		}
		cursor.close();
		scale.dirty = false;
	}

	/**
	 * delete a scale from the 'base
	 */
	public void delete(long id) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			String[] args = { String.valueOf(id) };
			db.delete(T_SCALE, F_ID + "= ?", args);
			db.setTransactionSuccessful();
		} catch (SQLiteException e) {
			Toast.makeText(context, 
					R.string.toastDeleteFailed, Toast.LENGTH_LONG).show();
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}
	
	/**
	 * get debug cert status of app
	 * @param ctx context
	 * @return true if app built with debug cert
	 */
	public static boolean isDebuggable(Context ctx)
	{
	    boolean debuggable = false;

	    try
	    {
	        PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),PackageManager.GET_SIGNATURES);
	        Signature signatures[] = pinfo.signatures;

	        CertificateFactory cf = CertificateFactory.getInstance("X.509");

	        for ( int i = 0; i < signatures.length;i++)
	        {   
	            ByteArrayInputStream stream = new ByteArrayInputStream(signatures[i].toByteArray());
	            X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);       
	            debuggable = cert.getSubjectX500Principal().equals(DEBUG_DN);
	            if (debuggable)
	                break;
	        }
	    }
	    catch (NameNotFoundException e)
	    {
	        //debuggable variable will remain false
	    }
	    catch (CertificateException e)
	    {
	        //debuggable variable will remain false
	    }
	    return debuggable;
	}	
	
	
	/**
	 * generates a table-creation string
	 */
	String getTableCreateString(String table, String[] names, String[] types) {
		String s = "CREATE TABLE " + table + "(";
		for (int i = 0; i < names.length; i++) {
			if (i > 0) {
				s += ",";
			}
			s += names[i] + " " + types[i];
		}
		s += ");";
		return s;
	}
}
