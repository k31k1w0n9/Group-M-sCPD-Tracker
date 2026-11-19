package com.example.scpd_tracker.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scpd_tracker.ScpdApplication
import com.example.scpd_tracker.di.AppContainer

/**
 * Creates (or reuses) a ViewModel scoped to the current navigation destination, handing it the app's
 * repositories and a SavedStateHandle that survives rotation and process death.
 */
@Composable
inline fun <reified VM : ViewModel> scpdViewModel(
    key: String? = null,
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
): VM = viewModel(
    key = key,
    factory = viewModelFactory {
        initializer {
            val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ScpdApplication
            create(app.container, createSavedStateHandle())
        }
    },
)
