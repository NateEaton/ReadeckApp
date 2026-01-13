package de.readeckapp.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor() : ViewModel() {

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent = _navigationEvent.asStateFlow()

    fun onClickBack() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateBack)
        }
    }

    fun onClickOpenSourceLibraries() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToOpenSourceLibraries)
        }
    }

    fun onNavigationEventConsumed() {
        viewModelScope.launch {
            _navigationEvent.emit(null)
        }
    }

    sealed class NavigationEvent {
        object NavigateBack : NavigationEvent()
        object NavigateToOpenSourceLibraries : NavigationEvent()
    }
}
