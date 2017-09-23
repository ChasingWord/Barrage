package com.example.barrage;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

import com.example.barrage.barrage.Barrage;
import com.example.barrage.barrage.BarrageLayout;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private BarrageLayout mBl;
    private Button mBtnStart, mBtnPause, mBtnResume, mBtnAdd;
    private SeekBar mSeekBar;
    private EditText mEtInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnStart = (Button) findViewById(R.id.btn_start);
        mBtnPause = (Button) findViewById(R.id.btn_pause);
        mBtnResume = (Button) findViewById(R.id.btn_resume);
        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
        mBtnAdd = (Button) findViewById(R.id.btn_add);
        mEtInput = (EditText) findViewById(R.id.et_input);

        initView();
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBl.startScrollBarrage();
            }
        });
        mBtnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBl.pause();
            }
        });
        mBtnResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBl.resume();
            }
        });
        mBtnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = mEtInput.getText().toString();
                Barrage barrage = new Barrage();
                barrage.setMsg(s);
                barrage.setTime(mBl.getCurrentTime());
                mBl.addBarrage(barrage);
            }
        });
    }

    private int[] color = new int[]{android.R.color.holo_red_light
            , android.R.color.holo_green_light
            , android.R.color.holo_blue_light
            , android.R.color.holo_purple
            , android.R.color.holo_orange_dark
            , android.R.color.black};

    private void initView() {
        Random random = new Random(48);
        mBl = (BarrageLayout) findViewById(R.id.bl);
        ArrayList<Barrage> all = new ArrayList<>();
        for (int i = 1; i <= 500; i++) {
            Barrage barrage = new Barrage();
            barrage.setMsg("barrage " + i);
            barrage.setTime(i);
            barrage.setTextColorResId(color[random.nextInt(6)]);
            all.add(barrage);
            Barrage barrage1 = new Barrage();
            barrage1.setMsg("barragebarrage " + (i + 0.5));
            barrage1.setTextColorResId(color[random.nextInt(6)]);
            barrage1.setTime((int) (i + 0.5));
            all.add(barrage1);
        }
        mBl.setData(all);
        mBl.setTotalTime(all.get(all.size() - 1).getTime() + 200);
        mSeekBar.setMax((int) all.get(all.size() - 1).getTime() + 200);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mBl.setCurrentTime(seekBar.getProgress());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBl.destroy();
    }
}
