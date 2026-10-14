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
        TerminalLine("", "Localhost Interactive CLI\nType 'help' for available commands.\n", localhostCli.getCurrentPromptPrefix())
    ))
    val history = _history.asStateFlow()

    private val _currentPath = MutableStateFlow(localhostCli.getCurrentPath())
    val currentPath = _currentPath.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private var activeJob: Job? = null

    fun getActiveVenvName(): String? = localhostCli.getActiveVenvName()

    fun runCommand(cmd: String) {
        if (cmd.isBlank()) return
        activeJob?.cancel()

        activeJob = viewModelScope.launch {
            _isRunning.value = true
            val prompt = localhostCli.getCurrentPromptPrefix()
            val currentOutput = StringBuilder()
            val targetIndex = _history.value.size
            _history.value = _history.value + TerminalLine(cmd, "", prompt)

            try {
                val out = localhostCli.execute(cmd) { chunk ->
                    currentOutput.append(chunk)
                    val list = _history.value.toMutableList()
                    if (targetIndex < list.size) {
                        list[targetIndex] = TerminalLine(cmd, currentOutput.toString(), prompt)
                        _history.value = list
                    }
                }
                _currentPath.value = localhostCli.getCurrentPath()

                if (out == "__CLEAR__") {
                    _history.value = emptyList()
                } else {
                    val finalOutput = if (currentOutput.isNotEmpty()) currentOutput.toString().trimEnd() else out
                    val list = _history.value.toMutableList()
                    if (targetIndex < list.size) {
                        list[targetIndex] = TerminalLine(cmd, finalOutput, prompt)
                        _history.value = list
                    }
                }
            } catch (_: Exception) {
                val list = _history.value.toMutableList()
                if (targetIndex < list.size) {
                    list[targetIndex] = TerminalLine(cmd, "^C (Interrupted)", prompt)
                    _history.value = list
                }
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun stopRunningCommand() {
        localhostCli.interrupt()
        activeJob?.cancel()
        _isRunning.value = false
        val prompt = localhostCli.getCurrentPromptPrefix()
        val list = _history.value.toMutableList()
        if (list.isNotEmpty() && list.last().output.isEmpty()) {
            list[list.lastIndex] = list.last().copy(output = "^C")
            _history.value = list
        } else {
            _history.value = _history.value + TerminalLine("", "^C", prompt)
        }
    }

    fun clear() {
        _history.value = emptyList()
    }
}
