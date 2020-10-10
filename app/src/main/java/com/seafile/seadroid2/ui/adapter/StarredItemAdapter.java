package com.seafile.seadroid2.ui.adapter;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.common.collect.Lists;
import com.seafile.seadroid2.R;
import com.seafile.seadroid2.data.DataManager;
import com.seafile.seadroid2.data.SeafItem;
import com.seafile.seadroid2.data.SeafStarredFile;
import com.seafile.seadroid2.ui.WidgetUtils;
import com.seafile.seadroid2.ui.activity.BrowserActivity;
import com.seafile.seadroid2.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StarredItemAdapter extends BaseAdapter {
    private static final String DEBUG_TAG = "StarredItemAdapter";
    private ArrayList<SeafItem> items;
    private BrowserActivity mActivity;
    private DataManager dataManager;

    private boolean actionModeOn;
    private SparseBooleanArray mSelectedItemsIds;
    private List<Integer> mSelectedItemsPositions = Lists.newArrayList();
    private List<SeafStarredFile> mSelectedItemsValues = Lists.newArrayList();
    private ListView mListView;

    public StarredItemAdapter(BrowserActivity activity, ListView listView) {
        this.mActivity = activity;
        this.mListView = listView;
        items = Lists.newArrayList();
        mSelectedItemsIds = new SparseBooleanArray();
        dataManager = mActivity.getDataManager();

    }

    @Override
    public int getCount() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }

    public void add(SeafItem entry) {
        items.add(entry);
    }

    public void notifyChanged() {
        notifyDataSetChanged();
    }

    @Override
    public SeafItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setActionModeOn(boolean actionModeOn) {
        this.actionModeOn = actionModeOn;
    }

    public void toggleSelection(int position) {
        if (mSelectedItemsIds.get(position)) {
            // unselected
            mSelectedItemsIds.delete(position);
            mSelectedItemsPositions.remove(Integer.valueOf(position));
            mSelectedItemsValues.remove(items.get(position));
        } else {
            mSelectedItemsIds.put(position, true);
            mSelectedItemsPositions.add(position);
            mSelectedItemsValues.add((SeafStarredFile) items.get(position));
        }

        mActivity.getStarredFragment().updateContextualActionBar();
        notifyDataSetChanged();
    }

    public int getCheckedItemCount() {
        return mSelectedItemsIds.size();
    }

    public List<SeafStarredFile> getSelectedItemsValues() {
        return mSelectedItemsValues;
    }

    public void setItems(List<SeafStarredFile> starredFiles) {
        items.clear();
        items.addAll(starredFiles);
        this.mSelectedItemsIds.clear();
        this.mSelectedItemsPositions.clear();
        this.mSelectedItemsValues.clear();
    }

    public void deselectAllItems() {
        mSelectedItemsIds.clear();
        mSelectedItemsPositions.clear();
        mSelectedItemsValues.clear();
        notifyDataSetChanged();
    }

    public void selectAllItems() {
        mSelectedItemsIds.clear();
        mSelectedItemsPositions.clear();
        mSelectedItemsValues.clear();
        for (int i = 0; i < items.size(); i++) {
            mSelectedItemsIds.put(i, true);
            mSelectedItemsPositions.add(i);
            mSelectedItemsValues.add((SeafStarredFile) items.get(i));
        }
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final SeafItem item = items.get(position);
        View view = convertView;
        final Viewholder viewHolder;

        if (convertView == null) {
            view = LayoutInflater.from(mActivity).inflate(R.layout.starred_list_item, null);
            ImageView multiSelect = (ImageView) view.findViewById(R.id.list_item_multi_select_btn);
            TextView title = (TextView) view.findViewById(R.id.starred_list_item_title);
            TextView subtitle = (TextView) view.findViewById(R.id.starred_list_item_subtitle);
            ImageView icon = (ImageView) view.findViewById(R.id.starred_list_item_icon);
            ImageView action = (ImageView) view.findViewById(R.id.starred_list_item_action);
            viewHolder = new Viewholder(title, subtitle, multiSelect, icon, action);
            view.setTag(viewHolder);
        } else {
            viewHolder = (Viewholder) convertView.getTag();
        }
        viewHolder.icon.setTag(item.getTitle());
        viewHolder.title.setText(item.getTitle());
        viewHolder.subtitle.setText(item.getSubtitle());
        judgeRepo(item, viewHolder);

        if (Utils.isViewableImage(item.getTitle())) {
            String url = dataManager.getThumbnailLink(((SeafStarredFile) item).getRepoID(), ((SeafStarredFile) item).getPath(), WidgetUtils.getThumbnailWidth());
            if (url == null) {
                judgeRepo(item, viewHolder);
            } else {
                viewHolder.icon.setImageResource(R.drawable.file_image);
                GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                        .addHeader("Authorization", "Token " + dataManager.getAccount().token)
                        .build());
                RequestOptions opt = new RequestOptions()
                        .placeholder(R.drawable.file_image)
                        .skipMemoryCache(true)
                        .override(WidgetUtils.getThumbnailWidth(), WidgetUtils.getThumbnailWidth())
                        .diskCacheStrategy(DiskCacheStrategy.NONE);
                Glide.with(mActivity)
                        .asBitmap()
                        .load(glideUrl)
                        .apply(opt)
                        .thumbnail(0.1f)
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                ImageView imageView = (ImageView) mListView.findViewWithTag(item.getTitle());
                                File file = dataManager.getLocalRepoFile(((SeafStarredFile) item).getRepoName(), ((SeafStarredFile) item).getRepoID(), ((SeafStarredFile) item).getPath());
                                if (imageView != null && file.exists() && Utils.isViewableImage(file.getName())) {
                                    Bitmap bitmap = Utils.openImage(file.getAbsolutePath().toString());
                                    imageView.setImageBitmap(bitmap);
                                }
                            }

                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                ImageView imageView = (ImageView) mListView.findViewWithTag(item.getTitle());
                                if (imageView != null) {
                                    imageView.setImageBitmap(resource);
                                }
                            }
                        });
            }
        } else {

            judgeRepo(item, viewHolder);

        }

        if (actionModeOn) {
            viewHolder.multiSelect.setVisibility(View.VISIBLE);
            if (mSelectedItemsIds.get(position)) {
                viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_checked);
            } else
                viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_unchecked);

            viewHolder.multiSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mSelectedItemsIds.get(position)) {
                        viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_checked);
                        mSelectedItemsIds.put(position, true);
                        mSelectedItemsPositions.add(position);
                        mSelectedItemsValues.add((SeafStarredFile) item);
                    } else {
                        viewHolder.multiSelect.setImageResource(R.drawable.multi_select_item_unchecked);
                        mSelectedItemsIds.delete(position);
                        mSelectedItemsPositions.remove(Integer.valueOf(position));
                        mSelectedItemsValues.remove(item);
                    }

                    mActivity.onItemSelected();
                }
            });
        } else
            viewHolder.multiSelect.setVisibility(View.GONE);

        return view;
    }

    private void judgeRepo(SeafItem item, Viewholder viewHolder) {
        if (((SeafStarredFile) item).isRepo_encrypted() && ((SeafStarredFile) item).isDir() && ((SeafStarredFile) item).getPath().equals("/")) {
            viewHolder.icon.setImageResource(R.drawable.repo_encrypted);

        } else {
            if (((SeafStarredFile) item).isDir() && ((SeafStarredFile) item).getPath().equals("/")) {
                viewHolder.icon.setImageResource(R.drawable.repo);
            } else {
                viewHolder.icon.setImageResource(item.getIcon());
            }
        }
    }

    private class Viewholder {
        TextView title, subtitle;
        ImageView multiSelect, icon, action;

        public Viewholder(TextView title, TextView subtitle, ImageView multiSelect, ImageView icon, ImageView action) {
            super();
            this.multiSelect = multiSelect;
            this.icon = icon;
            this.action = action;
            this.title = title;
            this.subtitle = subtitle;
        }
    }


}
