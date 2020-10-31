package com.procurement.requisition.infrastructure.handler.v2.pcr.query.find.pmm

import com.procurement.requisition.application.extension.trySerialization
import com.procurement.requisition.application.service.Logger
import com.procurement.requisition.application.service.Transform
import com.procurement.requisition.application.service.find.pmm.FindProcurementMethodModalitiesService
import com.procurement.requisition.domain.failure.incident.InternalServerError
import com.procurement.requisition.infrastructure.handler.Action
import com.procurement.requisition.infrastructure.handler.Actions
import com.procurement.requisition.infrastructure.handler.CommandHandler
import com.procurement.requisition.infrastructure.handler.model.CommandDescriptor
import com.procurement.requisition.infrastructure.handler.model.response.ApiResponseV2
import com.procurement.requisition.infrastructure.handler.v2.AbstractHandlerV2
import com.procurement.requisition.infrastructure.handler.v2.pcr.query.find.pmm.model.FindProcurementMethodModalitiesRequest
import com.procurement.requisition.infrastructure.handler.v2.pcr.query.find.pmm.model.convert
import com.procurement.requisition.lib.fail.Failure
import com.procurement.requisition.lib.functional.Result

@CommandHandler
class FindProcurementMethodModalitiesHandler(
    override val logger: Logger,
    override val transform: Transform,
    private val findProcurementMethodModalities: FindProcurementMethodModalitiesService
) : AbstractHandlerV2() {

    override val action: Action = Actions.FIND_PROCUREMENT_METHOD_MODALITIES

    override fun execute(descriptor: CommandDescriptor): Result<String, Failure> {

        val command = descriptor.getCommand(FindProcurementMethodModalitiesRequest::convert)
            .onFailure { failure -> return failure }

        return findProcurementMethodModalities.find(command)
            .flatMap { result ->
                val response =
                    if (result.tender.procurementMethodModalities.isEmpty())
                        null
                    else
                        result.convert()

                ApiResponseV2.Success(version = descriptor.version, id = descriptor.id, result = response)
                    .trySerialization(transform)
                    .mapFailure { failure ->
                        InternalServerError(description = failure.description, reason = failure.reason)
                    }
            }
    }
}