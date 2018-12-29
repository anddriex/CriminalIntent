package com.bignerdranch.android.criminalintent;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.bignerdranch.android.criminalintent.database.CrimeBaseHelper;
import com.bignerdranch.android.criminalintent.database.CrimeCursorWrapper;
import com.bignerdranch.android.criminalintent.database.CrimeDBSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.bignerdranch.android.criminalintent.database.CrimeDBSchema.*;

public class CrimeLab {
    private static CrimeLab sCrimeLab;
    private Context mContext;
    private SQLiteDatabase mDataBase;


    public static CrimeLab get(Context context) {
        if(sCrimeLab == null){
            sCrimeLab = new CrimeLab(context);
        }

        return sCrimeLab;
    }

    private CrimeLab(Context context){
        mContext = context.getApplicationContext();
        mDataBase = new CrimeBaseHelper(mContext)
                .getWritableDatabase();
    }

    public void addCrime(Crime crime) {
        ContentValues values = getContentValues(crime);

        mDataBase.insert(CrimeTable.NAME, null, values);
    }

    public void deleteCrime(Crime crime){

        mDataBase.delete(CrimeTable.NAME,
                CrimeTable.Cols.UUID + "= ?",
                new String[]{crime.getId().toString()}
                );

        System.out.println("CrimeLab.deleteCrime " + crime.getTitle() + " deleted");
    }

    public List<Crime> getCrimes(){
        List<Crime> crimes = new ArrayList<>();

        CrimeCursorWrapper cursor = queryCrimes(null, null);

        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                crimes.add(cursor.getCrime());
                cursor.moveToNext();
            }
        } finally{
            cursor.close();
        }

        return crimes;
    }

    public Crime getCrime(UUID id){
        CrimeCursorWrapper cursorWrapper = queryCrimes(
                CrimeTable.Cols.UUID + " = ?",
                new String[] {id.toString()}
        );

        try {
            if(cursorWrapper.getCount() == 0){
                return null;
            }

            cursorWrapper.moveToFirst();
            return cursorWrapper.getCrime();
        } finally {
          cursorWrapper.close();
        }
    }

    public void updateCrime(Crime crime){
        String uuidString = crime.getId().toString();
        ContentValues values = getContentValues(crime);

        mDataBase.update(CrimeTable.NAME, values,
                CrimeTable.Cols.UUID + "= ?",
                new String[] {uuidString});
        System.out.println("CrimeLab.updateCrime "+crime.getTitle() +" updated");
    }

    private CrimeCursorWrapper queryCrimes(String whereClause, String[] whereArgs){
        Cursor cursor = mDataBase.query(
                CrimeTable.NAME,
                null, //columns - null selects all columns
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        return new CrimeCursorWrapper(cursor);
    }

    private static ContentValues getContentValues(Crime crime){
        ContentValues values = new ContentValues();
        values.put(CrimeTable.Cols.UUID, crime.getId().toString());
        values.put(CrimeTable.Cols.TITLE, crime.getTitle());
        values.put(CrimeTable.Cols.DATE, crime.getDate().getTime());
        values.put(CrimeTable.Cols.SOLVED, crime.isSolved() ? 1 : 0);
        values.put(CrimeTable.Cols.SUSPECT, crime.getSuspect());

        return values;
    }
}
