package com.csl.cs108ademoapp.adapters;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import android.util.Log;

import com.csl.cs108ademoapp.MainActivity;
import com.csl.cs108ademoapp.fragments.InventoryBarcodeFragment;
import com.csl.cs108ademoapp.fragments.InventoryRfidiMultiFragment;

public class InventoryAdapter extends FragmentStatePagerAdapter {
    private final int NO_OF_TABS = 2;
    public Fragment fragment0, fragment1;

    @Override
    public Fragment getItem(int index) {
        Log.i("Hello", "InventoryAdadpter.getItem");
        Fragment fragment = null;
        switch (index) {
            case 0:
                fragment = InventoryRfidiMultiFragment.newInstance(false, null);
                fragment0 = fragment;
                break;
            case 1:
                fragment = new InventoryBarcodeFragment();
                fragment1 = fragment;
                break;
            default:
                fragment = null;
                break;
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return NO_OF_TABS;
    }

    @Override
    public int getItemPosition(Object object) {
        return PagerAdapter.POSITION_NONE;
    }

    public InventoryAdapter(FragmentManager fm) {
        super(fm);
    }
}
