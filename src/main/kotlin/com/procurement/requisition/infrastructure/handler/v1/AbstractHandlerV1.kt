package com.procurement.requisition.infrastructure.handler.v1

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.requisition.domain.failure.error.JsonErrors
import com.procurement.requisition.infrastructure.extension.tryGetAttribute
import com.procurement.requisition.infrastructure.handler.AbstractHandler
import com.procurement.requisition.infrastructure.handler.model.ApiVersion
import com.procurement.requisition.lib.functional.Result

abstract class AbstractHandlerV1 : AbstractHandler() {

    final override val version: ApiVersion
        get() = ApiVersion(1, 0, 0)

    fun getContext(node: JsonNode): Result<CommandContext, JsonErrors> = node.tryGetAttribute("context")
        .map { CommandContext(it) }

    inline fun <reified T> getData(node: JsonNode): Result<T, JsonErrors> = node.tryGetAttribute("data")
        .flatMap { data ->
            transform.tryMapping(data, T::class.java)
                .mapFailure { failure ->
                    JsonErrors.Parsing(failure.reason)
                }
        }
}
