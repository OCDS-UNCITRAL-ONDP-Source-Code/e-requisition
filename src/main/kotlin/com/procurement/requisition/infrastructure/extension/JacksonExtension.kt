package com.procurement.requisition.infrastructure.extension

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.NullNode
import com.procurement.requisition.domain.failure.error.JsonErrors
import com.procurement.requisition.lib.EnumElementProvider
import com.procurement.requisition.lib.EnumElementProvider.Companion.keysAsStrings
import com.procurement.requisition.lib.functional.Result
import com.procurement.requisition.lib.functional.Result.Companion.failure
import com.procurement.requisition.lib.functional.Result.Companion.success
import com.procurement.requisition.lib.functional.asSuccess
import java.math.BigDecimal

fun JsonNode.getOrNull(name: String): JsonNode? = if (this.has(name)) this.get(name) else null

fun JsonNode.tryGetAttribute(name: String): Result<JsonNode, JsonErrors> =
    get(name)
        ?.let { node ->
            if (node is NullNode)
                failure(
                    JsonErrors.DataTypeMismatch(
                        path = name,
                        actual = "null",
                        expected = "not null",
                        reason = null
                    )
                )
            else
                success(node)
        }
        ?: failure(JsonErrors.MissingRequiredAttribute(path = name, reason = null))

fun JsonNode.tryGetAttribute(name: String, type: JsonNodeType): Result<JsonNode, JsonErrors> =
    tryGetAttribute(name = name)
        .flatMap { node ->
            if (node.nodeType == type)
                success(node)
            else
                failure(
                    JsonErrors.DataTypeMismatch(
                        path = name,
                        expected = type.name,
                        actual = node.nodeType.name,
                        reason = null
                    )
                )
        }

fun JsonNode.tryGetTextAttribute(name: String): Result<String, JsonErrors> =
    tryGetAttribute(name = name, type = JsonNodeType.STRING)
        .map { it.asText() }

fun JsonNode.tryGetBigDecimalAttribute(name: String): Result<BigDecimal, JsonErrors> =
    tryGetAttribute(name = name, type = JsonNodeType.NUMBER)
        .map { it.decimalValue() }

fun <T> JsonNode.tryGetAttributeAsEnum(name: String, enumProvider: EnumElementProvider<T>):
    Result<T, JsonErrors> where T : Enum<T>,
                                T : EnumElementProvider.Key =
    tryGetTextAttribute(name)
        .flatMap { text ->
            enumProvider.orNull(text)
                ?.asSuccess<T, JsonErrors.UnknownValue>()
                ?: failure(
                    JsonErrors.UnknownValue(
                        path = name,
                        expectedValues = enumProvider.allowedElements.keysAsStrings(),
                        actualValue = text,
                        reason = null
                    )
                )
        }
