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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.karyaplatform.karya.R
import com.karyaplatform.karya.ui.scenarios.common.BaseMTRendererFragment
import com.karyaplatform.karya.utils.extensions.gone
import com.karyaplatform.karya.utils.extensions.invisible
import com.karyaplatform.karya.utils.extensions.observe
import com.karyaplatform.karya.utils.extensions.viewLifecycleScope
import com.karyaplatform.karya.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.microtask_quiz.*
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
          mcqChipGroup.invisible()
        }

        QuestionType.text -> {
          mcqChipGroup.gone()
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
          mcqChipGroup.removeAllViews()
          mcqChipGroup.visible()

          mcqChipGroup.isSingleSelection = question.multiple == false
          val chipStyle =
            if (question.multiple == true) R.style.Widget_MaterialComponents_Chip_Filter else R.style.Widget_MaterialComponents_Chip_Choice

          question.options?.forEach { option ->

            val chip = Chip(requireContext())
            val chipDrawable = ChipDrawable.createFromAttributes(
              requireContext(),
              null,
              0,
              chipStyle
            )
            chip.setChipDrawable(chipDrawable)
            chip.text = option
            chip.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(ssp.dimen._14ssp))
            mcqChipGroup.addView(
              chip,
              RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
            chip.setOnCheckedChangeListener { compoundButton, checked ->
              viewModel.updateMCQResponse(option, checked)
            }
          }
        }
      }
    }
  }

  private fun setupListeners() {
    nextBtn.setOnClickListener {

      // Check for input is in range when question is of type numeric and range is given
      val question = viewModel.question.value
      if (question.required != null && question.required) {
        if (question.type == QuestionType.text) {
          if (question.numeric == true) {
            val value = try { Integer.parseInt(textResponseEt.text.toString()) } catch (e: Exception) { 0 }
            val range = viewModel.question.value.range
            if (range != null && range.isNotEmpty() && value !in range[0]..range[1]) {
              Toast.makeText(requireContext(), "Value should be in range ${range[0]} - ${range[1]}", Toast.LENGTH_SHORT)
                .show()
              return@setOnClickListener
            }
          }
        }
      }

      viewModel.submitResponse()
      textResponseEt.setText("")
    }

    textResponseEt.doAfterTextChanged {
      viewModel.updateTextResponse(textResponseEt.text.toString())
    }
  }
}
