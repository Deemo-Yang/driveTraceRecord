package com.example.drivetracerecord;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class MyFragment extends Fragment {

    private String content;
    private Button btn;

    public static MyFragment newInstance(String content) {
        MyFragment newFragment = new MyFragment();
        Bundle bundle = new Bundle();
        bundle.putString("content", content);
        newFragment.setArguments(bundle);
        return newFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_my_fragment,container,false);
        TextView txt_content = view.findViewById(R.id.txt_content);
        txt_content.setText(content);
        btn = view.findViewById(R.id.beginDrive);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
            }
        });
        return view;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        if (args != null) {
            content = args.getString("content");
        }
    }
}
