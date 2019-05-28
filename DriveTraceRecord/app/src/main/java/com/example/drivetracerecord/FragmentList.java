package com.example.drivetracerecord;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import util.DbAdapter;
import util.PathRecord;


public class FragmentList extends Fragment implements AdapterView.OnItemClickListener {


    private RecordAdapter mAdapter;
    private ListView mAllRecordListView;
    private DbAdapter mDataBaseHelper;
    private List<PathRecord> mAllRecord = new ArrayList<PathRecord>();
    public static final String RECORD_ID = "record_id";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.recordlist, container, false);


        mAllRecordListView = view.findViewById(R.id.recordlist);

        mDataBaseHelper = new DbAdapter(getActivity());
        mDataBaseHelper.open();
        searchAllRecordFromDB();


        mAdapter = new RecordAdapter(getActivity(), mAllRecord);
        mAllRecordListView.setAdapter(mAdapter);
        mAllRecordListView.setOnItemClickListener(this);
        return view;
    }

    private void searchAllRecordFromDB() {
        mAllRecord = mDataBaseHelper.queryRecordAll();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        PathRecord recorditem = (PathRecord) parent.getAdapter().getItem(
                position);
        Intent intent = new Intent(getActivity(),TrackSearchActivity.class);
        intent.putExtra("trackID", recorditem.getmTrackID());
        startActivity(intent);
    }

}
