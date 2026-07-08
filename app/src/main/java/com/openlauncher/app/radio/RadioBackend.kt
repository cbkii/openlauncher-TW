package com.openlauncher.app.radio

import kotlinx.coroutines.flow.StateFlow
import com.openlauncher.app.viewmodel.LauncherViewModel.HardwareRadioState
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.data.AppSettings

interface RadioBackend {
    val hardwareRadio: StateFlow<HardwareRadioState?>
    fun start()
    fun stop()
    fun tune(band: String, freqMhz: Float)
    fun seekUp()
    fun seekDown()
    fun cycleFm()
    fun switchAm()
    fun launchApp()
    fun stopApp()
    fun updateState(nowPlaying: NowPlayingState?, settings: AppSettings)
}
