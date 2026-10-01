package com.aura.weight.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.aura.weight.data.UserProfile
import com.aura.weight.data.WeightEntry
import com.aura.weight.data.WeightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

sealed interface Screen {
    data object Onboarding : Screen
    data object Home : Screen
    data object History : Screen
    data object Settings : Screen
}

class AuraViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WeightRepository(app)

    private val _profile = MutableStateFlow(repo.profile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _entries = MutableStateFlow(repo.all())
    val entries: StateFlow<List<WeightEntry>> = _entries.asStateFlow()

    private val _screen = MutableStateFlow<Screen>(
        if (_profile.value.onboarded) Screen.Home else Screen.Onboarding,
    )
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _widgetProviders = MutableStateFlow(-1)
    val widgetProviders: StateFlow<Int> = _widgetProviders.asStateFlow()

    init {
        refreshWidgetProviders()
    }

    private val _logOpen = MutableStateFlow(false)
    val logOpen: StateFlow<Boolean> = _logOpen.asStateFlow()

    fun refresh() {
        _entries.value = repo.all()
        _profile.value = repo.profile()
    }

    fun logWeight(kg: Double, day: Long = today(), note: String? = null) {
        repo.upsert(WeightEntry(day, kg, note, System.currentTimeMillis()))
        refresh()
        notifyWidgets()
    }

    fun delete(day: Long) {
        repo.delete(day)
        refresh()
        notifyWidgets()
    }

    fun saveProfile(p: UserProfile) {
        repo.saveProfile(p)
        _profile.value = p
        notifyWidgets()
    }

    fun go(to: Screen) {
        _screen.value = to
    }

    fun setLogOpen(open: Boolean) {
        _logOpen.value = open
    }

    fun today(): Long = LocalDate.now().toEpochDay()

    fun refreshWidgetProviders() {
        val app = getApplication<Application>()
        _widgetProviders.value = try {
            val mgr = android.appwidget.AppWidgetManager.getInstance(app)
            mgr?.getInstalledProvidersForPackage(app.packageName, null)?.size ?: 0
        } catch (e: Exception) {
            -1
        }
    }

    private fun notifyWidgets() {
        com.aura.weight.widget.WidgetUpdater.updateAll(getApplication())
        refreshWidgetProviders()
    }

    override fun onCleared() {
        repo.close()
    }
}
