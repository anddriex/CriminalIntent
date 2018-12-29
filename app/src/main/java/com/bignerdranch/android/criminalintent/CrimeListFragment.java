package com.bignerdranch.android.criminalintent;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class CrimeListFragment extends Fragment {

    private static final String SAVED_SUBTITLE_VISIBLE = "subtitle";

    @BindView(R.id.crime_recyler_view) RecyclerView mCrimeRecyclerView;


    private CrimeAdapter mAdapter;
    private Unbinder unbinder;
    private boolean mSubtitleVisible;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                                                @Nullable ViewGroup container,
                                                @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragmet_crime_list, container, false);
        unbinder = ButterKnife.bind(this, view);
        /*layOutManager positions every item and also defines how scrolling works*/
        mCrimeRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if(savedInstanceState != null){
            mSubtitleVisible =
                    savedInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE);
        }

        updateUI();

        return  view;


    }

    @Override public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_SUBTITLE_VISIBLE, mSubtitleVisible);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime_list, menu);

        MenuItem subtitleItem = menu.findItem(R.id.show_subtitle);
        if(mSubtitleVisible){
            subtitleItem.setTitle(R.string.hide_subtittle);
        } else {
            subtitleItem.setTitle(R.string.show_subtittle);
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {

            case R.id.new_crime:
                Crime crime = new Crime();
                CrimeLab.get(getActivity()).addCrime(crime);
                Intent intent = CrimePagerActivity
                        .newIntent(getActivity(), crime.getId());
                startActivity(intent);
                return true;

            case R.id.show_subtitle:
                mSubtitleVisible = !mSubtitleVisible;
                getActivity().invalidateOptionsMenu();
                updateSubtitle();
                return true;

                default:
                    return super.onOptionsItemSelected(item);
        }
    }

    private void updateSubtitle() {
        CrimeLab crimeLab = CrimeLab.get(getActivity());
        int crimeCount = crimeLab.getCrimes().size();
        String subtitle  = getString(R.string.subtittle_format, crimeCount);

        if(!mSubtitleVisible){
            subtitle = null;
        }
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setSubtitle(subtitle);
    }

    private void updateUI() {
        CrimeLab crimeLab = CrimeLab.get(getActivity()); // CrimeLab es un singleton
        List<Crime> crimes = crimeLab.getCrimes();


        if(mAdapter == null){
            mAdapter = new CrimeAdapter(crimes);
            mCrimeRecyclerView.setAdapter(mAdapter);
        }else {
            mAdapter.setCrimes(crimes);
            mAdapter.notifyDataSetChanged();
        }
        updateSubtitle();
    }

    // Crime holder: funcion sostener una View
    protected class CrimeHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.crime_title) TextView mTitleTextView;
        @BindView(R.id.crime_date) TextView mDateTextView;
        @BindView(R.id.crime_solved) ImageView mSolvedImageView;

        private Crime mCrime;

        public CrimeHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_crime, parent, false));
            itemView.setOnClickListener(this);
            ButterKnife.bind(this, itemView);//funcionara? claro que si
        }

        public void bind(Crime crime) {
            mCrime = crime;
            mTitleTextView.setText(mCrime.getTitle());
            mDateTextView.setText(mCrime.getDate().toString());
            mSolvedImageView.setVisibility(crime.isSolved() ? View.VISIBLE : View.GONE);
        }

        @Override public void onClick(View v) {
            Intent intent = CrimePagerActivity.newIntent(getActivity(), mCrime.getId());
            Log.d("llamada", "Onclick list id: " + mCrime.getId());
            startActivity(intent);
        }
    }

    // encargado de crear los viewholder posicionarlos y cambiar su contenido.
    private class CrimeAdapter extends RecyclerView.Adapter<CrimeHolder> {
        private List<Crime> mCrimes;

        public CrimeAdapter(List<Crime> crimes) {
            mCrimes = crimes;
        }

        @NonNull@Override public CrimeHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new CrimeHolder(layoutInflater, parent);

        }

        @Override public void onBindViewHolder(@NonNull CrimeHolder crimeHolder, int position) {
            Crime crime = mCrimes.get(position);
            crimeHolder.bind(crime);
        }

        @Override public int getItemCount() {
            return mCrimes.size();
        }

        public void setCrimes(List<Crime> crimes){
            mCrimes = crimes;
        }
    }


    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
