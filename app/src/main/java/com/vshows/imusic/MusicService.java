package com.vshows.imusic;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Vshows on 2018/4/26.
 */

public class MusicService extends Service{

    private MediaPlayer music_player;
    private LinkedList<MusicItem> play_list;
    private ContentResolver resolver;

    //当前是否为播放暂停状态
    private boolean paused;
    //存放当前要播放的音乐
    private MusicItem current_musicitem;
    //定义循环发送的消息
    private final int MSG_PROGRESS_UPDATE = 0;
    private MediaPlayer.OnCompletionListener OnCompletionListener = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            //将当前播放的音乐记录时间重置为0，更新到数据库
            //下次播放就可以从头开始
            current_musicitem.playedTime = 0;
            updateMusicItem(current_musicitem);
            //播放下一首音乐
            playNextInner();
        }
    };



    //注册监听器的接口
    private List<OnStateChangeListenr> ListenerList = new ArrayList<OnStateChangeListenr>();
    public interface OnStateChangeListenr {

        void onPlayProgressChange(MusicItem item);
        void onPlay(MusicItem item);
        void onPause(MusicItem item);
    }
    private Handler mHandler = new Handler() {


        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PROGRESS_UPDATE: {
                    //将音乐的时长和当前播放的进度保存到MusicItem数据结构中，
                    current_musicitem.playedTime = music_player.getCurrentPosition();
                    current_musicitem.duration = music_player.getDuration();

                    //通知监听者当前的播放进度
                    for(OnStateChangeListenr l : ListenerList) {
                        l.onPlayProgressChange(current_musicitem);
                    }

                    //将当前的播放进度保存到数据库中
                    updateMusicItem(current_musicitem);

                    //间隔一秒发送一次更新播放进度的消息
                    sendEmptyMessageDelayed(MSG_PROGRESS_UPDATE, 1000);
                }
                break;
            }
        }
    };
    private void updateMusicItem(MusicItem item) {

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.DURATION, item.duration);
        cv.put(DBHelper.LAST_PLAY_TIME, item.playedTime);

        String strUri = item.SongUri.toString();
        resolver.update(MyContentProvider.CONTENT_SONGS_URI, cv, DBHelper.SONG_URI + "=\"" + strUri + "\"", null);
    }

    public void onCreate(){
        super.onCreate();
        resolver = getContentResolver();
        play_list = new LinkedList<MusicItem>();
        music_player = new MediaPlayer();
        music_player.setOnCompletionListener(OnCompletionListener);
        paused = false;
        if(current_musicitem != null) {

            prepareToPlay(current_musicitem);
        }
        init_play_list();
    }
    public void onDestroy(){
        super.onDestroy();
        music_player.release();
        mHandler.removeMessages(MSG_PROGRESS_UPDATE);
        ListenerList.clear();
        play_list.clear();

    }
    private final IBinder music_binder = new MusicSeviceIBinder();
    public IBinder onBind(Intent intent){
        return music_binder;
    }
    public class MusicSeviceIBinder extends Binder{

        public void clear_list(){
            inner_clear_list();
        }

        public void addPlayList(MusicItem item) {
            addPlayListInner(item,true);
        }

        public void play() {
            playInner();

        }

        public void playNext() {
            playNextInner();

        }

        public void change_current_item(){
            change_current_item_inner();
        }

        public void playPre() {
            playPreInner();
        }

        public void pause() {
            pauseInner();
        }

        public void seekTo(int pos) {
            seekToInner(pos);
        }

        public void registerOnStateChangeListener(OnStateChangeListenr l) {
            registerOnStateChangeListenerInner(l);

        }

        public void unregisterOnStateChangeListener(OnStateChangeListenr l) {
            unregisterOnStateChangeListenerInner(l);
        }


        public MusicItem getCurrentMusic() {
            return getCurrentMusicInner();
        }

        public boolean isPlaying() {
            return isPlayingInner();
        }

        public List<MusicItem> getPlayList() {
            return play_list;
        }

        public void change_to_first(MusicItem item){
            int position = play_list.indexOf(item);
            play_list.remove(position);
            play_list.addFirst(item);

        }
    }


    public void inner_clear_list(){


    }

    public void addPlayListInner(MusicItem item,boolean needplay) {
        if(play_list.contains(item)){
            return;
        }
        play_list.add(0,item);
        insertMusicItemToContentProvider(item);
        if(needplay) {
            //添加完成后，开始播放
            current_musicitem = play_list.get(0);
            playInner();
        }
    }

    public void change_current_item_inner(){
        this.current_musicitem = null;
    }
    public void playNextInner() {
        int currentIndex = play_list.indexOf(current_musicitem);
        if(currentIndex < play_list.size() -1 ) {
            //获取当前播放（或者被加载）音乐的下一首音乐
            //如果后面有要播放的音乐，把那首音乐设置成要播放的音乐
            //并重新加载该音乐，开始播放
            current_musicitem = play_list.get(currentIndex + 1);
            playMusicItem(current_musicitem, true);
        }
    }


    public void playInner() {
        //如果之前没有选定要播放的音乐，就选列表中的第一首音乐开始播放
        if(current_musicitem == null && play_list.size() > 0)
            {
                current_musicitem = play_list.get(0);
            }
        Log.i("music",current_musicitem.name);
            //如果是从暂停状态恢复播放音乐，那么不需要重新加载音乐；
            //如果是从完全没有播放过的状态开始播放音乐，那么就需要重新加载音乐
            if(paused) {
                playMusicItem(current_musicitem, false);
            }
            else {
                playMusicItem(current_musicitem, true);
            }
        }

    public void playPreInner() {
        int currentIndex = play_list.indexOf(current_musicitem);
        if(currentIndex - 1 >= 0 ) {
            //获取当前播放（或者被加载）音乐的上一首音乐
            //如果前面有要播放的音乐，把那首音乐设置成要播放的音乐
            //并重新加载该音乐，开始播放
            current_musicitem = play_list.get(currentIndex - 1);
            playMusicItem(current_musicitem, true);
        }
    }


    public void pauseInner() {
        //暂停当前正在播放的音乐
        music_player.pause();
        mHandler.removeMessages(MSG_PROGRESS_UPDATE);
        //将播放状态的改变通知给监听者
        for(OnStateChangeListenr l : ListenerList) {
            l.onPause(current_musicitem);
        }
        //设置为暂停播放状态
        paused = true;
    }

    public void seekToInner(int pos) {
        music_player.seekTo(pos);
    }

    public void registerOnStateChangeListenerInner(OnStateChangeListenr l) {
        ListenerList.add(l);
    }

    public void unregisterOnStateChangeListenerInner(OnStateChangeListenr l) {
        ListenerList.remove(l);
    }


    public MusicItem getCurrentMusicInner() {
        return current_musicitem;
    }

    public boolean isPlayingInner() {
        return music_player.isPlaying();
    }

    private void insertMusicItemToContentProvider(MusicItem item) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.NAME, item.name);
        cv.put(DBHelper.SINGER,item.singer);
        cv.put(DBHelper.DURATION, item.duration);
        cv.put(DBHelper.LAST_PLAY_TIME, item.playedTime);
        cv.put(DBHelper.SONG_URI, item.SongUri.toString());
        cv.put(DBHelper.ALBUM_URI, item.AlbumUri.toString());
        Uri uri = resolver.insert(MyContentProvider.CONTENT_SONGS_URI, cv);

    }

    private void deleteMusicItemFromContentProvider(){

    }

    public void init_play_list(){
        play_list.clear();
        Cursor cursor = resolver.query(MyContentProvider.CONTENT_SONGS_URI,
                null,
                null,
                null,
                null);
        if( play_list.size() > 0) {
            current_musicitem = play_list.get(0);
        }
        while(cursor.moveToNext()){
            String singer = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.SINGER));
            String songUri = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.SONG_URI));
            String albumUri = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.ALBUM_URI));
            String name = cursor.getString(cursor.getColumnIndex(DBHelper.NAME));
            long playedTime = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LAST_PLAY_TIME));
            long duration = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.DURATION));
            MusicItem item = new MusicItem(Uri.parse(songUri), Uri.parse(albumUri), name,singer, duration, playedTime);
            play_list.add(item);
        }
        cursor.close();
        if( play_list.size() > 0) {
            current_musicitem = play_list.get(0);
        }
    }

    //将要播放的音乐载入MediaPlayer，但是并不播放
    private void prepareToPlay(MusicItem item) {
        try {
            //重置播放器状态
            Log.i("load",item.name);
           music_player.reset();
            //设置播放音乐的地址
           music_player.setDataSource(MusicService.this, item.SongUri);
            //准备播放音乐
           music_player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //播放音乐，根据reload标志位判断是非需要重新加载音乐
    private void playMusicItem(MusicItem item, boolean reload) {
        //如果这里传入的是空值，就什么也不做
        if(item == null) {
            return;
        }
        Log.i("play",item.name+reload);
        if(reload) {
            //需要重新加载音乐
            prepareToPlay(item);
        }
        mHandler.removeMessages(MSG_PROGRESS_UPDATE);
        mHandler.sendEmptyMessage(MSG_PROGRESS_UPDATE);
        //开始播放，如果之前只是暂停播放，那么音乐将继续播放

        music_player.start();

        //将音乐设置到指定时间开始播放，时间单位为毫秒
        seekToInner((int)item.playedTime);
        //将播放的状态通过监听器通知给监听者
        for(OnStateChangeListenr l : ListenerList) {
            l.onPlay(item);
        }
        //设置为非暂停播放状态
        paused = false;
        mHandler.removeMessages(MSG_PROGRESS_UPDATE);
        mHandler.sendEmptyMessage(MSG_PROGRESS_UPDATE);
    }
}
