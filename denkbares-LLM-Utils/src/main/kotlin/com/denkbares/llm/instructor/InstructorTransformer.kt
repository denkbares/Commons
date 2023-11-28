package com.denkbares.llm.instructor

import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import java.nio.file.Path
import java.nio.file.Paths


data class InstructorInput(val instruction: String, val text: String)

class InstructorTransformer(val model: ZooModel<InstructorInput, FloatArray>) {

    fun embed(text: String, instruction: String): FloatArray {
        return model.newPredictor().predict(InstructorInput(instruction, text))
    }

    fun embed(input:InstructorInput): FloatArray {
        return model.newPredictor().predict(input)
    }

    companion object {

        val vocabularyName = "ruedee-spiece.model"
        fun fromPath(modelPath: Path): InstructorTransformer? {

            val vocabularyPath = Path.of(modelPath.parent.toString(), vocabularyName)

            val inputEncoder =
                SentencePieceEncoder(vocabularyPath)

            val modelUrl = modelPath.toString()
            val criteria = Criteria.builder()
                .setTypes(InstructorInput::class.java, FloatArray::class.java)
                .optModelUrls(modelUrl)
                .optTranslator(InstuctorTransformerTranslator(inputEncoder))
                .optEngine("OnnxRuntime")
                .build()
            val model: ZooModel<InstructorInput, FloatArray> = criteria.loadModel()

            return InstructorTransformer(model)
        }
    }
}


fun main(args: Array<String>) {


    val modelFile = Paths.get("/Users/mkrug/git/denkbares/troi-llm/INSTRUCTOR-XL/outnnx.onnx")

    val instructor = InstructorTransformer.fromPath(modelFile) ?: return

    val input = InstructorInput(
        "Create an embedding of this description for retrieval",
        "The quick brown fox jumps over the lazy dog"
    )

    val prediction: FloatArray = instructor.embed(input.text, input.instruction)
}