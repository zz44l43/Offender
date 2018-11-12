package com.project.zhi.tigerapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.GridView;

import com.google.gson.Gson;
import com.project.zhi.tigerapp.Adapter.PeopleAdapter;
import com.project.zhi.tigerapp.Entities.Data;
import com.project.zhi.tigerapp.Entities.Person;
import com.project.zhi.tigerapp.Entities.Record.IntelRecord;
import com.project.zhi.tigerapp.Services.ActivityService;
import com.project.zhi.tigerapp.Services.DataFilteringService;
import com.project.zhi.tigerapp.Services.DataSourceServices;
import com.project.zhi.tigerapp.Services.MenuService;
import com.project.zhi.tigerapp.Services.NavigationService;
import com.project.zhi.tigerapp.Services.UserPrefs_;
import com.project.zhi.tigerapp.Utils.Utils;
import com.project.zhi.tigerapp.complexmenu.MenuModel;
import com.project.zhi.tigerapp.complexmenu.SelectMenuView;
import com.project.zhi.tigerapp.complexmenu.holder.SubjectHolder;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import lombok.experimental.var;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    @ViewById(R.id.gridview)
    GridView gridview;

    @ViewById(R.id.btn_clear_all)
    Button btnClearAll;

    @ViewById(R.id.toolbar)
    Toolbar Toolbar;

    @Pref
    UserPrefs_ userPrefs;


    PeopleAdapter adapter;
    @Bean
    DataFilteringService dataFilteringService;
    @Bean
    DataSourceServices dataSourceServices;
    @Bean
    NavigationService navigationService;
    @Bean
    ActivityService activityService;
    @Bean
    MenuService menuService;

    android.app.AlertDialog dialog;

    @ViewById(R.id.menu)
    SelectMenuView selectMenuView;

    private SubjectHolder.OnSearchBtnListener onSearchBtnListener;
    Context context = this;

    @AfterViews
    void bindAdapter() {
        onLoading();
        boolean isValid = onActivityValidation();
        onSetView();
        if (isValid) {
            onInitGridView();
        }
        onSetMenu();
    }

    public void onStart() {
        super.onStart();
    }

    private boolean isDataSourceExpired(){
        boolean isExpired = false;
        if(userPrefs.isFile().get()){
            File file = new File(userPrefs.file().get());
            if(Utils.isExpiredFile(file)){
                isExpired = true;
                try {
                    Utils.secureDelete(file);
                } catch (IOException e) {
                    e.printStackTrace();
                    file.delete();
                }
                finally {
                    if(userPrefs.isFolder().get()) {
                        File folder = new File(userPrefs.folder().get());
                        ArrayList<File> files = Utils.getAllFilesInDir(folder);
                        if (files != null) {
                            for (File folderFile : files
                                    ) {
                                if (Utils.isExpiredFile(folderFile)) {
                                    folderFile.delete();
                                }
                            }
                        }
                    }
                    userPrefs.file().put(null);
                    if(userPrefs.recordFolder().get() != null){
                        File folder = new File(userPrefs.recordFolder().get());
                        ArrayList<File> filesRecord = Utils.getAllFilesInDir(folder);
                        if (filesRecord != null) {
                            for (File folderFile : filesRecord
                                    ) {
                                if (Utils.isExpiredFile(folderFile)) {
                                    folderFile.delete();
                                }
                            }
                        }
                    }
                    dataSourceServices.dataSourceChange(this);
                }
            }
        }
        return isExpired;
    }


    private boolean onActivityValidation() {
        boolean isDataSourceValid = isDataSourceExpired();
        if(isDataSourceValid){
            Utils.setAlertDialog("Warning", "Data source had expired, please re-upload", this).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onInvalidLocalActivity();
                }
            }).show();
            onDismiss();
            return false;
        }
        else {
            boolean isValid = checkValidActivity();
            boolean isMenuValid = activityService.validInitMenu(this);
            if (isValid && !isMenuValid) {
                dataSourceServices.dataSourceChange(this);
            }
            return isValid;
        }
    }

    private void onSetView() {
        setSupportActionBar(Toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, Toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
    }

    void onInitGridView(){
        ArrayList<Person> people = new ArrayList<Person>();
        setGridview(people);
        updateAdapter();
    }

    void onSetMenu(){
        selectMenuView.setOnFilteringBtnListener(new SelectMenuView.OnFilteringBtnListener() {
            @Override
            public void OnFiltering(ArrayList<MenuModel> nameMenus, ArrayList<MenuModel> mainDemoMenu, ArrayList<MenuModel> otherDemoMenu) {
                onLoading();
                updateAdapter();
            }
        });

        selectMenuView.setOnSearchingBtnListener(new SelectMenuView.OnSearchingBtnListener() {
            @Override
            public void OnSearching(String query) {
                onLoading();
                onSearching(query);
            }
        });
        selectMenuView.setOnLocationSearchingBtnListener(new SelectMenuView.OnLocationSearchingBtnListener() {
            @Override
            public void OnLocationSearching(Double longitude, Double latitude, Double radius) {
                onLoading();
                onLocationSearching(longitude,latitude,radius);
            }
        });

    }

    @Background
    void onLocationSearching(Double longitude, Double latitude, Double radius) {
        Data data = dataSourceServices.getPeopleSource(context);
        ArrayList<IntelRecord> intelRecords =dataSourceServices.getIntelRecordsSource(context);
        data = dataSourceServices.mergeEntitiesAndRecords(data,intelRecords);


        var newList = dataSourceServices.getPeopleFromEntities(dataFilteringService.searchLocation(data.getEntitiesList(), longitude,latitude,radius));
        setAdapterUi(newList);
    }

    @Background
    void onSearching(String query){
        var newList = dataSourceServices.getPeopleFromEntities(dataFilteringService.search(dataSourceServices.getPeopleSource(context).getEntitiesList(), query));
        setAdapterUi(newList);
    }

    boolean checkValidActivity(){
        if(!activityService.validActivity(this)){
            if(userPrefs.isUrl().get()){
                onDismiss();
                this.onInvalidInternetActivity();
                return false;
            }
            else{
                onDismiss();
                this.onInvalidLocalActivity();
                return false;
            }
        }
        return true;
    }

    @UiThread
    void setGridview(ArrayList<Person> people){
        adapter = new PeopleAdapter(this,people);
        gridview.setAdapter(adapter);
    }

    @UiThread
    void onInvalidInternetActivity(){
        Utils.setAlertDialog("Initialize", "Cannot found Internet source. Please synchronize source from server.", this).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity_.class);
                startActivity(intent);
                dialog.dismiss();
            }
        }).show();
    }

    @UiThread
    void onInvalidLocalActivity(){
        Utils.setAlertDialog("Initialize", "Cannot found local source. Please upload source from local file.", this).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(MainActivity.this, UploadActivity_.class);
                startActivity(intent);
                dialog.dismiss();
            }
        }).show();
    }
    @UiThread
    void onLoading(){
        dialog = Utils.setProgressDialog(this);
        dialog.show();
    }
    @UiThread
    void onDismiss(){
        dialog.dismiss();
    }
    @UiThread
    void onAdapterDataChange(){
        adapter.notifyDataSetChanged();
    }

    @UiThread
    void onAdapterDataChange2(PeopleAdapter adapter){
        adapter.notifyDataSetChanged();
        //adapter.notifyDataSetInvalidated();
        onDismiss();
    }


    @ItemClick(R.id.gridview)
    void gridViewItemClicked(Person person) {
        Gson gson = new Gson();
        String objStr = gson.toJson(person.getEntity());
        Intent intent = new Intent(this, ProfileActivity_.class);
        intent.putExtra("Profile", objStr);
        startActivity(intent);
    }
    @Click(R.id.btn_clear_all)
    void clearAll(){
        onLoading();
        userPrefs.voiceEntities().put(null);
        userPrefs.facialEntities().put(null);
        this.onClearAll();
    }
    @UiThread
    void setAdapterUi(ArrayList<Person> people){
        adapter.updatePeople(people);
        onDismiss();
    }
    @Background
    void updateAdapter(){
        ArrayList<Person> people = dataFilteringService.mergeAll(this, false);
        setAdapterUi(people);
        onDismiss();
    }
    @Background
    void onClearAll(){
        updateAdapter();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        startActivity(navigationService.getActivity(this, item));

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
