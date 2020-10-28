package org.akaza.openclinica.web.restful.errors;

public class ErrorConstants {

    /**
     * this section is "synchronized" from SBS
     */
    public static final String ERR_CONCURRENCY_FAILURE = "errorCode.concurrencyFailure";
    public static final String ERR_ACCESS_DENIED = "errorCode.accessDenied";
    public static final String ERR_VALIDATION = "errorCode.validation";
    public static final String ERR_METHOD_NOT_SUPPORTED = "errorCode.methodNotSupported";
    public static final String ERR_INTERNAL_SERVER_ERROR = "errorCode.internalServerError";

    public static final String UUID_EXISTS = "errorCode.uuidExists";
    public static final String STUDY_DOESNT_EXIST = "errorCode.studyDoesntExist";
    public static final String NAME_REQUIRED = "errorCode.nameRequired";
    public static final String NAME_NOT_UNIQUE = "errorCode.nameNotUnique";
    public static final String STATUS_REQUIRED = "errorCode.statusRequired";
    public static final String UNIQUE_IDENTIFIER_REQUIRED = "errorCode.uniqueIdentifierRequired";
    public static final String UNIQUE_IDENTIFIER_NOT_UNIQUE = "errorCode.uniqueIdentifierNotUnique";
    public static final String PRINCIPAL_INVESTIGATOR_REQUIRED = "errorCode.principalInvestigatorRequired";
    public static final String EXPECTED_ENROLLMENT_REQUIRED = "errorCode.expectedEnrollmentRequired";
    public static final String STUDY_ENVIRONMENT_UUID_DOESNT_EXIST = "errorCode.studyEnvironmentUuidDoesntExist";
    public static final String STUDY_ENVIRONMENT_SITE_DOESNT_EXIST = "errorCode.studyEnvironmentSiteDoesntExist";
    public static final String STUDY_SITE_DOESNT_EXIST = "errorCode.studySiteDoesntExist";
    public static final String SITE_DOESNT_EXIST = "errorCode.siteDoesntExist";
    public static final String SITE_NAME_ALREADY_EXISTS = "errorCode.siteNameAlreadyExists";
    public static final String START_DATE_AFTER_END_DATE = "errorCode.startDateAfterEndDate";
    public static final String PARTICIPANT_ID_TEMPLATE_TOO_LONG = "errorCode.participantIdTemplateTooLong";

    /**
     * this is new for RunTime
     */
    public static final String ERR_NOT_XML_FILE = "errorCode.notXMLfile";
    public static final String ERR_NOT_CSV_FILE = "errorCode.notCSVfile";
    public static final String ERR_BLANK_FILE = "errorCode.blankFile";
    public static final String ERR_NO_MAPPING_FILE = "errorCode.noMappingfile";
    public static final String ERR_NOT_SUPPORTED_FILE_FORMAT = "errorCode.notSupportedFileFormat";
    public static final String ERR_XML_NOT_WELL_FORMED = "errorCode.xmlNotWellFormed";

    public static final String ERR_STUDY_OID_MISSING="errorCode.studyOidMissing";
    public static final String ERR_STUDY_NOT_EXIST = "errorCode.studyNotExist";
    public static final String ERR_STUDY_NOT_AVAILABLE = "errorCode.studyNotAvailable";
    public static final String ERR_SITE_NOT_AVAILABLE = "errorCode.siteNotAvailable";

    public static final String ERR_SITE_NOT_EXIST = "errorCode.siteNotExist";
    public static final String ERR_NO_ROLE_SETUP = "errorCode.noRoleSetUp";
    public static final String ERR_NO_SUFFICIENT_PRIVILEGES = "errorCode.noSufficientPrivileges";
    public static final String ERR_PARTICIPANTS_ENROLLMENT_CAP_REACHED = "errorCode.participantsEnrollmentCapReached";

    public static final String ERR_NO_SUBJECT_FOUND = "errorCode.noSubjectFound";
    public static final String ERR_EVENT_NOT_EXIST = "errorCode.eventNotExist";
    public static final String ERR_EVENT_NOT_AVAILABLE = "errorCode.eventNotAvailable";
    public static final String ERR_FORM_NOT_AVAILABLE = "errorCode.formNotAvailable";

    public static final String ERR_NOT_INTEGER = "errorCode.notInteger";
    public static final String ERR_ORDINAL_TOO_BIG = "errorCode.ordinalTooBig";

    public static final String ERR_NO_START_DATE = "errorCode.noStartDate";
    public static final String ERR_START_DATE = "errorCode.startDateError";
    public static final String ERR_PARSE_DATE = "errorCode.dateParsedError";

    public static final String INVALID_DELIMITER = "errorCode.invalidDelimiter";

    public static final String ERR_END_DATE_BEFORE_START_DATE = "errorCode.endDateBeforeStartDate";
    public static final String ERR_STUDY_LOCKED = "errorCode.studyLocked";

    public static final String ERR_SUBJECT_REMOVED = "errorCode.subjectRemoved";
    public static final String ERR_WRONG_EVENT_TYPE = "errorCode.wrongEventType";
    public static final String ERR_ORDINAL_NOT_ONE_FOR_NONREPEATING = "errorCode.ordinalNot1ForNONRepeating";
    public static final String ERR_NON_REPEATING_ALREADY_EXISIT = "errorCode.nonRepeatingAlreadyExist";
    public static final String ERR_ALREADY_EXISIT = "errorCode.alreadyExist";

    public static final String ERR_GREATER_THAN_MAX_ORDINAL = "errorCode.greaterThanMaxSampleOrdinal";
    public static final String ERR_EVENT_NOT_ACTIVE = "errorCode.notActive";
    public static final String ERR_LOG_FILE = "errorCode.logFile";
    public static final String ERR_DATA_MISSING_PIPE = "errorCode.dataRowMissingPipe";
    public static final String ERR_STUDY_NOT_Valid_OID = "errorCode.studyNotValidOid";
    public static final String ERR_SITE_NOT_Valid_OID = "errorCode.siteNotValidOid";
    public static final String ERR_STUDY_TO_SITE_NOT_Valid_OID = "errorCode.studyTositeNotValidOid";
    public static final String ERR_PARTICIPATE_INACTIVE = "errorCode.participateInActive";
   
    public static final String ERR_INVALID_XML_FILE = "errorCode.invalidXMLFile";
    public static final String ERR_FILE_FORMAT_NOT_SUPPORTED = "errorCode.fileFormatNotSupported";

    public static final String ERR_MISSING_START_DATE = "errorCode.missingStartDate";
    public static final String ERR_INVALID_START_DATE = "errorCode.invalidStartDate";
    public static final String ERR_INVALID_END_DATE = "errorCode.invalidEndDate";
    public static final String ERR_EVENT_REPEAT_KEY_TOO_LARGE = "errorCode.eventRepeatKeyTooLarge";
    public static final String ERR_INVALID_EVENT_REPEAT_KEY = "errorCode.invalidStudyEventRepeatKey";

    public static final String ERR_ITEM_TYPE_NOT_SUPPORTED = "errorCode.itemTypeNotSupportedInImport";
    public static final String ERR_IMPORT_FAILED = "errorCode.importFailed";
    public static final String ERR_VALUE_CHOICE_NOT_FOUND = "errorCode.valueChoiceCodeNotFound";
    public static final String ERR_INVALID_DATE_FORMAT = "errorCode.invalidDateFormat";
    public static final String ERR_VALUE_TYPE_MISMATCH = "errorCode.valueTypeMismatch";
    public static final String ERR_VALUE_TOO_LONG = "errorCode.valueTooLong";
    public static final String ERR_MISSING_VALUE = "errorCode.missingValue";
    public static final String ERR_ITEM_NOT_FOUND = "errorCode.itemNotFound";

    public static final String ERR_MISSING_PARTICIPANT_ID = "errorCode.missingParticipantID";
    public static final String ERR_PARTICIPANT_NOT_FOUND = "errorCode.participantNotFound";
    public static final String ERR_PARTICIPANT_NOT_AVAILABLE = "errorCode.participantNotAvailable";
    public static final String ERR_PARTICIPANT_ID_NOT_AVAILABLE = "errorCode.participantIDNotAvailable";
    public static final String ERR_PARTICIPANT_ID_ALREADY_EXISTS="errorCode.participantIDAlreadyExists";
    public static final String ERR_PARTICIPANT_IDENTIFIERS_MISMATCH = "errorCode.participantIdentiersMismatch";
    public static final String ERR_MISSING_STUDYEVENTOID = "errorCode.missingStudyEventOID";
    public static final String ERR_INVALID_STUDYEVENTOID = "errorCode.invalidStudyEventOID";
    public static final String ERR_MISSING_FORMOID = "errorCode.missingFormOid";
    public static final String ERR_FORMOID_NOT_FOUND = "errorCode.formOIDNotFound";
    public static final String ERR_FORMLAYOUTOID_NOT_FOUND = "errorCode.formLayoutOIDNotFound";
    public static final String ERR_FORM_STATUS_NOT_VALID = "errorCode.formStatusNotValid";
    public static final String ERR_FORM_ALREADY_COMPLETE = "errorCode.formAlreadyComplete";
    public static final String ERR_ITEMGROUPOID_NOT_FOUND = "errorCode.itemGroupOIDNotFound";
    public static final String ERR_MISSING_ITEMGROUPOID = "errorCode.missingItemGroupOID";
    public static final String ERR_ITEMGROUP_REPEATKEY_TOO_LARGE = "errorCode.itemGroupRepeatKeyTooLarge";
    public static final String ERR_ITEMGROUP_REPEATKEY_NOT_VALID = "errorCode.groupRepeatKeyNotValid";
    public static final String ERR_ITEMGROUP_REPEATKEY_LESS_THAN_ONE = "errorCode.groupRepeatKeyLessThanOne";
    public static final String ERR_ITEMGROUP_REPEATKEY_LARGER_THAN_ONE = "errorCode.repeatKeyLargerThanOne";
    public static final String ERR_ITEMGROUP_IS_ALREADY_REMOVED = "errorCode.itemGroupIsAlreadyRemoved";

    public static final String ERR_MULTIPLE_PARTICIPANTS_FOUND = "errorCode.multipleParticipantsFound";
    public static final String ERR_REPEAT_KEY_AND_FORM_MISMATCH = "errorCode.repeatKeyAndFormMismatch";

    public static final String ERR_INVALID_GROUP_REPEAT_KEY = "errorCode.invalidItemGroupRepeatKey";
    public static final String ERR_ITEMGROUP_REPEAT_KEY_TOO_LARGE = "errorCode.repeatKeyTooLarge";

    public static final String ERR_SUBJECT_DATA_MISSING = "errorCode.subjectDataMissing";
    public static final String ERR_COMMON_EVENTS_CANNOT_BE_SCHEDULED = "errorCode.commonEventsCannotBeScheduled";

    public static final String ERR_SUBJECT_DOES_NOT_CONTAIN_EVENTDATA = "errorCode.subjectDoesNotContainStudyEventData";
    public static final String ERR_EVENT_DOES_NOT_CONTAIN_FORMDATA = "errorCode.eventDoesNotContainFormData";
    public static final String ERR_FORM_DOES_NOT_CONTAIN_ITEMGROUPDATA = "errorCode.formDoesNotContainItemGroupData";
    public static final String ERR_ITEMGROUP_DOES_NOT_CONTAIN_ITEMDATA = "errorCode.itemGroupDoesNotContainItemData";

    public static final String ERR_EVENT_STATUS_NOT_NULL = "errorCode.eventStatusNotNull";
    public static final String ERR_EVENT_REPEAT_KEY_NOT_NULL = "errorCode.eventRepeatKeyNotNull";

    public static final String ERR_EVENT_ALREADY_EXISTS_CANT_SCHEDULE = "errorCode.eventAlreadyExistsCan'tSchedule";
    public static final String ERR_EVENT_DOES_NOT_EXISTS_TO_UPDATE = "errorCode.eventDoesNotExistsToUpdate";

    public static final String ERR_INVALID_EVENT_STATUS = "errorCode.invalidEventStatus";
    public static final String ERR_INVALID_EVENT_TRANSITION_STATUS = "errorCode.invalidEventTransitionStatus";

    public static final String ERR_END_DATE_IS_BEFORE_START_DATE = "errorCode.endDateIsBeforeStartDate";

    public static final String ERR_REPEAT_KEY_REQUIRED = "errorCode.repeatKeyRequired";
    public static final String ERR_STUDY_EVENT_REPEAT_NOT_FOUND = "errorCode.studyEventRepeatNotFound";
    public static final String ERR_EVENT_ALREADY_EXISTS = "errorCode.eventAlreadyExists";

    public static final String ERR_INVALID_REPEAT_KEY = "errorCode.invalidRepeatKey";
    public static final String ERR_GROUP_REPEAT_KEY_TOO_LARGE = "errorCode.groupRepeatKeyTooLarge";

    public static final String ERR_MISSING_STUDY_EVENT_REPEAT_KEY = "errorCode.missingStudyEventRepeatKey";
    public static final String ERR_FORM_MISSING_STUDY_EVENT_CONSTRUCT = "errorCode.formMissingStudyEventConstruct";
    public static final String ERR_FORMLAYOUTOID_NOT_AVAILABLE = "errorCode.formLayoutOIDNotAvailable";

    public static final String ERR_MISSING_PARTICIPANT_ID_DATA = "errorCode.missingParticipantIdData";
    public static final String ERR_MISSING_STUDY_EVENT_OID_DATA = "errorCode.missingStudyEventOidData";
    public static final String ERR_MISSING_START_DATE_DATA = "errorCode.missingStartDateData";


    public static final String ERR_MULTIPLE_PARTICIPANT_ID_HEADERS = "errorCode.multipleParticipantIdHeaders";
    public static final String ERR_MULTIPLE_STUDY_EVENT_OID_HEADERS = "errorCode.multipleStudyEventOidHeaders";
    public static final String ERR_MULTIPLE_STUDY_EVENT_REPEAT_KEY_HEADERS = "errorCode.multipleStudyEventRepeatKeyHeaders";
    public static final String ERR_MULTIPLE_START_DATE_HEADERS = "errorCode.multipleStartDateHeaders";
    public static final String ERR_MULTIPLE_END_DATE_HEADERS = "errorCode.multipleEndDateHeaders";
    public static final String ERR_MULTIPLE_STUDY_EVENT_STATUS_HEADERS = "errorCode.multipleStudyEventStatusHeaders";

    public static final String ERR_EMAIL_ADDRESS_TOO_LONG = "errorCode.emailAddressTooLong";
    public static final String ERR_INVALID_EMAIL_ADDRESS = "errorCode.invalidEmailAddress";

    public static final String ERR_PHONE_NUMBER_TOO_LONG = "errorCode.phoneNumberTooLong";
    public static final String ERR_INVALID_PHONE_NUMBER = "errorCode.invalidPhoneNumber";

    public static final String ERR_FIRST_NAME_TOO_LONG = "errorCode.firstNameTooLong";
    public static final String ERR_LAST_NAME_TOO_LONG = "errorCode.lastNameTooLong";
    public static final String ERR_IDENTIFIER_TOO_LONG = "errorCode.identifierTooLong";
    public static final String ERR_PARTICIPANT_ID_TOO_LONG = "errorCode.participantIdTooLong";

    public static final String ERR_SYSTEM_GENERATED_ID_ENABLED = "errorCode.studyHasSystemGeneratedIdEnabled";

    public static final String ERR_MULTIPLE_FIRST_NAME_HEADERS = "errorCode.multipleFirstNameHeaders";
    public static final String ERR_MULTIPLE_LAST_NAME_HEADERS = "errorCode.multipleLastNameHeaders";
    public static final String ERR_MULTIPLE_EMAIL_ADDRESS_HEADERS = "errorCode.multipleEmailAddressHeaders";
    public static final String ERR_MULTIPLE_MOBILE_PHONE_HEADERS = "errorCode.multipleMobilePhoneHeaders";
	public static final String ERR_IMPORT_XML_QUERY_CREAT_FAILED = "errorCode.importXMLcreateQueryFailed";

    public static final String ERR_INVALID_PAGE_NUMBER_PARAMETER = "errorCode.invalidPageNumberParameter";
    public static final String ERR_INVALID_PAGE_SIZE_PARAMETER = "errorCode.invalidPageSizeParameter";

    public static final String ERR_STUDYSUBJECT_STUDYEVENT_STUDYFORM_NOT_RELATED = "errorCode.participantIdStudEventOIDAndFormOIDNotMatching";
    public static final String ERR_EVENTOID_NOT_EXIST_IN_THIS_STUDY = "errorCode.eventOIDNotAvailableInThisStudy";
    public static final String ERR_FORMOID_NOT_EXIST_IN_THIS_STUDY = "errorCode.formOIDNotAvailableInThisStudy";
    public static final String ERR_PARTICIPANT_DOES_NOT_HAVE_THIS_EVENT_IN_THIS_STUDY = "errorCode.participantDoesNotHaveThisEventInThisStudy";
    public static final String ERR_EVENT_CRF_NOT_COMPLETED = "errorCode.eventCrfIsNotInCompleted";
    public static final String ERR_EVENT_ORDINAL_IS_INCORRECT = "errorCode.eventOrdinalIsIncorrect";

    public static final String ERR_HAS_NO_ACCESS_TO_FORM = "errorCode.hasNoAccessToForm";
    public static final String ERR_DISCREPANCY_NOTE_STATUS_NOT_VALID = "errorCode.discrepancyNoteStatusNotValid";
    public static final String ERR_MISSING_DISCREPANCY_NOTE_STATUS = "errorCode.missingDiscrepancyNoteStatus";
    public static final String ERR_DISCREPANCY_NOTE_TYPE_NOT_VALID = "errorCode.discrepancyNoteTypeNotValid";
    public static final String ERR_MISSING_DISCREPANCY_NOTE_TYPE = "errorCode.missingDiscrepancyNoteType";
    public static final String ERR_DISCREPANCY_NOTE_ID_TOO_LONG = "errorCode.discrepancyNoteIdTooLong";
    public static final String ERR_DISCREPANCY_NOTE_ID_IS_REDUNDANT = "errorCode.discrepancyNoteIdIsRedundant";
    public static final String ERR_USER_NOT_VALID = "errorCode.userNotValid";
    public static final String ERR_MISSING_USER_NAME = "errorCode.missingUserName";
    public static final String ERR_ASSIGNED_USER_NOT_VALID = "errorCode.assignedUserNotValid";
    public static final String ERR_DETAILED_NOTE_MISSING = "errorCode.detailedNoteMissing";
    public static final String ERR_DETAILED_NOTE_TOO_LONG = "errorCode.detailedNoteTextTooLong";
    public static final String ERR_QUERY_STATUS_NOT_APPLICABLE = "errorCode.queryStatusNotApplicable";
    public static final String ERR_NEW_QUERIES_IN_BETWEEN_OLD_QUERIES = "errorCode.newQueryInBetweenOldQueries";
    public static final String ERR_ANNOTATION_MUST_HAVE_ONE_CHILD_NOTE = "errorCode.annotationMustHaveOneChildNote";
    public static final String ERR_NOTE_ID_ALREADY_IN_USE = "errorCode.noteIdAlreadyInUse";
    public static final String ERR_EXISTING_NOTE_ID_ON_OTHER_ITEM = "errorCode.existingNoteIdOnOtherItem";
    public static final String ERR_GENERATING_DISCREPANCY_NOTE_ID = "errorCode.errorGeneratingDiscrepancyNoteId";
    public static final String ERR_MISSING_CHILD_NOTE = "errorCode.missingChildNote";
    public static final String ERR_FORM_STATUS_SHOULD_BE_COMPLETE_FOR_SDV_VERIFICATION = "errorCode.formStatusShouldBeCompleteForSettingSdvVerifiedStatus";
    public static final String ERR_SDV_STATUS_NOT_VALID = "errorCode.sdvStatusNotValid";
    public static final String ERR_SDV_STATUS_NOT_APPLICABLE = "errorCode.sdvStatusNotApplicable";
    public static final String ERR_FORM_WITH_REMOVED_OR_ARCHIVED_ATTRIBUTE_CANNOT_BE_SDV_VERIFIED = "errorCode.formWithRemovedOrArchivedAttributeCannotBeSdvVerified";
    public static final String ERR_SDV_STATUS_CANNOT_BE_UPDATED_BECAUSE_OF_ITEM_IMPORT_FAILURE = "errorCode.sdvStatusCannotBeUpdatedBecauseOfItemImportFailure";
    public static final String ERR_SIGNED_STATUS_INVALID = "errorCode.signedStatusIsInvalid";
    public static final String ERR_EVENT_IS_NOT_ELIGIBLE_TO_BE_SIGNED = "errorCode.eventIsNotEligibleToBeSigned";
    public static final String ERR_CANNOT_SIGN_EVENT_WITHOUT_ATTESTATION = "errorcode.cannotSignEventWithoutAttestation";
    public static final String ERR_ATTESTATION_IS_MISSING = "errorCode.attestationIsMissing";
    public static final String ERR_ATTESTATION_TEXT_TOO_LONG = "errorCode.attestationTextTooLong";
    public static final String ERR_STATUS_ATTRIBUTE_INVALID = "errorCode.statusAttributeIsInvalid";
    public static final String ERR_ITEMGROUP_REMOVED_ATTRIBUTE_INVALID = "errorCode.itemGroupRemovedAttributeIsInvalid";
    public static final String ERR_FIRST_REPEATING_ITEMGROUP_CANNOT_BE_REMOVED = "errorCode.firstRepeatingGroupCannotBeRemoved";
    public static final String ERR_NON_REPEATING_ITEMGROUP_CANNOT_BE_REMOVED = "errorCode.nonRepeatingGroupCannotBeRemoved";

}