package com.procurement.requisition.infrastructure.handler.v2.pcr.query.get.currency

import com.procurement.requisition.application.extension.trySerialization
import com.procurement.requisition.application.service.Logger
import com.procurement.requisition.application.service.Transform
import com.procurement.requisition.application.service.get.tender.currency.GetTenderCurrencyService
import com.procurement.requisition.domain.failure.incident.InternalServerError
import com.procurement.requisition.infrastructure.handler.Action
import com.procurement.requisition.infrastructure.handler.Actions
import com.procurement.requisition.infrastructure.handler.CommandHandler
import com.procurement.requisition.infrastructure.handler.model.CommandDescriptor
import com.procurement.requisition.infrastructure.handler.model.response.ApiResponseV2
import com.procurement.requisition.infrastructure.handler.v2.AbstractHandlerV2
import com.procurement.requisition.infrastructure.handler.v2.pcr.query.get.currency.model.GetTenderCurrencyRequest
import com.procurement.requisition.infrastructure.handler.v2.pcr.query.get.currency.model.convert
import com.procurement.requisition.lib.fail.Failure
import com.procurement.requisition.lib.functional.Result

@CommandHandler
class GetTenderCurrencyHandler(
    override val logger: Logger,
    override val transform: Transform,
    private val getTenderCurrencyService: GetTenderCurrencyService
) : AbstractHandlerV2() {

    override val action: Action = Actions.GET_CURRENCY

    override fun execute(descriptor: CommandDescriptor): Result<String, Failure> {

        val command = descriptor.getCommand(GetTenderCurrencyRequest::convert)
            .onFailure { failure -> return failure }

        return getTenderCurrencyService.get(command)
            .flatMap { result ->
                ApiResponseV2.Success(version = descriptor.version, id = descriptor.id, result = result.convert())
                    .trySerialization(transform)
                    .mapFailure { failure ->
                        InternalServerError(description = failure.description, reason = failure.reason)
                    }
            }
    }
}