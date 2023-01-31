package com.karyaplatform.karya.ui.scenarios.quiz

import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.karyaplatform.karya.R
import com.karyaplatform.karya.ui.scenarios.common.BaseMTRendererFragment
import com.karyaplatform.karya.utils.extensions.gone
import com.karyaplatform.karya.utils.extensions.invisible
import com.karyaplatform.karya.utils.extensions.observe
import com.karyaplatform.karya.utils.extensions.viewLifecycleScope
import com.karyaplatform.karya.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.microtask_quiz.*
import nl.bryanderidder.themedtogglebuttongroup.ThemedButton
import com.intuit.ssp.R as ssp

@AndroidEntryPoint
class QuizMainFragment : BaseMTRendererFragment(R.layout.microtask_quiz) {
  override val viewModel: QuizViewModel by viewModels()
  private val args: QuizMainFragmentArgs by navArgs()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val view = super.onCreateView(inflater, container, savedInstanceState)
    viewModel.setupViewModel(args.taskId, args.completed, args.total)
    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Get and set microtask instruction
    // If no instruction gone
    try {
      val instruction = viewModel.task.params.asJsonObject.get("instruction").asString
      instructionTv.text = instruction
    } catch (e: Exception) {
      instructionTv.gone()
    }

    // Setup observers
    setupObservers()

    // Setup listeners
    setupListeners()
  }

  private fun setupObservers() {
    // Question has changed. Update the UI accordingly.
    viewModel.question.observe(viewLifecycleOwner.lifecycle, viewLifecycleScope) { question ->
      questionTv.text = question.question

      when (question.type) {
        QuestionType.invalid -> {
          textResponseEt.invisible()
          mcqResponseGroup.invisible()
        }

        QuestionType.text -> {
          mcqResponseGroup.gone()
          textResponseEt.visible()
          if (question.numeric == true) {
            textResponseEt.inputType = InputType.TYPE_CLASS_NUMBER
            textResponseEt.setLines(1)
          } else {
            textResponseEt.inputType = InputType.TYPE_CLASS_TEXT
            textResponseEt.minLines = if (question.long == true) 3 else 1
          }
        }

        QuestionType.mcq -> {
          textResponseEt.invisible()
          textResponseEt.minLines = 2
          mcqResponseGroup.visible()

          mcqResponseGroup.removeAllViews()

          question.options?.forEach { option ->
            val button = ThemedButton(requireContext())
            val params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.MATCH_PARENT)
            params.setMargins(10, 0, 10, 20)
            button.text = option
            // button.tvText.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(ssp.dimen._20ssp))
            // button.tvSelectedText.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(ssp.dimen._20ssp))
            button.selectedBgColor = R.color.c_dark_green
            mcqResponseGroup.addView(button, params)
          }

          mcqResponseGroup.selectableAmount = if (question.multiple == false) 1 else question.options!!.size
        }
      }
    }
  }

  private fun setupListeners() {
    nextBtn.setOnClickListener {

      // Check for input is in range when question is of type numeric and range is given
      if (viewModel.question.value.type == QuestionType.text) {
        if (viewModel.question.value.numeric == true) {
          val value = Integer.parseInt(textResponseEt.text.toString())
          val range = viewModel.question.value.range
          if (range != null && range.isNotEmpty() && value !in range[0]..range[1]) {
            Toast.makeText(requireContext(), "Value should be in range ${range[0]} - ${range[1]}", Toast.LENGTH_SHORT)
              .show()
            return@setOnClickListener
          }
        }
      }

      viewModel.submitResponse()
      textResponseEt.setText("")
    }

    textResponseEt.doAfterTextChanged {
      viewModel.updateTextResponse(textResponseEt.text.toString())
    }

    mcqResponseGroup.setOnSelectListener { button ->
      viewModel.updateMCQResponse(button.text)
    }
  }
}
