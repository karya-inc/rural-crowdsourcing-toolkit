package com.karyaplatform.karya.ui.scenarios.quiz

enum class QuestionType {
  text,
  mcq,
  invalid
}

data class Question(
  val type: QuestionType,
  val question: String = "",
  val key: String = "",
  val long: Boolean? = false,
  val numeric: Boolean? = false,
  val range: ArrayList<Int>? = arrayListOf(),
  val options: ArrayList<String>? = arrayListOf(),
  val multiple: Boolean? = false
)
