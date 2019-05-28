package com.vshows.imusic;

import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.TextView;

/**
 * Created by Vshows on 2018/4/26.
 */

public class MusicItem {

    public String singer;
    public String name;//歌曲名称
    public Uri SongUri;//歌曲uri路径
    public Uri AlbumUri;//歌曲封面路径
    public Bitmap thumb;;//缩略图
    public long duration;//播放时长
    public long playedTime;//已播放时长

    MusicItem(Uri songUri, Uri albumUri, String strName,String singer, long duration, long playTime){

        this.singer = singer;
        this.name = strName;
        this.SongUri = songUri;
        this.duration = duration;
        this.AlbumUri = albumUri;


    }

    public boolean equals(Object o){
        MusicItem oo = (MusicItem)o;
        if(this.SongUri.equals(oo.SongUri)){
            return true;
        }
        else {
            return false;
        }
    }
}
