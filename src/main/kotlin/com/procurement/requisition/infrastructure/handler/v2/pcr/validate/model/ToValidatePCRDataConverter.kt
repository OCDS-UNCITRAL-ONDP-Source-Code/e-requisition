package com.procurement.requisition.infrastructure.handler.v2.pcr.validate.model

import com.procurement.requisition.application.service.validate.model.ValidatePCRDataCommand
import com.procurement.requisition.domain.failure.error.JsonErrors
import com.procurement.requisition.domain.failure.error.repath
import com.procurement.requisition.domain.model.award.AwardCriteria
import com.procurement.requisition.domain.model.award.AwardCriteriaDetails
import com.procurement.requisition.domain.model.classification.ClassificationScheme
import com.procurement.requisition.domain.model.document.DocumentType
import com.procurement.requisition.domain.model.tender.ProcurementMethodModality
import com.procurement.requisition.domain.model.tender.TargetRelatesTo
import com.procurement.requisition.domain.model.tender.conversion.ConversionRelatesTo
import com.procurement.requisition.domain.model.tender.criterion.CriterionRelatesTo
import com.procurement.requisition.domain.model.tender.item.ItemId
import com.procurement.requisition.domain.model.tender.lot.LotId
import com.procurement.requisition.infrastructure.handler.converter.asEnum
import com.procurement.requisition.infrastructure.handler.converter.asLocalDateTime
import com.procurement.requisition.lib.failureIfEmpty
import com.procurement.requisition.lib.functional.Result
import com.procurement.requisition.lib.functional.Result.Companion.failure
import com.procurement.requisition.lib.functional.asSuccess
import com.procurement.requisition.lib.mapIndexedOrEmpty

fun ValidatePCRDataRequest.convert(): Result<ValidatePCRDataCommand, JsonErrors> = tender.convert()
    .onFailure { return it.repath(path = "/tender") }
    .let { ValidatePCRDataCommand(it).asSuccess() }

/**
 * Tender
 */
fun ValidatePCRDataRequest.Tender.convert(): Result<ValidatePCRDataCommand.Tender, JsonErrors> {
    val classification = classification.convert()
        .onFailure { return it.repath(path = "/classification") }
    val lots = lots
        .failureIfEmpty { return failure(JsonErrors.EmptyArray().repath(path = "lots")) }
        .mapIndexedOrEmpty { idx, lot ->
            lot.convert().onFailure { return it.repath(path = "/lots[$idx]") }
        }
    val items = items
        .failureIfEmpty { return failure(JsonErrors.EmptyArray().repath(path = "items")) }
        .mapIndexedOrEmpty { idx, item ->
            item.convert().onFailure { return it.repath(path = "/items[$idx]") }
        }
    val targets = targets
        .failureIfEmpty { return failure(JsonErrors.EmptyArray().repath(path = "targets")) }
        .mapIndexedOrEmpty { idx, target ->
            target.convert().onFailure { return it.repath(path = "/targets[$idx]") }
        }
    val criteria = criteria
        .failureIfEmpty { return failure(JsonErrors.EmptyArray().repath(path = "criteria")) }
        .mapIndexedOrEmpty { idx, criterion ->
            criterion.convert().onFailure { return it.repath(path = "/criteria[$idx]") }
        }
    val conversions = conversions
        .failureIfEmpty { return failure(JsonErrors.EmptyArray().repath(path = "conversions")) }
        .mapIndexedOrEmpty { idx, conversion ->
            conversion.convert().onFailure { return it.repath(path = "/conversions[$idx]") }
        }
    val procurementMethodModalities = procurementMethodModalities
        .failureIfEmpty { return failure(JsonErrors.EmptyArray().repath(path = "procurementMethodModalities")) }
        .mapIndexedOrEmpty { idx, procurementMethodModality ->
            procurementMethodModality.asEnum(target = ProcurementMethodModality)
                .onFailure { return it.repath(path = "/procurementMethodModalities[$idx]") }
        }
    val awardCriteria = awardCriteria.asEnum(target = AwardCriteria)
        .onFailure { return it.repath(path = "/awardCriteria") }
    val awardCriteriaDetails =
        awardCriteriaDetails.asEnum(target = AwardCriteriaDetails)
            .onFailure { return it.repath(path = "/awardCriteriaDetails") }

    val documents = documents
        .failureIfEmpty { return failure(JsonErrors.EmptyArray().repath(path = "documents")) }
        .mapIndexedOrEmpty { idx, document ->
            document.convert().onFailure { return it.repath(path = "/documents[$idx]") }
        }

    return ValidatePCRDataCommand.Tender(
        title = title,
        description = description,
        classification = classification,
        lots = lots,
        items = items,
        targets = targets,
        criteria = criteria,
        conversions = conversions,
        procurementMethodModalities = procurementMethodModalities,
        awardCriteria = awardCriteria,
        awardCriteriaDetails = awardCriteriaDetails,
        documents = documents
    ).asSuccess()
}

/**
 * Classification
 */
fun ValidatePCRDataRequest.Classification.convert(): Result<ValidatePCRDataCommand.Classification, JsonErrors> {
    val scheme = scheme.asEnum(target = ClassificationScheme)
        .onFailure { return it.repath(path = "/scheme") }
    return ValidatePCRDataCommand.Classification(id = id, scheme = scheme).asSuccess()
}

/**
 * Unit
 */
fun ValidatePCRDataRequest.Unit.convert(): Result<ValidatePCRDataCommand.Unit, JsonErrors> =
    ValidatePCRDataCommand.Unit(id = this.id).asSuccess()

/**
 * Lot
 */
fun ValidatePCRDataRequest.Tender.Lot.convert(): Result<ValidatePCRDataCommand.Tender.Lot, JsonErrors> {
    if (!LotId.validate(id))
        return failure(
            JsonErrors.DataFormatMismatch(actualValue = id, expectedFormat = LotId.pattern)
                .repath(path = "/id")
        )
    val classification = classification.convert().onFailure { return it.repath(path = "/classification") }
    val variants = variants.map { variant ->
        variant.convert().onFailure { return it.repath(path = "/variants") }
    }

    return ValidatePCRDataCommand.Tender.Lot(
        id = id,
        internalId = internalId,
        title = title,
        description = description,
        classification = classification,
        variants = variants,
    ).asSuccess()
}

fun ValidatePCRDataRequest.Tender.Lot.Variant.convert(): Result<ValidatePCRDataCommand.Tender.Lot.Variant, JsonErrors> =
    ValidatePCRDataCommand.Tender.Lot.Variant(hasVariants = hasVariants, variantsDetails = variantsDetails).asSuccess()

/**
 * Item
 */
fun ValidatePCRDataRequest.Tender.Item.convert(): Result<ValidatePCRDataCommand.Tender.Item, JsonErrors> {
    if (!ItemId.validate(id))
        return failure(
            JsonErrors.DataFormatMismatch(actualValue = id, expectedFormat = ItemId.pattern).repath(path = "/id")
        )
    val classification = classification.convert().onFailure { return it.repath(path = "/classification") }
    val unit = unit.convert().onFailure { return it.repath(path = "/unit") }
    if (!LotId.validate(relatedLot))
        return failure(
            JsonErrors.DataFormatMismatch(actualValue = relatedLot, expectedFormat = LotId.pattern)
                .repath(path = "/relatedLot")
        )

    return ValidatePCRDataCommand.Tender.Item(
        id = id,
        internalId = internalId,
        description = description,
        quantity = quantity,
        classification = classification,
        unit = unit,
        relatedLot = relatedLot,
    ).asSuccess()
}

/**
 * Target
 */
fun ValidatePCRDataRequest.Tender.Target.convert(): Result<ValidatePCRDataCommand.Tender.Target, JsonErrors> {
    val relatesTo = relatesTo.asEnum(target = TargetRelatesTo)
        .onFailure { return it.repath(path = "/relatesTo") }

    val observations = observations
        .failureIfEmpty { return failure(JsonErrors.EmptyArray().repath(path = "observations")) }
        .mapIndexedOrEmpty { observationIdx, observation ->
            observation.convert().onFailure { return it.repath(path = "/observations[$observationIdx]") }
        }

    return ValidatePCRDataCommand.Tender.Target(
        id = id,
        title = title,
        relatesTo = relatesTo,
        relatedItem = relatedItem,
        observations = observations
    ).asSuccess()
}

fun ValidatePCRDataRequest.Tender.Target.Observation.convert():
    Result<ValidatePCRDataCommand.Tender.Target.Observation, JsonErrors> {

    val period = period?.convert()?.onFailure { return it.repath(path = "/period") }
    val unit = unit.convert().onFailure { return it.repath(path = "/unit") }
    val dimensions = dimensions?.convert()?.onFailure { return it.repath(path = "/dimensions") }

    return ValidatePCRDataCommand.Tender.Target.Observation(
        id = id,
        period = period,
        measure = measure,
        unit = unit,
        dimensions = dimensions,
        notes = notes,
        relatedRequirementId = relatedRequirementId,
    ).asSuccess()
}

fun ValidatePCRDataRequest.Tender.Target.Observation.Period.convert():
    Result<ValidatePCRDataCommand.Tender.Target.Observation.Period, JsonErrors> {

    val startDate = startDate?.asLocalDateTime()?.onFailure { return it.repath(path = "/startDate") }
    val endDate = endDate?.asLocalDateTime()?.onFailure { return it.repath(path = "/endDate") }
    return ValidatePCRDataCommand.Tender.Target.Observation.Period(startDate = startDate, endDate = endDate).asSuccess()
}

fun ValidatePCRDataRequest.Tender.Target.Observation.Dimensions.convert():
    Result<ValidatePCRDataCommand.Tender.Target.Observation.Dimensions, JsonErrors> =
    ValidatePCRDataCommand.Tender.Target.Observation.Dimensions(requirementClassIdPR = requirementClassIdPR).asSuccess()

/**
 * Criterion
 */
fun ValidatePCRDataRequest.Tender.Criterion.convert(): Result<ValidatePCRDataCommand.Tender.Criterion, JsonErrors> {
    val relatesTo = relatesTo?.asEnum(target = CriterionRelatesTo)
        ?.onFailure { return it.repath(path = "/relatesTo") }
    val requirementGroups = requirementGroups
        .failureIfEmpty { return failure(JsonErrors.EmptyArray().repath(path = "requirementGroups")) }
        .mapIndexedOrEmpty { idx, requirementGroup ->
            requirementGroup.convert().onFailure { return it.repath(path = "/requirementGroups[$idx]") }
        }

    return ValidatePCRDataCommand.Tender.Criterion(
        id = id,
        title = title,
        description = description,
        relatesTo = relatesTo,
        relatedItem = relatedItem,
        requirementGroups = requirementGroups,
    ).asSuccess()
}

fun ValidatePCRDataRequest.Tender.Criterion.RequirementGroup.convert(): Result<ValidatePCRDataCommand.Tender.Criterion.RequirementGroup, JsonErrors> =
    ValidatePCRDataCommand.Tender.Criterion.RequirementGroup(
        id = id,
        description = description,
        requirements = requirements.toList(),
    ).asSuccess()

/**
 * Conversion
 */
fun ValidatePCRDataRequest.Tender.Conversion.convert(): Result<ValidatePCRDataCommand.Tender.Conversion, JsonErrors> {
    val relatesTo = relatesTo.asEnum(target = ConversionRelatesTo)
        .onFailure { return it.repath(path = "/relatesTo") }

    val coefficients = coefficients
        .failureIfEmpty { return failure(JsonErrors.EmptyArray().repath(path = "coefficients")) }
        .mapIndexedOrEmpty { idx, coefficient ->
            coefficient.convert().onFailure { return it.repath(path = "/coefficients[$idx]") }
        }

    return ValidatePCRDataCommand.Tender.Conversion(
        id = id,
        relatesTo = relatesTo,
        relatedItem = relatedItem,
        rationale = rationale,
        description = description,
        coefficients = coefficients
    ).asSuccess()
}

fun ValidatePCRDataRequest.Tender.Conversion.Coefficient.convert(): Result<ValidatePCRDataCommand.Tender.Conversion.Coefficient, JsonErrors> =
    ValidatePCRDataCommand.Tender.Conversion.Coefficient(id = id, value = value, coefficient = coefficient).asSuccess()

/**
 * Document
 */
fun ValidatePCRDataRequest.Tender.Document.convert(): Result<ValidatePCRDataCommand.Tender.Document, JsonErrors> {
    val documentType = documentType.asEnum(target = DocumentType)
        .onFailure { return it.repath(path = "/documentType") }
    val relatedLots = relatedLots
        .failureIfEmpty { return failure(JsonErrors.EmptyArray().repath(path = "relatedLots")) }
        .mapIndexedOrEmpty { idx, relatedLot ->
            if (LotId.validate(relatedLot))
                relatedLot
            else
                return failure(
                    JsonErrors.DataFormatMismatch(actualValue = relatedLot, expectedFormat = LotId.pattern)
                        .repath(path = "/relatedLots[$idx]")
                )
        }

    return ValidatePCRDataCommand.Tender.Document(
        id = id,
        documentType = documentType,
        title = title,
        description = description,
        relatedLots = relatedLots
    ).asSuccess()
}
