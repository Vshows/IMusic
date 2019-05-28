package com.vshows.imusic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Vshows on 2018/5/1.
 */

public class MusicAdapter extends BaseAdapter {
    private List<MusicItem> music_list;
    private final LayoutInflater mInflater;
    private final int mResource;
    private Context mContext;

    public MusicAdapter(Context context, int resId, List<MusicItem> data)
    {
        mContext = context;
        music_list = data;
        mInflater = LayoutInflater.from(context);
        mResource = resId;
    }
    @Override
    public int getCount() {
        return music_list != null ? music_list.size() : 0;
    }

    @Override
    public Object getItem(int i) {
        return music_list != null ? music_list.get(i): null ;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = mInflater.inflate(mResource, viewGroup, false);
        }
        MusicItem item = music_list.get(i);
        TextView title = (TextView) view.findViewById(R.id.music_title);
        title.setText(item.name);
        TextView singer = (TextView) view.findViewById(R.id.singer);
        singer.setText(item.singer);

        return view;
    }
}
