package org.akaza.openclinica.service;

import core.org.akaza.openclinica.bean.odmbeans.DiscrepancyNoteBean;
import core.org.akaza.openclinica.bean.submit.crfdata.*;
import core.org.akaza.openclinica.domain.datamap.*;
import core.org.akaza.openclinica.service.OCUserDTO;
import org.akaza.openclinica.controller.dto.DataImportReport;
import org.akaza.openclinica.controller.helper.table.ItemCountInForm;

import java.util.List;

public interface ImportValidationService {

    void validateQuery(DiscrepancyNoteBean discrepancyNotesBean,ItemData itemData, List<OCUserDTO> accepatableUsers);

    void validateItem(ImportItemDataBean itemDataBean, CrfBean crf, ImportItemGroupDataBean itemGroupDataBean, ItemCountInForm itemCountInForm);

    void validateEventCrf(StudySubject studySubject, StudyEvent studyEvent, FormLayout formLayout, EventDefinitionCrf edc);

    void validateSdvStatus(StudySubject studySubject, FormDataBean formDataBean, EventCrf eventCrf, Boolean proceedToSdv, Boolean sdvStatusUpdatedInternally);

    void validateEventDefnCrf(Study tenantStudy, StudyEventDefinition studyEventDefinition, CrfBean crf);

    void validateForm(FormDataBean formDataBean, Study tenantStudy, StudyEventDefinition studyEventDefinition);

    void validateSignatureForStudyEvent(StudyEventDataBean studyEventDataBean, StudyEvent studyEvent, StudySubject studySubject);

    void validateItemGroupRemoved(ImportItemGroupDataBean itemGroupDataBean, ItemGroup itemGroup);
}
