package com.project.zhi.tigerapp.Services;

import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;

import com.project.zhi.tigerapp.MainActivity_;
import com.project.zhi.tigerapp.R;
import com.project.zhi.tigerapp.UploadActivity_;

import org.androidannotations.annotations.EBean;

@EBean
public class NavigationService {
    public Intent getActivity(Context activity, MenuItem item){
        int id = item.getItemId();

        if (id == R.id.nav_search) {
            return new Intent(activity, MainActivity_.class);
        } else if (id == R.id.nav_gallery) {
            return new Intent(activity, UploadActivity_.class);
        }
        return new Intent(activity, activity.getClass());
    }
}
