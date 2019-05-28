package com.vshows.imusic;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by Vshows on 2018/4/27.
 */

public class Local_music_list extends AppCompatActivity {



    public ImageView image;
    public TextView music_title;
    public TextView singer;
    public Button control_play;
    public Button control_list;

    private MusicScanning scanner;
    private ListView listview;
    private List<MusicItem> music_list;
    private List<MusicItem> play_list;
    public Intent play_index;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.local_music_list);
        Log.i("zxc","start--------------------------------start-------");

        this.setTitle(R.string.local_title_name);

        music_list = new ArrayList<MusicItem>();
        listview = (ListView)findViewById(R.id.music_list);
        MusicAdapter adapter = new MusicAdapter(this, R.layout.music_item, music_list);
        listview.setAdapter(adapter);
        Intent i = new Intent(this, MusicService.class);
        startService(i);
        //实现绑定操作
        bindService(i, mServiceConnection, BIND_AUTO_CREATE);
        listview.setOnItemClickListener(OnMusicItemClickListener);
        scanner = new MusicScanning();
        scanner.execute();

        music_title = (TextView)findViewById(R.id.control_name);
        singer  = (TextView)findViewById(R.id.control_singer);
        image = (ImageView)findViewById(R.id.control_thumb);
        control_play = (Button)findViewById(R.id.control_play);
        control_play.setBackgroundResource(R.mipmap.start_test);
        control_list = (Button)findViewById(R.id.control_play_list);
        control_list.setBackgroundResource(R.mipmap.list);

    }


    public void update_control(){
        MusicItem item = music_binder.getCurrentMusic();
        if(image!=null){
            if(item.thumb!=null){
                image.setImageBitmap(item.thumb);
            }
            else {
                image.setImageResource(R.mipmap.thumb);
            }
        }
        music_title.setText(item.name);
        singer.setText(item.singer);
    }
    public void update_control(MusicItem item){

        if(image!=null){
            if(item.thumb!=null){
                image.setImageBitmap(item.thumb);
            }
            else {
                image.setImageResource(R.mipmap.thumb);
            }
        }
        music_title.setText(item.name);
        singer.setText(item.singer);
    }


    protected void onDestroy() {
        super.onDestroy();
        if(scanner!=null&&scanner.getStatus()== AsyncTask.Status.RUNNING){
            scanner.cancel(true);
        }
        scanner=null;
        music_binder.unregisterOnStateChangeListener(mStateChangeListenr);
        music_list.clear();
        music_binder.clear_list();
        unbindService(mServiceConnection);
    }

    private AdapterView.OnItemClickListener OnMusicItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            MusicItem item = music_list.get(i);
            if(music_binder != null) {
                if(!music_binder.getPlayList().contains(item)){
                    music_binder.addPlayList(music_list.get(i));


                        music_binder.play();

                    control_play.setBackgroundResource(R.mipmap.pause_test);

                }
                else{

                    music_binder.change_to_first(item);
                    music_binder.change_current_item();
                    Log.i("now","1");
                    music_binder.play();
                    control_play.setBackgroundResource(R.mipmap.pause_test);
                }

                update_control();



            }
        }
    };

    private MusicService.OnStateChangeListenr mStateChangeListenr = new MusicService.OnStateChangeListenr() {

        @Override
        public void onPlayProgressChange(MusicItem item) {
            update_control();

        }

        @Override
        public void onPlay(MusicItem item) {
            update_control();
        }

        @Override
        public void onPause(MusicItem item) {
            update_control();
        }
    };


    public void onClick(View view) {
        switch (view.getId()){
            case R.id.control_play:{
                if(music_binder != null) {
                    if(music_binder.getCurrentMusic()!=null){
                        if(!music_binder.isPlaying()) {
                            music_binder.play();
                            control_play.setBackgroundResource(R.mipmap.pause_test);
                            update_control();
                        }
                        else {
                            music_binder.pause();
                            control_play.setBackgroundResource(R.mipmap.start_test);
                            update_control();
                        }
                    }

                }
            }
            break;
            case R.id.control_play_list:{
                //play_index = new Intent(this,Play_inner.class);


                //startActivity(play_index);
                if(music_binder!=null){
                    showPlayList();
                }

            }
            break;
            case R.id.control_name :{
                play_index = new Intent(this,Play_inner.class);
                play_index.putExtra("data",new Gson().toJson(this));
                startActivity(play_index);
            }

            break;
            case R.id.control_singer :{
                play_index = new Intent(this,Play_inner.class);
                //play_index.putExtra("data",this);
                startActivity(play_index);
            }

            break;

            case  R.id.control_panel:{
                play_index = new Intent(this,Play_inner.class);


                startActivity(play_index);
            }

        }
    }

    private class MusicScanning extends AsyncTask<Object, MusicItem, Void> {


        @Override
        protected Void doInBackground(Object... objects) {
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] searchKey = new String[] {
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Albums.ALBUM_ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION
            };
            String where = MediaStore.Audio.Media.DATA+" like \"%"+"/music"+"%\"";
            String [] keywords = null;
            String sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(uri, searchKey, where, keywords, sortOrder);
            if(cursor!=null){
                while(cursor.moveToNext() && ! isCancelled()){
                    String singer = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    Uri musicUri = Uri.withAppendedPath(uri, id);
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    int albumId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ID));
                    Uri albumUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
                    MusicItem data = new MusicItem(musicUri, albumUri, name, singer,duration, 0/*, false*/);
                    if (uri != null) {
                        ContentResolver res = getContentResolver();
                        data.thumb = Utils.createThumbFromUir(res, albumUri);
                    }
                    publishProgress(data);
                }
                cursor.close();
            }

            return null;
        }


        protected void onProgressUpdate(MusicItem... values) {

            MusicItem data = values[0];

            //这是主线程，在这里把要显示的音乐添加到音乐的展示列表当中。
            music_list.add(data);
            MusicAdapter adapter = (MusicAdapter) listview.getAdapter();
            adapter.notifyDataSetChanged();
        }
    }

    private MusicService.MusicSeviceIBinder music_binder;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {

            music_binder = (MusicService.MusicSeviceIBinder) service;
            music_binder.registerOnStateChangeListener(mStateChangeListenr);
        }

        public void onServiceDisconnected(ComponentName name) {

        }
    };

    public MusicService.MusicSeviceIBinder getBinder(){
        return music_binder;
    }




    private void showPlayList(){
        final AlertDialog.Builder builder=new AlertDialog.Builder(this);
        //builder.setIcon(R.mipmap.ic_playlist);
        builder.setTitle("歌曲播放列表");


        List<MusicItem> playList = music_binder.getPlayList();
        ArrayList<String> data = new ArrayList<String>();
        for(MusicItem music : playList) {
            data.add(music.name);
        }
        if(data.size() > 0) {

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data);
            builder.setAdapter(adapter, null);
        }
        else {

            builder.setMessage("播放列表无音乐");
        }

        builder.setCancelable(true);


        builder.create().show();
    }
}
