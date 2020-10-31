package com.procurement.requisition.infrastructure.handler.v2.pcr.query.get.state.model

import com.procurement.requisition.application.service.get.tender.state.model.GetTenderStateCommand
import com.procurement.requisition.domain.failure.error.JsonErrors
import com.procurement.requisition.domain.failure.error.repath
import com.procurement.requisition.infrastructure.handler.converter.asCpid
import com.procurement.requisition.infrastructure.handler.converter.asSingleStageOcid
import com.procurement.requisition.lib.functional.Result
import com.procurement.requisition.lib.functional.asSuccess

fun GetTenderStateRequest.convert(): Result<GetTenderStateCommand, JsonErrors> {
    val cpid = cpid.asCpid().onFailure { return it.repath(path = "/cpid") }
    val ocid = ocid.asSingleStageOcid().onFailure { return it.repath(path = "/ocid") }
    return GetTenderStateCommand(cpid = cpid, ocid = ocid).asSuccess()
}
