package com.vshows.imusic;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by Vshows on 2018/5/27.
 */

public class Play_inner extends AppCompatActivity {
    Local_music_list l;
    ImageView image;
    private Imgae_change imagess;
    private Button play_start;
    private SeekBar MusicSeekBar;
    private TextView duration_time;
    private TextView played_time;
    private MusicService.MusicSeviceIBinder music_binder;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {

            music_binder = (MusicService.MusicSeviceIBinder) service;

            music_binder.registerOnStateChangeListener(mStateChangeListenr);
        }

        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private MusicService.OnStateChangeListenr mStateChangeListenr = new MusicService.OnStateChangeListenr() {

        @Override
        public void onPlayProgressChange(MusicItem item) {
            update_infomation(item);

        }

        @Override
        public void onPlay(MusicItem item) {
            update_infomation(item);
        }

        @Override
        public void onPause(MusicItem item) {
            update_infomation(item);
        }
    };

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.play);
        Log.i("inner","inner");
        image = (ImageView)findViewById(R.id.play_back);
        image.setImageResource(R.mipmap.back);
        Intent i = new Intent(this, MusicService.class);
        startService(i);
        //实现绑定操作
        bindService(i, mServiceConnection, BIND_AUTO_CREATE);
        duration_time = (TextView)findViewById(R.id.duration_time) ;
        played_time = (TextView)findViewById(R.id.played_time);
        MusicSeekBar = (SeekBar)findViewById(R.id.seek_music);
        play_start = (Button)findViewById(R.id.play_btn) ;
        MusicSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        //imagess = new Imgae_change();
        //imagess.execute();
        //this.setTitle(music_binder.getCurrentMusic().name);
        //Intent intent = getIntent();
        //String JsonData = intent.getStringExtra("data");
        //l = new Gson().fromJson(JsonData,Local_music_list.class);
    }

    protected void onDestroy() {

        super.onDestroy();
    }


    public void update_image(){

    }


    public void update_infomation(MusicItem item){
        String times = Utils.convertMSecendToTime(item.duration);
        duration_time.setText(times);

        times = Utils.convertMSecendToTime(item.playedTime);
        played_time.setText(times);

        MusicSeekBar.setMax((int) item.duration);
        MusicSeekBar.setProgress((int) item.playedTime);

        this.setTitle(item.name);
        if(item.thumb!=null){
            image.setImageBitmap(item.thumb);
        }
        else {
            image.setImageResource(R.mipmap.back);
        }

    }


    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            if(music_binder != null) {
                music_binder.seekTo(seekBar.getProgress());
            }
        }
    };
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.pre_btn:{
                music_binder.playPre();
                Log.i("test","test");

                update_infomation(music_binder.getCurrentMusic());
            }
            break;
            case R.id.play_btn:{
                if(music_binder.isPlaying()==true){
                    music_binder.pause();
                    play_start.setBackgroundResource(R.mipmap.start);

                }
                else {
                    music_binder.play();
                    play_start.setBackgroundResource(R.mipmap.pause);

                }
                update_infomation(music_binder.getCurrentMusic());


            }
            break;
            case R.id.next_btn:{
                music_binder.playNext();
                update_infomation(music_binder.getCurrentMusic());
            }
            break;

        }
    }

    public class Imgae_change extends AsyncTask<Object,Void , Void> {

        @Override
        protected Void doInBackground(Object... objects) {
            update_image();
            return null;
        }
    }

}
