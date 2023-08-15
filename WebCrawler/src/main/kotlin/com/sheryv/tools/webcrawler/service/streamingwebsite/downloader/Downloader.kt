package com.sheryv.tools.webcrawler.service.streamingwebsite.downloader

import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.ConfigurationChangedEvent
import com.sheryv.util.*
import com.sheryv.util.logging.log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.greenrobot.eventbus.Subscribe
import java.nio.file.Path


object Downloader {
  private var schedulerJob: Job? = null
  
  //  private val currentOffset = AtomicLong(0)
  private val lock = Any()
  private val queue = ArrayDeque<DownloadingTask>()
  private var config = Configuration.get().downloaderConfig
//  val queue = ConcurrentLinkedQueue<M3U8Process>()
//  val inProgress = ConcurrentLinkedQueue<M3U8Process>()
//  val completed = ConcurrentLinkedQueue<M3U8Process>()
  
  
  fun add(url: String, outputPath: Path) {
    val process = M3U8DownloadingTask(outputPath, url, config = config.copy())
    synchronized(lock) {
      queue.addLast(process)
    }
    notifyChanged()
  }
  
  fun removeFromQueue(task: DownloadingTask) {
    synchronized(lock) {
      queue.remove(task)
    }
    notifyChanged()
  }
  
  fun startScheduler() {
    schedulerJob?.cancel()
    var lastProcessed = 0
    schedulerJob = inBackground {
      while (schedulerJob != null) {
        
        val task = synchronized(lock) {
          val processed = queue.count { it.state.inBeingProcessed() }
          if (processed > 0 || lastProcessed > 0) {
            notifyChanged()
            lastProcessed = processed
          }
          
          return@synchronized if (processed < config.concurrentDownloads && queue.isNotEmpty()) {
            queue.firstOrNull { !it.state.isStarted && it.state != DownloadingState.STOPPED }?.also {
              it.setStarted()
            }
          } else {
            null
          }
        }
        
        if (task != null) {
          notifyChanged()
          inBackground {
            try {
              task.startAndWait { state, handler ->
                synchronized(lock) {
                  handler(state)
                }
                notifyChanged()
              }
              notifyChanged()
            } catch (e: ProcessStoppedException) {
              log.warn("Interrupted download of ${task.fileName()} [${task.id}]")
              notifyChanged()
            }
          }
          delay(10)
        } else {
          delay(300)
        }
      }
    }
    subscribeEvent<ConfigurationChangedEvent> {
      this.config = it.configuration.downloaderConfig
    }
  }
  
  private fun notifyChanged() {
    val e =
      DownloadingStateChanged(
      )
    emitEvent(e)
  }
  
  fun stopScheduler() {
    schedulerJob?.cancel()
    schedulerJob = null
  }
  
  fun terminateScheduler() {
    stopScheduler()
    queue.forEach { it.stop() }
  }
  
  fun isRunning() = schedulerJob != null
  
  @Subscribe
  fun onConfigChanged(c: ConfigurationChangedEvent) {
    this.config = c.configuration.downloaderConfig
  }
  
  fun tasks(): List<DownloadingTask> = synchronized(lock) {
    return@synchronized queue.toList()
  }
  
}
