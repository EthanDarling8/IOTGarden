package com.example.iotgarden.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PlantsViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public PlantsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is plants fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}