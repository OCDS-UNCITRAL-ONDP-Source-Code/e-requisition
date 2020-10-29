package com.procurement.requisition.application.service.find.pmm.error

import com.procurement.requisition.domain.model.Cpid
import com.procurement.requisition.domain.model.Ocid
import com.procurement.requisition.lib.fail.Failure

sealed class FindProcurementMethodModalitiesErrors(
    override val code: String,
    override val description: String
) : Failure.Error() {

    override val reason: Exception? = null

    class PCRNotFound(cpid: Cpid, ocid: Ocid) :
        FindProcurementMethodModalitiesErrors(
            code = "VR.COM-17.7.1",
            description = "PCR by cpid '${cpid.underlying}' and ocid '${ocid.underlying}' is not found."
        )
}
