package com.procurement.requisition.infrastructure.exception

class RequirementValueException(requirementValue: String, description: String = "") :
    RuntimeException("Incorrect value in requirement: '$requirementValue'. $description")
