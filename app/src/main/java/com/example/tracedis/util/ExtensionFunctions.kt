package com.example.tracedis.util

import android.view.View
import android.widget.Button

object ExtensionFunctions {

    fun View.show(){
        this.visibility=View.VISIBLE
    }
    fun View.hide(){
        this.visibility=View.INVISIBLE
    }

    fun Button.enabled(){
        this.isEnabled=true
    }


    fun Button.disable(){
        this.isEnabled=false
    }


}