package com.lowderancorp.inioli.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.lowderancorp.inioli.InioliApplication

fun CreationExtras.inioliApplication(): InioliApplication {
    return this[APPLICATION_KEY] as InioliApplication
}
