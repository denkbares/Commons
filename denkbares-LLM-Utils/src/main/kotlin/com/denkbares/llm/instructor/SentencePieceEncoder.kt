package com.denkbares.llm.instructor

import ai.djl.sentencepiece.SpTextEmbedding
import ai.djl.sentencepiece.SpTokenizer
import java.nio.file.Path

class SentencePieceEncoder(pathToSentPiece: Path) {
    val tokenizer = SpTokenizer(pathToSentPiece.toFile().readBytes())
    val embedder = SpTextEmbedding.from(tokenizer)

    fun encode(text: String): LongArray {
        return embedder.preprocessTextToEmbed(listOf(text))
    }
}