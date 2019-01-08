package com.bignerdranch.android.criminalintent;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.w3c.dom.Text;

import java.text.ChoiceFormat;
import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import butterknife.Unbinder;

import static android.provider.ContactsContract.CommonDataKinds.*;
import static android.support.v4.app.ShareCompat.*;
import static android.widget.CompoundButton.*;

public class CrimeFragment extends Fragment {
    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";
    private static final String TRACK = "track";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_READ_CONTACTS = 2;

    private Crime mCrime;
    private Unbinder unbinder;
    private String mSuspectId = null;
    private String mPhone;
    private final Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);

    @BindView(R.id.crime_title) EditText mTitleField;
    @BindView(R.id.crime_solved) CheckBox mSolvedCheckBox;
    @BindView(R.id.crime_date) Button mDateButton;
    @BindView(R.id.crime_report) Button mReportButton;
    @BindView(R.id.crime_suspect) Button mSuspectButton;
    @BindView(R.id.crime_call) Button mCallButton;

    @OnClick(R.id.crime_call)
    public void callSuspect(){
        queryPhone();
        dialUp();
    }

    @OnClick(R.id.crime_date)
    public void openDatePicker() {
        FragmentManager manager = getFragmentManager();
        DatePickerFragment dialog = DatePickerFragment
                .newInstance(mCrime.getDate());
        dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
        dialog.show(manager, DIALOG_DATE);
    }

    @OnCheckedChanged(R.id.crime_solved)
    public void crimeSolver(boolean isChecked){
        mCrime.setSolved(isChecked);
    }

    @OnTextChanged(R.id.crime_title)
    public void changeText(CharSequence s, int start, int before, int count){
        mCrime.setTitle(s.toString());
    }

    @OnClick(R.id.crime_report)
    public void sendReport(View v){
        IntentBuilder intentB = IntentBuilder.from(getActivity());
        intentB.setType("text/plain");
        intentB.setChooserTitle(R.string.send_report);
        intentB.setSubject(getString(R.string.crime_report_subject));
        intentB.setText(getCrimeReport());
        Intent i = intentB.createChooserIntent();
        startActivity(i);
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
    }

    @Override
    public void onPause() {
        super.onPause();

        CrimeLab.get(getActivity())
                .updateCrime(mCrime);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_crime, container, false);
        unbinder = ButterKnife.bind(this, v);
        mTitleField.setText(mCrime.getTitle());
        updateDate();
        mSolvedCheckBox.setChecked(mCrime.isSolved());


        mSuspectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){

                    if(ContextCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_CONTACTS},
                                REQUEST_READ_CONTACTS);
                    }else{
                        startActivityForResult(pickContact, REQUEST_CONTACT);
                    }
                }

            }
        });

        if(mCrime.getSuspect() != null){
            mSuspectButton.setText(mCrime.getSuspect());
        } else {
            mCallButton.setEnabled(false);
        }

        PackageManager packageManager = getActivity().getPackageManager();

        if(packageManager.resolveActivity(pickContact,
                PackageManager.MATCH_DEFAULT_ONLY) == null){
            mSuspectButton.setEnabled(false);
        }
        return  v;
    }


    public static CrimeFragment newInstance(UUID crimeId){
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);
        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK) {
            return;
        }

        if(requestCode == REQUEST_DATE){
            Date date = (Date) data
                    .getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            updateDate();

        } else if(requestCode == REQUEST_CONTACT && data != null){
            Uri contactUri = data.getData();

            // Specify wich fields you want your query return
            // values for
            String[] queryFields = new String[] {
                    ContactsContract.Contacts.DISPLAY_NAME
            };



            // Perform your query - the contactUri is like a "where"
            // clause here
            Cursor c = getActivity().getContentResolver()
                    .query(contactUri, queryFields, null, null, null);


            try{
                // Double-check that you actually got results
                if(c.getCount() == 0){
                    return;
                }

                // Pull out the first column of the first row of data -
                // that is your suspect's name
                c.moveToFirst();
                String suspect = c.getString( c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                mCrime.setSuspect(suspect);
                mSuspectButton.setText(suspect);
                mCallButton.setEnabled(true);
            } finally {
                c.close();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime, menu);


    }


    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.delete_crime:
                CrimeLab.get(getActivity()).deleteCrime(mCrime);
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateDate() {
        mDateButton.setText(mCrime.getDate().toString());
    }

    private String getCrimeReport(){
        String solvedString = null;
        if(mCrime.isSolved()){
            solvedString = getString(R.string.crime_report_solved);
        } else{
            solvedString = getString(R.string.crime_report_unsolved);
        }

        String dateFormat = "EEE, MMM dd";
        String dateString = android.text.format.DateFormat.format(dateFormat,
                mCrime.getDate()).toString();

        String suspect = mCrime.getSuspect();
        if(suspect==null){
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }

        String report = getString(R.string.crime_report,
                mCrime.getTitle(), dateString, solvedString, suspect);

        return report;
    }

    private void queryPhone(){

//        String[] projection = new String[]{
//                Phone.NUMBER
//        };
//
//        String phone = null;
//        Log.d(TRACK, "Suspect id >>>>>>>>>>>>>>>>> " + mSuspectId);
//        if(mSuspectId != null){
//            Cursor cPhone = getActivity().getContentResolver()
//                    .query(Phone.CONTENT_URI,
//                            projection,
//                            Phone._ID + " = ?",
//                            new String[]{mSuspectId},
//                            null);
//            Log.d(TRACK, "count IS this -----------------------------> " + cPhone.getCount());
//            try {
//                if (cPhone.getCount() == 0){
//                    return phone;
//                }
//
//                cPhone.moveToFirst();
//                phone = cPhone.getString(cPhone.getColumnIndex(Phone.NUMBER));
//                Log.d(TRACK, "THE PHONE ?????????? <<<<<<< <<<<<<< " + phone);
//            }finally {
//                cPhone.close();
//            }
//        }

        ContentResolver cr = getActivity().getContentResolver();

        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
                "DISPLAY_NAME ='" + mCrime.getSuspect() + "'",
                null,
                null);
        if(cursor.moveToFirst()){
            String contactd = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

            Cursor phones = cr.query(Phone.CONTENT_URI, null,
                    Phone.CONTACT_ID + " = " + contactd, null,null);
            while(phones.moveToNext()){
                String number = phones.getString(phones.getColumnIndex(Phone.NUMBER));
                int type = phones.getInt(phones.getColumnIndex(Phone.TYPE));
                switch (type){
                    case Phone.TYPE_MOBILE:{
                        mPhone = number;
                        return;
                    }
                    case Phone.TYPE_HOME:{
                        // so something with home number
                    }
                    case Phone.TYPE_WORK:{
                        // something with work number
                    }
                }
            }
            phones.close();
        }
        cursor.close();
    }

    private void dialUp(){
        Log.d(TRACK, "The number >>>>>>>>>>>>>>>>>>>>>>>> " + mPhone);
        if(mPhone != null){
            Uri numberUri = Uri.parse("tel:" + mPhone);
            final Intent callPhone = new Intent(Intent.ACTION_DIAL,numberUri);
            startActivity(callPhone);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_READ_CONTACTS: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startActivityForResult(pickContact, REQUEST_CONTACT);
                }
                return;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
