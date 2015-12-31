package com.rabidllamastudios.avigate;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ryan on 12/19/15.
 * This FragmentPagerAdapter class configures any number of Tab Title and Fragment pairs
 * This class is intended for use with the TabLayout class
 */
public class TabFragmentPagerAdapter extends FragmentPagerAdapter {
    private List<String> mTabTitles;
    private List<Fragment> mFragments;

    public TabFragmentPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
        mTabTitles = new ArrayList<>();
        mFragments = new ArrayList<>();
    }

    //Adds a Tab Title and Fragment pair in a given position in the associated List instance var
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