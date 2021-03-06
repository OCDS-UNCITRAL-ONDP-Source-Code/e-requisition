package com.procurement.requisition.application.repository.pcr

import com.procurement.requisition.domain.model.PCR
import com.procurement.requisition.lib.fail.Failure
import com.procurement.requisition.lib.functional.Result

interface PCRSerializer {
    fun build(pcr: PCR): Result<String, Failure>
}
