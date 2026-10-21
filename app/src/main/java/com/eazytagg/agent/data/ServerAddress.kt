package com.eazytagg.agent.data

import java.net.URI

internal object ServerAddress {
    fun candidates(input: String): List<String> = try {
        val raw = input.trim()
        require("://" !in raw || raw.startsWith("http://", true) || raw.startsWith("https://", true))
        val base = Api.normalizeBase(raw)
        val uri = URI(base)
        require(uri.scheme.lowercase() in listOf("http", "https") && !uri.host.isNullOrBlank())
        require(uri.userInfo == null && uri.query == null && uri.fragment == null)
        require(uri.port == -1 || uri.port in 1..65535)
        if (uri.path.isNotBlank() && uri.path != "/") listOf(base)
        else listOf(base + "PDynamics/", base, base + "eazytagg_test/")
    } catch (_: Exception) { emptyList() }
}
