package org.jikvict.gradle.tasks

import org.jikvict.gradle.logic.GithubRetriever
import org.junit.jupiter.api.Test

class GithubRetrieverTests {

    @Test
    fun test() {
        val retriever = GithubRetriever("1", "https://github.com/JIkvict/JIkvictBackend.git")
        println(retriever.getOpenApiJson())
    }
}
