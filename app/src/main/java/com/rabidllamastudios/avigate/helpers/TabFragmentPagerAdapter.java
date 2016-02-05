package com.rabidllamastudios.avigate.helpers;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Configures any number of Tab Title and Fragment pairs inside a TabFragmentPagerAdapter
 * This class is intended for use with the TabLayout class
 * Created by Ryan Staatz on 12/19/15.
 */
public class TabFragmentPagerAdapter extends FragmentPagerAdapter {
    private List<String> mTabTitles;
    private List<Fragment> mFragments;

    /** Constructor that takes a Fragment Manager */
    public TabFragmentPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
        mTabTitles = new ArrayList<>();
        mFragments = new ArrayList<>();
    }

    /** Adds a Tab Title and Fragment pair at the input position (sequentially is left to right)
     * @param position the unique position of the Tab Title and Fragment pair to store
     * @param tabTitle the title of the Tab
     * @param fragment the Fragment associated with the input Tab Title
     */
    public void addEntry(int position, String tabTitle, Fragment fragment) {
        mTabTitles.add(position, tabTitle);
        mFragments.add(position, fragment);
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mTabTitles.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabTitles.get(position);
    }

}