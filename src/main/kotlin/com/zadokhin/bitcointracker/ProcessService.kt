package com.zadokhin.bitcointracker

import com.zadokhin.bitcointracker.process.Process
import org.springframework.stereotype.Service

@Service
class ProcessService {

    var trackedProcesses: List<Process> = listOf()

    fun createProcess(process: Process) {
        process.start()
        trackedProcesses = trackedProcesses + process
    }

    fun updateTrackedProcesses() {
        trackedProcesses = trackedProcesses.filter { !it.completed() }
        trackedProcesses.forEach { it.update() }
    }
}
