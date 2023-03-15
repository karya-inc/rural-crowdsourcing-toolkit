package com.microsoft.research.karya.ui.scenarios.quiz

enum class Type {
  text,
  mcq,
  invalid
}

enum class OptionType {
  text,
  image,
  invalid
}

data class Question(
  val type: Type,
  val optionType: OptionType = OptionType.invalid,
  val question: String = "",
  val questionImage: String? = null,
  val key: String = "",
  val long: Boolean? = false,
  val options: ArrayList<String>? = arrayListOf(),
  val multiple: Boolean? = false
)
