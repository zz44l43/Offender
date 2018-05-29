package com.project.zhi.tigerapp.Services;

import com.project.zhi.tigerapp.Entities.Attributes;
import com.project.zhi.tigerapp.Entities.Entities;
import com.project.zhi.tigerapp.Entities.Person;
import com.project.zhi.tigerapp.Enums.AttributeType;
import com.project.zhi.tigerapp.Utils.Utils;
import com.project.zhi.tigerapp.complexmenu.MenuModel;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import lombok.experimental.var;
import lombok.val;

@EBean
public class DataFilteringService {
    @Pref
    UserPrefs_ userPrefs;
    public String getPersonName(Entities entities){
        String firstName = "";
        String middleName = "";
        String lastName = "";
        for (Attributes attribute: entities.getList()) {
            if(attribute.getAttributeKey().equalsIgnoreCase("firstname")){
                firstName = attribute.getStringValue();
            }
            else if(attribute.getAttributeKey().equalsIgnoreCase("middlename")){
                middleName = attribute.getStringValue();
            }
            else if(attribute.getAttributeKey().equalsIgnoreCase("familyname")){
                lastName = attribute.getStringValue();
            }
        }
        return firstName + (middleName.isEmpty() ? "" : " " + middleName) + (lastName.isEmpty() ? "" : " " + lastName);
    }

    public ArrayList<Entities> search(ArrayList<Entities> entities, String query){
        if(query == null || query.isEmpty()){
            return entities;
        }
        String [] queries = query.split(",");
        ArrayList<Entities> filteredEntities = new ArrayList<>();
        for (String oneQuery: queries
             ) {
            for (Entities entity: entities){
                if(isSatisySingleQuery(oneQuery.trim(),entity.getList())){
                    filteredEntities.add(entity);
                }
            }
            entities.retainAll(filteredEntities);
            filteredEntities.clear();
        }
        return entities;
    }

    public ArrayList<Entities> update(ArrayList<Entities> entities, ArrayList<MenuModel> nameMenu, ArrayList<MenuModel> mainDemoMenu, ArrayList<MenuModel> otherDemoMenu) {
        ArrayList<Entities> filteredEntities = new ArrayList<>();
        val noneEmptyNameMenu = nonEmptyCriteria(nameMenu);
        ArrayList<MenuModel> noneEmptyMainDemoMenu = nonEmptyCriteria(mainDemoMenu);
        ArrayList<MenuModel> noneEmptyOtherDemoMenu = nonEmptyCriteria(otherDemoMenu);
        for (Entities entity : entities
                ) {
            if(isSatisfyAllCriteriaFromAListOfCriteria(noneEmptyNameMenu, entity.getList(), true) && isSatisfyAllCriteriaFromAListOfCriteria(noneEmptyMainDemoMenu, entity.getList(),true) && isSatisfyAllCriteriaFromAListOfCriteria(noneEmptyOtherDemoMenu, entity.getList(),true)){
                filteredEntities.add(entity);
            }
        }
        return filteredEntities;
    }
    public ArrayList<MenuModel> nonEmptyCriteria(ArrayList<MenuModel> menuList){
        ArrayList<MenuModel> nonEmptyMenuList = new ArrayList<>(menuList);
        var nonEmptyFilter = new Predicate<MenuModel>() {
            @Override
            public boolean evaluate(MenuModel object) {
                return object.getValue() != null && !object.getValue().isEmpty();
            }
        };
        CollectionUtils.filter(nonEmptyMenuList, nonEmptyFilter);
        return nonEmptyMenuList;
    }
    public boolean isSatisfyAllCriteriaFromAListOfCriteria(ArrayList<MenuModel> criterias, ArrayList<Attributes> attributes, boolean fuzzyMatch ) {
        if(attributes == null && attributes.size() == 0){
            return true;
        }
        for (var criteria : criterias
                ) {
            if(fuzzyMatch){
                if(!isSatisyFuzzySignleCriteria(criteria, attributes))
                    return false;
            }
            else {
                if (!isSatisySignleCriteria(criteria, attributes)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isSatisySingleQuery(String query, ArrayList<Attributes> attributes){
        for(var i =0; i< attributes.size(); i++){
            var attribute= attributes.get(i);
            var key = attribute.getAttributeKey();
            var isFuzzy = true;
            var value = "";
            if(attribute.getType().equalsIgnoreCase("TEXT")){
                value = attribute.getStringValue();
            }
            else if(attribute.getType().equalsIgnoreCase(AttributeType.LIST.name())){
                value = attribute.getListKey();
                isFuzzy = false;
            }
            else if (attribute.getDoubleValue()!=null){
                value = attribute.getDoubleValue().toString();
            }
            if(isFuzzy) {
                if (value.toLowerCase().contains(query.toLowerCase())) {
                    return true;
                }
            }
            else{
                if (value.toLowerCase().equalsIgnoreCase(query.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }


    public boolean isSatisySignleCriteria(MenuModel criteria, ArrayList<Attributes> attributes){
        for(var i =0; i< attributes.size(); i++){
            var attribute= attributes.get(i);
            var key = attribute.getAttributeKey();
            var value = "";
            if(attribute.getType().equalsIgnoreCase("TEXT")){
                value = attribute.getStringValue();
            }
            else{
                value = attribute.getDoubleValue().toString();
            }
            if(criteria.getAttributeKey().equalsIgnoreCase(key) && criteria.getValue().equalsIgnoreCase(value)){
                return true;
            }
        }
        return false;
    }

    public boolean isSatisyFuzzySignleCriteria(MenuModel criteria, ArrayList<Attributes> attributes){
        for(var i =0; i< attributes.size(); i++){
            var attribute= attributes.get(i);
            var key = attribute.getAttributeKey();
            var value = "";
            if(attribute.getType().equalsIgnoreCase(AttributeType.NUMERIC.name())){
                if(criteria.getAttributeKey().equalsIgnoreCase(key) && attribute.getDoubleValue() >= criteria.getMinValue() && attribute.getDoubleValue() <= criteria.getMaxValue() ){
                    return true;
                }
            }
            else if(attribute.getType().equalsIgnoreCase(AttributeType.BOOLEAN.name())){
                if(criteria.getAttributeKey().equalsIgnoreCase(key) && Utils.getAttributeValues(attribute).equalsIgnoreCase(criteria.getValue())){
                    return true;
                }
            }
            else{
                value = attribute.getStringValue();
            }
            if(criteria.getAttributeKey().equalsIgnoreCase(key) && value.toLowerCase().contains(criteria.getValue().toLowerCase())){
                return true;
            }
        }
        return false;
    }

    public ArrayList<Person> mergeAll(){
        ArrayList<Person> filteredPersonList = new ArrayList<Person>();
        String jsonFilteredEntites = userPrefs.filteredEntites().get();////json filterd
        String jsonVoice = userPrefs.voiceEntities().get();/// json voice
        String jsonFace = userPrefs.facialEntities().get(); //// json face
        return filteredPersonList;
    }
}
