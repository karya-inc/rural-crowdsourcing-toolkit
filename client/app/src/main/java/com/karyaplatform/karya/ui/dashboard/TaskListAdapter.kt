package com.karyaplatform.karya.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.karyaplatform.karya.data.model.karya.modelsExtra.TaskInfo
import com.karyaplatform.karya.databinding.ItemTaskBinding
import com.karyaplatform.karya.utils.extensions.gone
import com.karyaplatform.karya.utils.extensions.visible

class TaskListAdapter(
  private var tasks: List<TaskInfo>,
  private val dashboardItemClick: (task: TaskInfo) -> Unit = {},
) : RecyclerView.Adapter<TaskListAdapter.NgTaskViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NgTaskViewHolder {
    val layoutInflater = LayoutInflater.from(parent.context)
    val binding = ItemTaskBinding.inflate(layoutInflater, parent, false)

    return NgTaskViewHolder(binding, dashboardItemClick)
  }

  override fun onBindViewHolder(holder: NgTaskViewHolder, position: Int) {
    holder.bind(tasks[position])
  }

  override fun getItemCount(): Int {
    return tasks.size
  }

  fun addTasks(newTasks: List<TaskInfo>) {
    val oldTaskCount = tasks.size
    val tempList = mutableListOf<TaskInfo>()
    tempList.addAll(tasks)
    tempList.addAll(newTasks)

    tasks = tempList
    notifyItemRangeInserted(oldTaskCount, newTasks.size)
  }

  fun updateList(newList: List<TaskInfo>) {
    tasks = newList
    notifyDataSetChanged()
  }

  class NgTaskViewHolder(
    private val binding: ItemTaskBinding,
    private val dashboardItemClick: (task: TaskInfo) -> Unit,
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(taskInfo: TaskInfo) {
      val status = taskInfo.taskStatus
      val verified = status.verifiedMicrotasks
      val submitted = status.submittedMicrotasks + verified
      val completed = status.completedMicrotasks + submitted
      val assigned = status.assignedMicrotasks
      val skipped = status.skippedMicrotasks
      val expired = status.expiredMicrotasks

      val clickable = (assigned + skipped) > 0

      with(binding) {
        // Set text
        taskNameTv.text = taskInfo.taskName
        numIncompleteTv.text = assigned.toString()
        numCompletedTv.text = completed.toString()
        numSubmittedTv.text = submitted.toString()
        numVerifiedTv.text = verified.toString()
        numSkippedTv.text = skipped.toString()
        numExpiredTv.text = expired.toString()

        // Set views
        completedTasksPb.max = assigned + completed
        completedTasksPb.progress = completed

        // Set speech data report
        val report = taskInfo.reportSummary
        if (report == null) {
          scoreGroup.gone()
        } else {
          scoreGroup.visible()
          if (report.has("accuracy")) {
            accuracyFeedbackCl.visible()
            accuracyScore.rating = report.get("accuracy").asFloat
          } else {
            accuracyFeedbackCl.gone()
          }

          if (report.has("volume")) {
            volumeFeedbackCl.visible()
            volumeScore.rating = report.get("volume").asFloat
          } else {
            volumeFeedbackCl.gone()
          }

          if (report.has("quality")) {
            qualityFeedbackCl.visible()
            qualityScore.rating = report.get("quality").asFloat
          } else {
            qualityFeedbackCl.gone()
          }
        }

        // Task click listener
        taskLl.setOnClickListener { dashboardItemClick(taskInfo) }
        taskLl.isClickable = clickable
        taskLl.isEnabled = clickable
      }
    }
  }
}
