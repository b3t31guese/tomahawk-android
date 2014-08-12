/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.tomahawk_android.adapters;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.MultiColumnClickListener;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Parcelable;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link PagerAdapter} which provides functionality to swipe an AlbumArt image. Used in {@link
 * org.tomahawk.tomahawk_android.fragments.PlaybackFragment}
 */
public class AlbumArtSwipeAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener,
        SlidingUpPanelLayout.PanelSlideListener {

    //Used to provide fake infinite swiping behaviour, if current Playlist is repeating
    private static final int FAKE_INFINITY_COUNT = 20000;

    private Context mContext;

    private LayoutInflater mLayoutInflater;

    private FragmentManager mFragmentManager;

    private int mFakeInfinityOffset;

    private boolean mByUser;

    private boolean mSwiped;

    private ViewPager mViewPager;

    private MultiColumnClickListener mClickListener;

    private SlidingUpPanelLayout mSlidingUpPanelLayout;

    private PlaybackService mPlaybackService;

    private Playlist mPlaylist;

    private int mCurrentViewPage = 0;

    /**
     * Constructs a new AlbumArtSwipeAdapter.
     *
     * @param viewPager ViewPager which this adapter has been connected with
     */
    public AlbumArtSwipeAdapter(Context context, FragmentManager fragmentManager,
            LayoutInflater layoutInflater, ViewPager viewPager,
            MultiColumnClickListener clickListener, SlidingUpPanelLayout slidingUpPanelLayout) {
        mContext = context;
        mLayoutInflater = layoutInflater;
        mFragmentManager = fragmentManager;
        mViewPager = viewPager;
        mClickListener = clickListener;
        mSlidingUpPanelLayout = slidingUpPanelLayout;
        mSlidingUpPanelLayout.setPanelSlideListener(this);
        mByUser = true;
        mSwiped = false;
    }

    /**
     * Instantiate an item in the {@link PagerAdapter}. Fill it async with the correct AlbumArt
     * image.
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = mLayoutInflater.inflate(
                org.tomahawk.tomahawk_android.R.layout.album_art_view_pager_item, container, false);
        Query query = null;
        if (mPlaylist != null && mPlaylist.getCount() > 0) {
            if (mPlaylist.isRepeating()) {
                query = mPlaylist.peekEntryAtPos((position) % mPlaylist.getCount()).getQuery();
            } else {
                query = mPlaylist.peekEntryAtPos(position).getQuery();
            }
            refreshTrackInfo(view, query);
        } else {
            refreshTrackInfo(view, null);
        }
        if (view != null) {
            container.addView(view);
        }
        return view;
    }

    /**
     * @return If current {@link Playlist} is empty or null, return 1. If current {@link Playlist}
     * is repeating, return FAKE_INFINITY_COUNT. Else return the current {@link Playlist}'s length.
     */
    @Override
    public int getCount() {
        if (mPlaylist == null || mPlaylist.getCount() == 0) {
            return 1;
        }
        if (mPlaylist.isRepeating()) {
            return FAKE_INFINITY_COUNT;
        }
        return mPlaylist.getCount();
    }

    /**
     * @return the offset by which the position should be shifted, when {@link Playlist} is
     * repeating
     */
    public int getFakeInfinityOffset() {
        return mFakeInfinityOffset;
    }

    /**
     * Remove the given {@link View} from the {@link ViewPager}
     */
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    /**
     * @return true if view is equal to object
     */
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    /**
     * Dummy method
     *
     * @return always null
     */
    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    /**
     * Is being called, whenever a new Page in our {@link AlbumArtSwipeAdapter} has been selected/
     * swiped to
     */
    @Override
    public void onPageSelected(int position) {
        if (mPlaybackService != null && isByUser()) {
            setSwiped(true);
            if (position == mCurrentViewPage - 1) {
                mPlaybackService.previous();
            } else if (position == mCurrentViewPage + 1) {
                mPlaybackService.next();
            }
        }
        mCurrentViewPage = position;
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    /**
     * @param position     to set the current item to
     * @param smoothScroll boolean to determine whether or not to show a scrolling animation
     */
    public void setCurrentItem(int position, boolean smoothScroll) {
        if (position != mCurrentViewPage) {
            if (mPlaylist.isRepeating()) {
                if (position == (mCurrentViewPage % mPlaylist.getCount()) + 1 || (
                        (mCurrentViewPage % mPlaylist.getCount()) == mPlaylist.getCount() - 1
                                && position == 0)) {
                    setCurrentToNextItem(smoothScroll);
                } else if (position == (mCurrentViewPage % mPlaylist.getCount()) - 1 || (
                        (mCurrentViewPage % mPlaylist.getCount()) == 0
                                && position == mPlaylist.getCount() - 1)) {
                    setCurrentToPreviousItem(smoothScroll);
                } else {
                    mViewPager.setCurrentItem(position, false);
                }
            } else {
                mViewPager.setCurrentItem(position, smoothScroll);
            }
            mCurrentViewPage = mViewPager.getCurrentItem();
        }
    }

    /**
     * @param smoothScroll boolean to determine whether or not to show a scrolling animation
     */
    public void setCurrentToNextItem(boolean smoothScroll) {
        mViewPager.setCurrentItem(mCurrentViewPage + 1, smoothScroll);
    }

    /**
     * @param smoothScroll boolean to determine whether or not to show a scrolling animation
     */
    public void setCurrentToPreviousItem(boolean smoothScroll) {
        mViewPager.setCurrentItem(mCurrentViewPage - 1, smoothScroll);
    }

    /**
     * update the {@link Playlist} of the {@link AlbumArtSwipeAdapter} to the given {@link
     * Playlist}
     */
    public void updatePlaylist() {
        if (mPlaybackService != null) {
            mPlaylist = mPlaybackService.getCurrentPlaylist();
            notifyDataSetChanged();
        }
        if (mPlaylist != null && mPlaylist.getCount() > 0) {
            mFakeInfinityOffset = mPlaylist.getCount() * ((FAKE_INFINITY_COUNT / 2) / mPlaylist
                    .getCount());
            setByUser(false);
            if (mPlaylist.isRepeating()) {
                setCurrentItem(mPlaylist.getCurrentQueryIndex() + getFakeInfinityOffset(), false);
            } else {
                setCurrentItem(mPlaylist.getCurrentQueryIndex(), false);
            }
            setByUser(true);
        }
    }

    /**
     * @return whether or not previous swipe was done by user
     */
    public boolean isByUser() {
        return mByUser;
    }

    /**
     * Set whether or not previous swipe was done by user
     */
    public void setByUser(boolean byUser) {
        this.mByUser = byUser;
    }

    /**
     * @return whether or not previous skipping to next/previous {@link org.tomahawk.libtomahawk.collection.Track}
     * was induced by swiping
     */
    public boolean isSwiped() {
        return mSwiped;
    }

    /**
     * Set whether or not previous skipping to next/previous {@link org.tomahawk.libtomahawk.collection.Track}
     * was induced by swiping
     */
    public void setSwiped(boolean isSwiped) {
        this.mSwiped = isSwiped;
    }

    /**
     * Set this {@link AlbumArtSwipeAdapter}'s {@link PlaybackService} reference
     */
    public void setPlaybackService(PlaybackService mPlaybackService) {
        this.mPlaybackService = mPlaybackService;
        updatePlaylist();
    }

    private void refreshTrackInfo(View view, final Query query) {
        TextView artistTextView = (TextView) view.findViewById(R.id.now_playing_artist);
        TextView titleTextView = (TextView) view.findViewById(R.id.now_playing_title);
        ImageButton playPauseButton = (ImageButton) view.findViewById(R.id.now_playing_button1);
        ImageButton loveButton = (ImageButton) view.findViewById(R.id.now_playing_button2);
        ImageView imageView = (ImageView) view.findViewById(R.id.album_art_image);
        View clickView = view.findViewById(R.id.sliding_layout_click_view);
        ImageView nowPlayingAlbumArt = (ImageView) view.findViewById(R.id.now_playing_album_art);
        if (query != null) {
            mSlidingUpPanelLayout.showPanel();
            boolean landscapeMode = mContext.getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE;
            TomahawkUtils.loadImageIntoImageView(mContext, imageView, query.getImage(),
                    Image.getLargeImageSize(), landscapeMode);
            TomahawkUtils.loadImageIntoImageView(mContext, nowPlayingAlbumArt, query.getImage(),
                    Image.getSmallImageSize());
            if (!mSlidingUpPanelLayout.isPanelExpanded()) {
                playPauseButton.setEnabled(true);
                playPauseButton.setVisibility(View.VISIBLE);
                if (mPlaybackService.isPlaying()) {
                    TomahawkUtils.loadDrawableIntoImageView(mContext, playPauseButton,
                            R.drawable.ic_player_pause);
                } else {
                    TomahawkUtils.loadDrawableIntoImageView(mContext, playPauseButton,
                            R.drawable.ic_player_play);
                }
                playPauseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPlaybackService.playPause();
                    }
                });
            } else {
                playPauseButton.setEnabled(false);
                playPauseButton.setVisibility(View.GONE);
            }

            // Update all relevant TextViews
            if (artistTextView != null) {
                if (query.getArtist() != null && query.getArtist().getName() != null) {
                    artistTextView.setText(query.getArtist().toString());
                } else {
                    artistTextView.setText(R.string.playbackactivity_unknown_string);
                }
            }
            if (titleTextView != null) {
                if (query.getName() != null) {
                    titleTextView.setText(query.getName());
                } else {
                    titleTextView.setText(R.string.playbackactivity_unknown_string);
                }
            }
            if (loveButton != null) {
                if (DatabaseHelper.getInstance().isItemLoved(query)) {
                    loveButton.setImageResource(R.drawable.ic_action_loved);
                } else {
                    loveButton.setImageResource(R.drawable.ic_action_notloved);
                }
                loveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CollectionManager.getInstance().toggleLovedItem(query);
                        mContext.sendBroadcast(
                                new Intent(PlaybackService.BROADCAST_CURRENTTRACKCHANGED));
                    }
                });
            }
            imageView.setOnLongClickListener(new ClickListener(query, mClickListener));
            clickView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mSlidingUpPanelLayout.isEnabled()) {
                        return;
                    }
                    if (!mSlidingUpPanelLayout.isPanelExpanded()
                            && !mSlidingUpPanelLayout.isPanelAnchored()) {
                        mSlidingUpPanelLayout.expandPanel(mSlidingUpPanelLayout.getAnchorPoint());
                    } else {
                        mSlidingUpPanelLayout.collapsePanel();
                    }
                }
            });
        } else {
            mSlidingUpPanelLayout.hidePanel();
            //No track has been given, so we update the view state accordingly
            // Update all relevant TextViews

            if (artistTextView != null) {
                artistTextView.setText("");
            }
            if (titleTextView != null) {
                titleTextView.setText(R.string.playbackactivity_no_track);
            }
            if (loveButton != null) {
                loveButton.setImageResource(R.drawable.ic_action_notloved);
            }
        }
    }

    @Override
    public void onPanelSlide(View view, float v) {

    }

    @Override
    public void onPanelCollapsed(View view) {
        notifyDataSetChanged();
    }

    @Override
    public void onPanelExpanded(View view) {
        notifyDataSetChanged();
    }

    @Override
    public void onPanelAnchored(View view) {

    }

    @Override
    public void onPanelHidden(View view) {

    }
}
