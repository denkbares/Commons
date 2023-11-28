package com.denkbares.llm.instructor

import ai.djl.ndarray.NDList
import ai.djl.ndarray.types.Shape
import ai.djl.translate.NoBatchifyTranslator
import ai.djl.translate.TranslatorContext
import java.nio.file.Path

class InstuctorTransformerTranslator(val sentencePieceEncoder: SentencePieceEncoder) :
    NoBatchifyTranslator<InstructorInput, FloatArray> {


    override fun processInput(ctx: TranslatorContext, input: InstructorInput): NDList {
        var tokens = sentencePieceEncoder.encode(input.instruction + input.text)
        val contextTokens = sentencePieceEncoder.encode(input.instruction)
        tokens = longArrayOf(*tokens, 1L)
        println(tokens.size)
        val inputIds = tokens
        val attentionMask = encodeAttentionMask(inputIds.toList())
        val attentionMaskContext = longArrayOf(contextTokens.size.toLong())


        return ctx.ndManager.let {
            val inputIdsT = it.create(inputIds, Shape(1, tokens.size.toLong()))
            val attentionMaskT = it.create(attentionMask, Shape(1, tokens.size.toLong()))
            val contextMaskT = it.create(attentionMaskContext, Shape(1))

            NDList(inputIdsT, attentionMaskT, contextMaskT)
        }
    }

    private fun encodeAttentionMask(tokens: List<Long>) =
        LongArray(tokens.size) { 0L }.also {
            tokens.forEachIndexed { i, t ->
                it[i] = 1
            }
        }

    override fun processOutput(ctx: TranslatorContext, list: NDList): FloatArray {
        val first = list[4].toFloatArray()
        return first
    }


}