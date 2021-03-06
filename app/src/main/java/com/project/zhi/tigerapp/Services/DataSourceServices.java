package com.project.zhi.tigerapp.Services;

import android.content.Context;

import com.project.zhi.tigerapp.Entities.Attributes;
import com.project.zhi.tigerapp.Entities.Data;
import com.project.zhi.tigerapp.Entities.Entities;
import com.project.zhi.tigerapp.R;

import org.androidannotations.annotations.EBean;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import lombok.experimental.var;

interface  IDataSourceServices{
    Data getPeopleSource(Context context);
}
@EBean
public class DataSourceServices implements IDataSourceServices {

    @Override
    public Data getPeopleSource(Context context) {
        Serializer serializer = new Persister();
        InputStream input = context.getResources().openRawResource(R.raw.entities);
        Data data = null;
        try {
            data = serializer.read(Data.class, input);
            data = setImagePath(data);
        } catch (Exception e) {
            //Likely to the issue with the data parser.
        }
        finally {
            return data;
        }
    }

    public ArrayList<String> getUniqueKey(Data data){
        ArrayList<String> attributesList = new ArrayList<String>();
        for (Entities entity: data.getEntitiesList()
             ) {
            for (Attributes attributes: entity.getList()
                 ) {
                if(!attributesList.contains(attributes.getAttributeKey())){
                    attributesList.add(attributes.getAttributeKey());
                }
            }
        }
        return attributesList;
    }
    public Data setImagePath(Data data){
        for (Entities entity: data.getEntitiesList()
             ) {
            if(entity.getAttachments() != null && entity.getAttachments().getFilename() != null && !entity.getAttachments().getFilename().isEmpty()){
                entity.getAttachments().setFilename(setFileName(entity.getAttachments().getFilename()));
            }
        }
        return data;
    }

    public String setFileName(String fileName){
        fileName = fileName.toLowerCase();
        fileName = fileName.replace(" ", "_");
        fileName = fileName.replaceAll("^\\d+","");
        fileName = fileName.replaceAll("[^a-z0-9\\\\_\\\\.]","_");
        fileName = fileName.replaceAll("^\\_+","");
        fileName = FilenameUtils.removeExtension(fileName);
        return  fileName;
    }
}

