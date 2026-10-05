package com.localhost.cli

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerminalLine(val command: String, val output: String, val cwd: String = "")

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val localhostCli: LocalhostCli
) : ViewModel() {

    private val _history = MutableStateFlow<List<TerminalLine>>(listOf(
        TerminalLine("", "Localhost Interactive CLI\nType 'help' for available commands.\n", localhostCli.getCurrentPath())
    ))
    val history = _history.asStateFlow()

    private val _currentPath = MutableStateFlow(localhostCli.getCurrentPath())
    val currentPath = _currentPath.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private var activeJob: Job? = null

    fun runCommand(cmd: String) {
        if (cmd.isBlank()) return
        activeJob?.cancel()

        activeJob = viewModelScope.launch {
            _isRunning.value = true
            try {
                val out = localhostCli.execute(cmd)
                _currentPath.value = localhostCli.getCurrentPath()

                if (out == "__CLEAR__") {
                    _history.value = emptyList()
                } else {
                    _history.value = _history.value + TerminalLine(cmd, out, _currentPath.value)
                }
            } catch (_: Exception) {
                _history.value = _history.value + TerminalLine(cmd, "^C (Interrupted)", _currentPath.value)
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun stopRunningCommand() {
        localhostCli.interrupt()
        activeJob?.cancel()
        _isRunning.value = false
        _history.value = _history.value + TerminalLine("", "^C", _currentPath.value)
    }

    fun clear() {
        _history.value = emptyList()
    }
}
