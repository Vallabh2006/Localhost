package com.localhost.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.localhost.core.data.repository.ProjectRepository
import com.localhost.runtime.process.ProcessSupervisor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var processSupervisor: ProcessSupervisor

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, SupervisorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            CoroutineScope(Dispatchers.IO).launch {
                val projects = projectRepository.getAll()
                projects.filter { it.autoStartOnBoot }.forEach { p ->
                    processSupervisor.startProject(p)
                }
            }
        }
    }
}
