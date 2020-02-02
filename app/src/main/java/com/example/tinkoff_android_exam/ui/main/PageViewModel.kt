package com.example.tinkoff_android_exam.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tinkoff_android_exam.R

class PageViewModel : ViewModel() {

    private val _index = MutableLiveData<Int>()

//    Пердаем номер текущей вкладки
    val index: LiveData<Int> = Transformations.map(_index) {
        it
    }

    fun setIndex(index: Int) {
        _index.value = index
    }
}