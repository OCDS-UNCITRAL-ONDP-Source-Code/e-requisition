package com.procurement.requisition.infrastructure.handler.v1.get.tender.owner

import com.procurement.requisition.application.extension.trySerialization
import com.procurement.requisition.application.service.Logger
import com.procurement.requisition.application.service.Transform
import com.procurement.requisition.application.service.get.tender.owner.GetTenderOwnerService
import com.procurement.requisition.application.service.get.tender.owner.model.GetTenderOwnerCommand
import com.procurement.requisition.domain.failure.incident.InternalServerError
import com.procurement.requisition.infrastructure.handler.Action
import com.procurement.requisition.infrastructure.handler.CommandHandler
import com.procurement.requisition.infrastructure.handler.model.CommandDescriptor
import com.procurement.requisition.infrastructure.handler.model.response.ApiResponseV1
import com.procurement.requisition.infrastructure.handler.v1.AbstractHandlerV1
import com.procurement.requisition.infrastructure.web.v1.CommandsV1
import com.procurement.requisition.lib.fail.Failure
import com.procurement.requisition.lib.functional.Result

@CommandHandler
class GetTenderOwnerHandler(
    override val logger: Logger,
    override val transform: Transform,
    private val getTenderOwnerService: GetTenderOwnerService
) : AbstractHandlerV1() {

    override val action: Action = CommandsV1.CommandType.GET_TENDER_OWNER

    override fun execute(descriptor: CommandDescriptor): Result<String, Failure> {

        val context = getContext(descriptor.body.asJsonNode)
            .onFailure { failure -> return failure }

        val cpid = context.cpid
            .onFailure { return it }

        val ocid = context.ocid
            .onFailure { return it }

        val command = GetTenderOwnerCommand(cpid = cpid, ocid = ocid)

        return getTenderOwnerService.get(command)
            .flatMap { result ->
                ApiResponseV1.Success(
                    version = descriptor.version,
                    id = descriptor.id,
                    result = GetTenderOwnerResponse(result)
                )
                    .trySerialization(transform)
                    .mapFailure { failure ->
                        InternalServerError(description = failure.description, reason = failure.reason)
                    }
            }
    }
}
