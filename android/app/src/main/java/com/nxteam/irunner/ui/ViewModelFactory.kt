package com.nxteam.irunner.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.nxteam.irunner.AppContainer
import com.nxteam.irunner.NxIRunnerApplication

@Composable
fun rememberAppContainer(): AppContainer =
    (LocalContext.current.applicationContext as NxIRunnerApplication).container

class ContainerViewModelFactory<T : ViewModel>(
    private val create: () -> T,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <V : ViewModel> create(modelClass: Class<V>, extras: CreationExtras): V = create() as V
}
