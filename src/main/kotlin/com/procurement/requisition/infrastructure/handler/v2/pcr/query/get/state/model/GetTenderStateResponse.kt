package com.procurement.requisition.infrastructure.handler.v2.pcr.query.get.state.model

import com.fasterxml.jackson.annotation.JsonProperty

data class GetTenderStateResponse(
    @field:JsonProperty("status") @param:JsonProperty("status") val status: String,
    @field:JsonProperty("statusDetails") @param:JsonProperty("statusDetails") val statusDetails: String
)
