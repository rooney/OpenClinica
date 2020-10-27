package org.akaza.openclinica.controller;


import java.io.*;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import core.org.akaza.openclinica.bean.core.DataEntryStage;
import core.org.akaza.openclinica.bean.login.*;
import core.org.akaza.openclinica.bean.managestudy.StudyEventBean;
import core.org.akaza.openclinica.bean.rule.XmlSchemaValidationHelper;
import core.org.akaza.openclinica.bean.submit.DisplayItemBeanWrapper;
import core.org.akaza.openclinica.bean.submit.EventCRFBean;
import core.org.akaza.openclinica.bean.submit.crfdata.ODMContainer;
import core.org.akaza.openclinica.bean.submit.crfdata.SubjectDataBean;
import core.org.akaza.openclinica.dao.hibernate.StudyDao;
import core.org.akaza.openclinica.dao.hibernate.UserAccountDao;
import core.org.akaza.openclinica.dao.login.UserAccountDAO;
import core.org.akaza.openclinica.domain.datamap.JobDetail;
import core.org.akaza.openclinica.domain.datamap.Study;
import core.org.akaza.openclinica.domain.enumsupport.JobType;
import core.org.akaza.openclinica.domain.user.UserAccount;
import core.org.akaza.openclinica.exception.OpenClinicaException;
import core.org.akaza.openclinica.logic.importdata.PipeDelimitedDataHelper;
import core.org.akaza.openclinica.service.*;
import org.akaza.openclinica.control.submit.ImportCRFInfo;
import org.akaza.openclinica.control.submit.ImportCRFInfoContainer;
import org.akaza.openclinica.control.submit.ImportCRFInfoSummary;
import org.akaza.openclinica.controller.helper.RestfulServiceHelper;
import core.org.akaza.openclinica.dao.core.CoreResources;
import core.org.akaza.openclinica.exception.OpenClinicaSystemException;
import core.org.akaza.openclinica.i18n.util.ResourceBundleProvider;
import core.org.akaza.openclinica.logic.rulerunner.ExecutionMode;
import core.org.akaza.openclinica.logic.rulerunner.ImportDataRuleRunnerContainer;
import core.org.akaza.openclinica.service.rule.RuleSetServiceInterface;

import core.org.akaza.openclinica.web.restful.data.bean.BaseStudyDefinitionBean;
import core.org.akaza.openclinica.web.restful.data.validator.CRFDataImportValidator;
import org.akaza.openclinica.domain.enumsupport.EventCrfWorkflowStatusEnum;
import org.akaza.openclinica.service.*;
import org.akaza.openclinica.view.Page;
import org.akaza.openclinica.web.restful.errors.ErrorConstants;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Tao Li
 */
@RestController
@RequestMapping(value = "/auth/api/clinicaldata")
@Api(value = "DataImport", tags = {"Clinical Data"}, description = "REST API for Data Import")
public class FlatFileImportController {

    protected static final Logger logger = LoggerFactory.getLogger(FlatFileImportController.class);
    private final Locale locale = new Locale("en_US");
    public static final String USER_BEAN_NAME = "userBean";
    //CSV file header
    private static final String SAS_FILE_EXTENSION = "sas7bdat";
    private static final String XLSX_FILE_EXTENSION = "xlsx";
    private static final String CSV_FILE_EXTENSION = "csv";
    private static final String TXT_FILE_EXTENSION = "txt";

    static {
        disableSslVerification();
    }

    @Autowired
    @Qualifier("dataSource")
    private DataSource dataSource;

    @Autowired
    private RuleSetServiceInterface ruleSetService;

    @Autowired
    private DataImportService dataImportService;

    @Autowired
    private CoreResources coreResources;

    @Autowired
    StudyDao studyDao;

    @Autowired
    private StudyBuildService studyBuildService;

    @Autowired
    SasFileConverterServiceImpl sasFileConverterService;

    @Autowired
    ExcelFileConverterServiceImpl excelFileConverterService;

    @Autowired
    CsvFileConverterServiceImpl csvFileConverterService;

    @Autowired
    private UtilService utilService;

    @Autowired
    UserAccountDao userAccountDao;

    @Autowired
    private UserService userService;

    @Autowired
    private ImportService importService;

    @Autowired
    private ValidateService validateService;

    private RestfulServiceHelper serviceHelper;
    protected UserAccountBean userBean;
    private ImportDataResponseSuccessDTO responseSuccessDTO;
    private XmlSchemaValidationHelper schemaValidator = new XmlSchemaValidationHelper();
    private PipeDelimitedDataHelper importDataHelper;

    public FlatFileImportController (StudyDao studyDao, UserAccountDao userAccountDao, ValidateService validateService,
                                     UtilService utilService, ImportService importService, UserService userService,
                                     SasFileConverterServiceImpl sasFileConverterService,
                                     ExcelFileConverterServiceImpl excelFileConverterService,
                                     CsvFileConverterServiceImpl csvFileConverterService, DataSource dataSource) {
        this.studyDao = studyDao;
        this.userAccountDao = userAccountDao;
        this.validateService = validateService;
        this.utilService = utilService;
        this.importService = importService;
        this.userService = userService;
        this.sasFileConverterService = sasFileConverterService;
        this.excelFileConverterService = excelFileConverterService;
        this.csvFileConverterService = csvFileConverterService;
        this.responseSuccessDTO = new ImportDataResponseSuccessDTO();
        this.dataSource = dataSource;
    }

    @ApiOperation(value = "To import study data in XML file (Deprecated, please use /clinicaldata/import endpoint)", notes = "Will read the data in XML file and validate study,event and participant against the  setup first, for more detail please refer to OpenClinica online document  ")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "Bad Request -- Normally means found validation errors, for detail please see the error message")})

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<Object> importDataXMLFile(HttpServletRequest request, MultipartFile file) throws Exception {

        ArrayList<ErrorMessage> errorMsgs = new ArrayList<ErrorMessage>();
        ResponseEntity<Object> response = null;

        String validation_failed_message = "VALIDATION FAILED";
        String validation_passed_message = "SUCCESS";

        String importXml = null;
        responseSuccessDTO = new ImportDataResponseSuccessDTO();

        try {
            //only support XML file
            if (file != null) {
                String fileNm = file.getOriginalFilename();

                if (fileNm != null && fileNm.endsWith(".xml")) {
                    importXml = RestfulServiceHelper.readFileToString(file);
                } else {
                    throw new OpenClinicaSystemException("errorCode.notXMLfile", "The file format is not supported, please send correct XML file, like *.xml ");

                }

            } else {

                /**
                 *  if call is from the mirth server, then may have no attached file in the request
                 *
                 */

                // Read from request content
                StringBuilder buffer = new StringBuilder();
                BufferedReader reader = request.getReader();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                importXml = buffer.toString();


            }

            errorMsgs = importDataInTransaction(importXml, request);
        } catch (OpenClinicaSystemException e) {
            logger.error("Error importing the XML: ", e);
            String err_msg = e.getMessage();
            ErrorMessage error = createErrorMessage(e.getErrorCode(), err_msg);
            errorMsgs.add(error);

        } catch (Exception e) {
            logger.error("Error processing import request: ", e);
            String err_msg = "Error processing data import request.";
            ErrorMessage error = createErrorMessage("errorCode.Exception", err_msg);
            errorMsgs.add(error);

        }

        if (errorMsgs != null && errorMsgs.size() != 0) {
            ImportDataResponseFailureDTO responseDTO = new ImportDataResponseFailureDTO();
            responseDTO.setMessage(validation_failed_message);
            responseDTO.setErrors(errorMsgs);
            response = new ResponseEntity(responseDTO, org.springframework.http.HttpStatus.BAD_REQUEST);
        } else {
            responseSuccessDTO.setMessage(validation_passed_message);
            response = new ResponseEntity(responseSuccessDTO, org.springframework.http.HttpStatus.OK);
        }

        return response;
    }

    /**
     * @param importXml
     * @param request
     * @return
     * @throws Exception
     */
    protected synchronized ArrayList<ErrorMessage> importDataInTransaction(String importXml, HttpServletRequest request) throws Exception {
        ResourceBundleProvider.updateLocale(new Locale("en_US"));
        ResourceBundle respage = ResourceBundleProvider.getPageMessagesBundle();
        ResourceBundle resWords = ResourceBundleProvider.getWordsBundle();
        ArrayList<ErrorMessage> errorMsgs = new ArrayList<ErrorMessage>();

        Enumeration<String> headerNames = request.getHeaderNames();
        boolean isLogUpdated = false;

        String logFileName = request.getHeader("logFileName");
        request.setAttribute("logFileName", logFileName);

        try {
            // check more xml format--  can't be blank
            if (importXml == null) {
                String err_msg = "Your XML content is blank.";
                ErrorMessage errorOBject = createErrorMessage("errorCode.BlankFile", err_msg);
                errorMsgs.add(errorOBject);

                return errorMsgs;
            }

            if (importXml.trim().equals("errorCode.noParticipantIDinDataFile")) {
                String err_msg = "Participant ID data not found in the data file.";
                ErrorMessage errorOBject = createErrorMessage("errorCode.noParticipantIDinDataFile", err_msg);
                errorMsgs.add(errorOBject);

                return errorMsgs;
            }

            // check more xml format--  must put  the xml content in <ODM> tag
            int beginIndex = importXml.indexOf("<ODM>");
            if (beginIndex < 0) {
                beginIndex = importXml.indexOf("<ODM ");
            }
            int endIndex = importXml.indexOf("</ODM>");
            if (beginIndex < 0 || endIndex < 0) {
                String err_msg = "Please send valid content with correct root tag ODM";
                ErrorMessage errorOBject = createErrorMessage("errorCode.XmlRootTagisNotODM", err_msg);
                errorMsgs.add(errorOBject);
                return errorMsgs;
            }

            importXml = importXml.substring(beginIndex, endIndex + 6);

            userBean = this.getRestfulServiceHelper().getUserAccount(request);

            if (userBean == null) {
                String err_msg = "Please send request as a valid user";
                ErrorMessage errorOBject = createErrorMessage("errorCode.InvalidUser", err_msg);
                errorMsgs.add(errorOBject);
                return errorMsgs;
            }

            File xsdFile = this.getRestfulServiceHelper().getXSDFile(request, "ODM1-3-0.xsd");
            File xsdFile2 = this.getRestfulServiceHelper().getXSDFile(request, "ODM1-2-1.xsd");

            Mapping myMap = new Mapping();
            String ODM_MAPPING_DIRPath = CoreResources.ODM_MAPPING_DIR;
            myMap.loadMapping(ODM_MAPPING_DIRPath + File.separator + "cd_odm_mapping.xml");

            Unmarshaller um1 = new Unmarshaller(myMap);
            boolean fail = false;
            ODMContainer odmContainer = new ODMContainer();

            try {
                // unmarshal xml to java
                InputStream is = new ByteArrayInputStream(importXml.getBytes());
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                odmContainer = (ODMContainer) um1.unmarshal(isr);

            } catch (Exception me1) {
                // expanding it to all exceptions, but hoping to catch Marshal
                // Exception or SAX Exceptions
                logger.error("found exception with xml transform: ", me1);
                logger.info("trying 1.2.1");
                try {
                    schemaValidator.validateAgainstSchema(importXml, xsdFile2);
                    // for backwards compatibility, we also try to validate vs
                    // 1.2.1 ODM 06/2008
                    InputStream is = new ByteArrayInputStream(importXml.getBytes());
                    InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                    odmContainer = (ODMContainer) um1.unmarshal(isr);
                } catch (Exception me2) {
                    // not sure if we want to report me2
                    MessageFormat mf = new MessageFormat("");

                    Object[] arguments = {me1.getMessage()};
                    String errCode = mf.format(arguments);

                    String err_msg = "Your XML file is not well-formed.";
                    ErrorMessage errorOBject = createErrorMessage("errorCode.XmlNotWellFormed", err_msg);
                    errorMsgs.add(errorOBject);
                }
            }

            String studyUniqueID = odmContainer.getCrfDataPostImportContainer().getStudyOID();
            BaseStudyDefinitionBean crfDataImportBean = new BaseStudyDefinitionBean(studyUniqueID, userBean);

            DataBinder dataBinder = new DataBinder(crfDataImportBean);
            Errors errors = dataBinder.getBindingResult();

            // set DB schema
            try {
                getRestfulServiceHelper().setSchema(studyUniqueID, request);
            } catch (OpenClinicaSystemException e) {
                errors.reject(e.getErrorCode(), e.getMessage());

                // log error into file
                isLogUpdated = true;
                SubjectDataBean subjectDataBean = odmContainer.getCrfDataPostImportContainer().getSubjectData().get(0);
                String participantId = subjectDataBean == null ? null : subjectDataBean.getStudySubjectID();

                String originalFileName = request.getHeader("originalFileName");
                String recordNum = null;
                if (originalFileName != null) {
                    recordNum = originalFileName.substring(originalFileName.lastIndexOf("_") + 1, originalFileName.indexOf("."));
                    originalFileName = originalFileName.substring(0, originalFileName.lastIndexOf("_"));
                }
                String msg = recordNum + "," + participantId + ",FAILED," + e.getMessage();
                this.dataImportService.getImportCRFDataService().getPipeDelimitedDataHelper().writeToMatchAndSkipLog(originalFileName, msg, request);
            }

            CRFDataImportValidator crfDataImportValidator = new CRFDataImportValidator(dataSource, studyDao, studyBuildService);

            // if no error then continue to validate
            if (!errors.hasErrors()) {
                crfDataImportValidator.validate(crfDataImportBean, errors, request);
            }


            // if no error then continue to validate
            if (!errors.hasErrors()) {
                Study studyBean = crfDataImportBean.getStudy();

                List<DisplayItemBeanWrapper> displayItemBeanWrappers = new ArrayList<DisplayItemBeanWrapper>();
                HashMap<Integer, String> importedCRFStatuses = new HashMap<Integer, String>();

                /**
                 *  for pipe delimited import, already validate meta data immediately after upload the mapping file
                 *  so will skip at this stage
                 */
                List<String> errorMessagesFromValidation = null;
                String comeFromPipe = (String) request.getHeader("PIPETEXT");
                if (comeFromPipe != null && comeFromPipe.equals("PIPETEXT")) {
                } else {
                    errorMessagesFromValidation = dataImportService.validateMetaData(odmContainer, dataSource, coreResources, studyBean, userBean,
                            displayItemBeanWrappers, importedCRFStatuses);

                }

                if (errorMessagesFromValidation != null && errorMessagesFromValidation.size() > 0) {
                    String err_msg = convertToErrorString(errorMessagesFromValidation);

                    ErrorMessage errorOBject = createErrorMessage("errorCode.ValidationFailed", err_msg);
                    errorMsgs.add(errorOBject);

                    /**
                     * log error into log file 
                     */
                    isLogUpdated = true;
                    String participantId = odmContainer.getCrfDataPostImportContainer().getSubjectData().get(0).getStudySubjectID();
                    String originalFileName = request.getHeader("originalFileName");
                    // sample file name like:originalFileName_123.txt,pipe_delimited_local_skip_2.txt
                    String recordNum = null;
                    if (originalFileName != null) {
                        recordNum = originalFileName.substring(originalFileName.lastIndexOf("_") + 1, originalFileName.indexOf("."));
                        originalFileName = originalFileName.substring(0, originalFileName.lastIndexOf("_"));
                    }
                    String msg = recordNum + "," + participantId + ",FAILED," + err_msg;
                    this.dataImportService.getImportCRFDataService().getPipeDelimitedDataHelper().writeToMatchAndSkipLog(originalFileName, msg, request);

                    return errorMsgs;
                }

                HashMap validateDataResult = (HashMap) dataImportService.validateData(odmContainer, dataSource, coreResources, studyBean, userBean,
                        displayItemBeanWrappers, importedCRFStatuses, request);
                ArrayList<StudyEventBean> newStudyEventBeans = (ArrayList<StudyEventBean>) validateDataResult.get("studyEventBeans");
                List<EventCRFBean> eventCRFBeans = (List<EventCRFBean>) validateDataResult.get("eventCRFBeans");

                errorMessagesFromValidation = (List<String>) validateDataResult.get("errors");

                if (errorMessagesFromValidation.size() > 0) {
                    String erroCode = "";
                    String err_msg = convertToErrorString(errorMessagesFromValidation);
                    if (err_msg != null && err_msg.startsWith("errorCode.")) {
                        erroCode = err_msg.substring(0, err_msg.indexOf(":"));
                    } else {
                        erroCode = "errorCode.ValidationFailed";
                    }
                    ErrorMessage errorOBject = createErrorMessage(erroCode, err_msg);
                    errorMsgs.add(errorOBject);


                    /**
                     * log error into log file
                     */
                    isLogUpdated = true;
                    String participantId = odmContainer.getCrfDataPostImportContainer().getSubjectData().get(0).getStudySubjectID();
                    String originalFileName = request.getHeader("originalFileName");
                    // sample file name like:originalFileName_123.txt,pipe_delimited_local_skip_2.txt
                    String recordNum = null;
                    if (originalFileName != null) {
                        recordNum = originalFileName.substring(originalFileName.lastIndexOf("_") + 1, originalFileName.indexOf("."));
                        originalFileName = originalFileName.substring(0, originalFileName.lastIndexOf("_"));
                    }
                    // for skip err_msg:1,participantLabel,SUCCESS,Skip
                    String msg = null;
                    String skipMessage = "SUCCESS," + resWords.getString("skip");
                    if (err_msg.indexOf(skipMessage) > -1) {
                        msg = err_msg;
                    } else {
                        msg = recordNum + "," + participantId + ",FAILED," + err_msg;
                    }

                    this.dataImportService.getImportCRFDataService().getPipeDelimitedDataHelper().writeToMatchAndSkipLog(originalFileName, msg, request);
                    return errorMsgs;
                }

                // setup ruleSets to run if applicable
                ArrayList<SubjectDataBean> subjectDataBeans = odmContainer.getCrfDataPostImportContainer().getSubjectData();
                List<ImportDataRuleRunnerContainer> containers = dataImportService.runRulesSetup(dataSource, studyBean, userBean, subjectDataBeans,
                        ruleSetService);

                // Now can create event and CRF beans here
                if (newStudyEventBeans != null && newStudyEventBeans.size() > 0) {
                    ArrayList<StudyEventBean> studyEventBeanCreatedList = this.dataImportService.getImportCRFDataService().creatStudyEvent(newStudyEventBeans);

                    ArrayList<EventCRFBean> tempEventCRFBeans = new ArrayList<>();
                    for (StudyEventBean studyEventBean : studyEventBeanCreatedList) {
                        tempEventCRFBeans.addAll(studyEventBean.getEventCRFs());
                    }

                    ArrayList<Integer> permittedEventCRFIds = new ArrayList<Integer>();

                    for (EventCRFBean eventCRFBean : tempEventCRFBeans) {
                        DataEntryStage dataEntryStage = eventCRFBean.getStage();

                        if (!eventCRFBean.getWorkflowStatus().equals(EventCrfWorkflowStatusEnum.COMPLETED)) {
                            permittedEventCRFIds.add(new Integer(eventCRFBean.getId()));
                        }
                    }
                    // now need to update displayItemBeanWrappers
                    // The following line updates a map that is used for setting the EventCRF status post import
                    this.dataImportService.getImportCRFDataService().fetchEventCRFStatuses(odmContainer, importedCRFStatuses);
                    displayItemBeanWrappers = this.dataImportService.getImportCRFDataService().getDisplayItemBeanWrappers(request, odmContainer, userBean, permittedEventCRFIds, locale);
                }

                List<String> auditMsgs = dataImportService.submitData(odmContainer, dataSource, studyBean, userBean, displayItemBeanWrappers,
                        importedCRFStatuses);

                // run rules if applicable
                List<String> ruleActionMsgs = dataImportService.runRules(studyBean, userBean, containers, ruleSetService, ExecutionMode.SAVE);

                /**
                 *  Now it's time to log successful message into log file                      
                 */
                String participantId = odmContainer.getCrfDataPostImportContainer().getSubjectData().get(0).getStudySubjectID();
                String originalFileName = request.getHeader("originalFileName");
                // sample file name like:originalFileName_123.txt,pipe_delimited_local_skip_2.txt
                String recordNum = null;
                if (originalFileName != null) {
                    recordNum = originalFileName.substring(originalFileName.lastIndexOf("_") + 1, originalFileName.indexOf("."));
                    originalFileName = originalFileName.substring(0, originalFileName.lastIndexOf("_"));
                }
                isLogUpdated = true;
                String msg = recordNum + "," + participantId + ",SUCCESS," + resWords.getString("imported");
                this.dataImportService.getImportCRFDataService().getPipeDelimitedDataHelper().writeToMatchAndSkipLog(originalFileName, msg, request);

                ImportCRFInfoContainer importCrfInfo = new ImportCRFInfoContainer(odmContainer, dataSource, studyDao);
                List<String> skippedCRFMsgs = getSkippedCRFMessages(importCrfInfo);

                // add detail messages to reponseDTO
                ArrayList<String> detailMessages = new ArrayList();
                detailMessages.add("Audit Messages:" + convertToErrorString(auditMsgs));
                detailMessages.add("Rule Action Messages:" + convertToErrorString(ruleActionMsgs));
                detailMessages.add("Skip CRF Messages:" + convertToErrorString(skippedCRFMsgs));
                this.responseSuccessDTO.setDetailMessages(detailMessages);

            } else {

                for (ObjectError error : errors.getAllErrors()) {
                    String err_msg = error.getDefaultMessage();
                    String errCode = error.getCode();
                    ErrorMessage errorMessage = createErrorMessage(errCode, err_msg);
                    errorMsgs.add(errorMessage);
                }
            }

            return errorMsgs;

        } catch (Exception e) {
            logger.error("Error processing data import request ", e);
            if (!isLogUpdated) {
                String participantId = request.getHeader("participantLabel");
                String originalFileName = request.getHeader("originalFileName");
                // sample file name like:originalFileName_123.txt,pipe_delimited_local_skip_2.txt
                String recordNum = null;
                if (originalFileName != null) {
                    recordNum = originalFileName.substring(originalFileName.lastIndexOf("_") + 1, originalFileName.indexOf("."));
                    originalFileName = originalFileName.substring(0, originalFileName.lastIndexOf("_"));
                }
                String msg = recordNum + "," + participantId + ",FAILED," + respage.getString("unexpected_error_occured");

                this.dataImportService.getImportCRFDataService().getPipeDelimitedDataHelper().writeToMatchAndSkipLog(originalFileName, msg, request);
            }
            throw new Exception(e);
        }
    }

    public ErrorMessage createErrorMessage(String code, String message) {
        ErrorMessage errorMsg = new ErrorMessage();
        errorMsg.setCode(code);
        errorMsg.setMessage(message);

        return errorMsg;
    }

    private String convertToErrorString(List<String> errorMessages) {
        StringBuilder result = new StringBuilder();
        for (String str : errorMessages) {
            result.append(str);
        }

        return result.toString();
    }

    private List<String> getSkippedCRFMessages(ImportCRFInfoContainer importCrfInfo) {
        List<String> msgList = new ArrayList<String>();

        ResourceBundle respage = ResourceBundleProvider.getPageMessagesBundle();
        ResourceBundle resword = ResourceBundleProvider.getWordsBundle();
        MessageFormat mf = new MessageFormat("");
        try {
            mf.applyPattern(respage.getString("crf_skipped"));
        } catch (Exception e) {
            // no need break, just continue
            String formatStr = "StudyOID {0}, StudySubjectOID {1}, StudyEventOID {2}, FormOID {3}, preImportStatus {4}";
            mf.applyPattern(formatStr);
        }


        for (ImportCRFInfo importCrf : importCrfInfo.getImportCRFList()) {
            if (!importCrf.isProcessImport()) {
                String preImportStatus = "";

                if (importCrf.getPreImportStage().isInitialDE())
                    preImportStatus = resword.getString("initial_data_entry");
                else if (importCrf.getPreImportStage().isInitialDE_Complete())
                    preImportStatus = resword.getString("initial_data_entry_complete");
                else if (importCrf.getPreImportStage().isDoubleDE())
                    preImportStatus = resword.getString("double_data_entry");
                else if (importCrf.getPreImportStage().isDoubleDE_Complete())
                    preImportStatus = resword.getString("data_entry_complete");
                else if (importCrf.getPreImportStage().isAdmin_Editing())
                    preImportStatus = resword.getString("administrative_editing");
                else if (importCrf.getPreImportStage().isLocked())
                    preImportStatus = resword.getString("locked");
                else
                    preImportStatus = resword.getString("invalid");

                Object[] arguments = {importCrf.getStudyOID(), importCrf.getStudySubjectOID(), importCrf.getStudyEventOID(), importCrf.getFormOID(),
                        preImportStatus};
                msgList.add(mf.format(arguments));
            }
        }
        return msgList;
    }

    public RuleSetServiceInterface getRuleSetService() {
        return ruleSetService;
    }

    public void setRuleSetService(RuleSetServiceInterface ruleSetService) {
        this.ruleSetService = ruleSetService;
    }

    public RestfulServiceHelper getRestfulServiceHelper() {
        if (serviceHelper == null) {
            serviceHelper = new RestfulServiceHelper(this.dataSource, studyBuildService, studyDao, sasFileConverterService,
                    excelFileConverterService, csvFileConverterService);
        }

        return serviceHelper;
    }


    private static void disableSslVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Disabling SSL Verification failed: ", e);
        } catch (KeyManagementException e) {
            logger.error("Disabling SSL Verification failed: ", e);
        }
    }


    @ApiOperation(value = "To import study data in Pipe Delimited Text File (Supports Common events with non-repeating item groups only)", notes = "Will read both the data text files and  one mapping text file, then validate study,event and participant against the  setup first, for more detail please refer to OpenClinica online document  ")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 400, message = "Bad Request -- Normally means found validation errors, for detail please see the error message")})

    @RequestMapping(value = "/pipe", method = RequestMethod.POST)
    public ResponseEntity<Object> importDataPipeDelimitedFile(HttpServletRequest request, MultipartFile dataFile, MultipartFile mappingFile) throws Exception {

        HashMap hm = new HashMap();

        MultipartFile[] mFiles = new MultipartFile[2];
        mFiles[0] = mappingFile;
        mFiles[1] = dataFile;

        try {
            String studyOID = this.getRestfulServiceHelper().getImportDataHelper().getStudyOidFromMappingFile(mappingFile);
            getRestfulServiceHelper().setSchema(studyOID, request);
            //only support text file
            if (mFiles[0] != null) {
                boolean foundMappingFile = false;

                File[] files = this.dataImportService.getImportCRFDataService().getPipeDelimitedDataHelper().convert(mFiles, studyOID);

                if (files.length < 2) {
                    throw new OpenClinicaSystemException("errorCode.notCorrectFileNumber", "When send files, Please send at least one data text files and  one mapping text file in correct format ");
                }

                for (File file : files) {

                    if (file == null || file.getName() == null) {
                        logger.info("file is empty.");

                    } else {
                        if (file.getName().toLowerCase().endsWith(".properties")) {
                            foundMappingFile = true;
                            logger.info("Found mapping property file and uploaded");
                            hm = this.dataImportService.getImportCRFDataService().getPipeDelimitedDataHelper().validateMappingFile(file);

                        } else {
                            //logFileName = this.getRestfulServiceHelper().buildLogFile(file.getName(), request);
                            //request.setAttribute("logFileName", logFileName);
                        }
                    }
                }

                if (!foundMappingFile) {
                    throw new OpenClinicaSystemException("errorCode.noMappingfile", "When send files, please include one correct mapping file, named like *mapping.txt ");
                }

                UserAccountBean userAccountBean = utilService.getUserAccountFromRequest(request);
                processDataAndStartImportJob(request, Arrays.asList(files), hm, studyOID, userAccountBean);

            } else {
                throw new OpenClinicaSystemException("errorCode.notCorrectFileNumber", "Please send at least one data text files and  one mapping text file in correct format ");
            }
        } catch (Exception e) {
            logger.error("Error processing data import request: ", e);
            return new ResponseEntity(e.getMessage(), org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity(responseSuccessDTO, org.springframework.http.HttpStatus.OK);
    }

    /**
     * Convert files to one odm xml file, create odm container, and start import.
     * @param request request
     * @param files the mapping file and the txt file
     * @param hm
     * @return response
     * @throws Exception
     */
    public ResponseEntity<Object> processDataAndStartImportJob(HttpServletRequest request, List<File> files, HashMap hm, String studyOID, UserAccountBean userAccountBean) throws Exception {
        String validation_passed_message = "SUCCESS";
        File odmxml = makeOdmXmlFile(files, hm, studyOID);
        String fileNm = "";
        String importXml;

        if (odmxml != null) {
            fileNm = odmxml.getName();
            if (fileNm != null && fileNm.endsWith(".xml")) {
                importXml = FileUtils.readFileToString(odmxml, "UTF-8");
            } else {
                logger.error("file is not an xml extension file");
                return new ResponseEntity(ErrorConstants.ERR_FILE_FORMAT_NOT_SUPPORTED, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
        } else {
            //  if call is from the mirth server, then may have no attached file in the request
            // Read from request content
            StringBuilder buffer = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            importXml = buffer.toString();
        }


        Mapping myMap = new Mapping();
        String ODM_MAPPING_DIRPath = CoreResources.ODM_MAPPING_DIR;
        myMap.loadMapping(ODM_MAPPING_DIRPath + File.separator + "cd_odm_mapping.xml");

        Unmarshaller um1 = new Unmarshaller(myMap);
        boolean fail = false;
        ODMContainer odmContainer = new ODMContainer();
        InputStream inputStream = null;
        try {
            // unmarshal xml to java
            inputStream = new ByteArrayInputStream(importXml.getBytes());
            String defaultEncoding = "UTF-8";

            BOMInputStream bOMInputStream = new BOMInputStream(inputStream);
            ByteOrderMark bom = bOMInputStream.getBOM();
            String charsetName = bom == null ? defaultEncoding : bom.getCharsetName();
            InputStreamReader reader = new InputStreamReader(new BufferedInputStream(bOMInputStream), charsetName);

            odmContainer = (ODMContainer) um1.unmarshal(reader);

        } catch (Exception e) {
            logger.error("found exception with xml transform {}", e);
            return new ResponseEntity(ErrorConstants.ERR_INVALID_XML_FILE + "\n" + e.getMessage(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        } finally {
            inputStream.close();
        }

        studyOID = studyOID.toUpperCase();
        Study publicStudy = studyDao.findPublicStudy(studyOID);
        if (publicStudy == null) {
            return new ResponseEntity(ErrorConstants.ERR_STUDY_NOT_EXIST, HttpStatus.NOT_FOUND);
        }
        String siteOid = null;
        String studyOid = null;

        if (publicStudy.getStudy() == null) {
            // This is a studyOid
            studyOid = studyOID;
        } else {
            //This is a siteOid
            siteOid = studyOID;
            studyOid = publicStudy.getStudy().getOc_oid();

        }
        if (studyOid != null)
            studyOid = studyOid.toUpperCase();
        if (siteOid != null)
            siteOid = siteOid.toUpperCase();

        utilService.setSchemaFromStudyOid(studyOid);

        ArrayList<StudyUserRoleBean> userRoles = userAccountBean.getRoles();

        if (!validateService.isStudyAvailable(studyOid)) {
            return new ResponseEntity(ErrorConstants.ERR_STUDY_NOT_AVAILABLE, HttpStatus.OK);
        }

        if (siteOid != null && !validateService.isSiteAvailable(siteOid)) {
            return new ResponseEntity(ErrorConstants.ERR_SITE_NOT_AVAILABLE, HttpStatus.OK);
        }

        if (!validateService.isStudyOidValid(studyOid)) {
            return new ResponseEntity(ErrorConstants.ERR_STUDY_NOT_EXIST, HttpStatus.OK);
        }

        // If the import is performed by a system user, then we can skip the roles check.
        boolean isSystemUserImport = validateService.isUserSystemUser(request);
        if (!isSystemUserImport) {
            if (siteOid != null) {
                if (!validateService.isUserHasAccessToSite(userRoles, siteOid)) {
                    return new ResponseEntity(ErrorConstants.ERR_NO_ROLE_SETUP, HttpStatus.OK);
                } else if (!validateService.isUserHas_CRC_INV_DM_DEP_DS_RoleInSite(userRoles, siteOid)) {
                    return new ResponseEntity(ErrorConstants.ERR_NO_SUFFICIENT_PRIVILEGES, HttpStatus.OK);
                }
            } else {
                if (!validateService.isUserHasAccessToStudy(userRoles, studyOid)) {
                    return new ResponseEntity(ErrorConstants.ERR_NO_ROLE_SETUP, HttpStatus.OK);
                } else if (!validateService.isUserHas_DM_DEP_DS_RoleInStudy(userRoles, studyOid)) {
                    return new ResponseEntity(ErrorConstants.ERR_NO_SUFFICIENT_PRIVILEGES, HttpStatus.OK);
                }
            }
        }

        String schema = CoreResources.getRequestSchema();

        String uuid;
        try {
            uuid = startImportJob(odmContainer, schema, studyOid, siteOid, userAccountBean, fileNm, isSystemUserImport);
            String msg = validation_passed_message;
            ArrayList<String> detailMessages = new ArrayList();
            detailMessages.add("Please see bulk action logs. Job uuid: " + uuid);
            responseSuccessDTO.setDetailMessages(detailMessages);
            responseSuccessDTO.setMessage(msg);
            return new ResponseEntity(responseSuccessDTO, org.springframework.http.HttpStatus.OK);
        } catch (CustomParameterizedException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public String startImportJob(ODMContainer odmContainer, String schema, String studyOid, String siteOid,
                                 UserAccountBean userAccountBean, String fileNm, boolean isSystemUserImport) {
        utilService.setSchemaFromStudyOid(studyOid);

        Study site = studyDao.findByOcOID(siteOid);
        Study study = studyDao.findByOcOID(studyOid);
        UserAccount userAccount = userAccountDao.findById(userAccountBean.getId());
        if (isSystemUserImport) {
            // For system level imports, instead of running import as an asynchronous job, run it synchronously
            logger.debug("Running import synchronously");
            boolean isImportSuccessful = importService.validateAndProcessFlatFileDataImport(odmContainer, studyOid, siteOid, userAccountBean, schema, null, isSystemUserImport);

            if (!isImportSuccessful) {
                // Throw an error if import fails such that randomize service can update the status accordingly
                throw new CustomParameterizedException(ErrorConstants.ERR_IMPORT_FAILED);
            }
            return null;
        } else {
            JobDetail jobDetail = userService.persistJobCreated(study, site, userAccount, JobType.FLAT_FILE_IMPORT, fileNm);
            CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                try {
                    importService.validateAndProcessFlatFileDataImport(odmContainer, studyOid, siteOid, userAccountBean, schema, jobDetail, isSystemUserImport);
                } catch (Exception e) {
                    logger.error("Exception is thrown while processing dataImport: " + e);
                    userService.persistJobFailed(jobDetail, fileNm);
                }
                return null;

            });
            return jobDetail.getUuid();
        }
    }

    public File makeOdmXmlFile(List<File> files, HashMap hm, String studyOID) throws Exception {
        /**
         *  prepare mapping file
         */
        File mappingFile = null;
        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(".properties")) {
                mappingFile = file;
                Study publicStudy = null;
                if (!StringUtils.isEmpty(studyOID))
                    publicStudy = studyDao.findPublicStudy(studyOID);
                if (publicStudy != null)
                    CoreResources.setRequestSchema(publicStudy.getSchemaName());
                break;
            }
        }

        for (File file : files) {
            // skip mapping file
            if (file.getName().toLowerCase().endsWith(".properties")) {
            } else {
                File dataFile = processData(mappingFile, file, studyOID);
                String originalFileName = dataFile.getName();
                String dataStr = this.getImportDataHelper().transformTextToODMxml(mappingFile, dataFile, hm);
                return this.getImportDataHelper().saveDataToFile(dataStr, originalFileName, studyOID);
            }
        }
        return null;
    }

    public File processData(File mappingFile, File dataFile, String studyOID) throws IOException, OpenClinicaException {
        String importFileDir = this.getImportDataHelper().getImportFileDir(studyOID);
        String fileType = com.google.common.io.Files.getFileExtension(dataFile.getAbsolutePath());
        if (fileType.equals(SAS_FILE_EXTENSION)) {
            // convert sas to pipe-delimited
            dataFile = sasFileConverterService.convert(dataFile);
        } else if (fileType.equals(XLSX_FILE_EXTENSION)) {
            // convert xlsx to pipe-delimited
            dataFile = excelFileConverterService.convert(dataFile);
        } else if (fileType.equals(CSV_FILE_EXTENSION)) {
            // convert csv to pipe-delimited
            dataFile = csvFileConverterService.convert(dataFile);
        } else if (fileType.equals(TXT_FILE_EXTENSION)) {
            Properties mappingProperties = readMappingProperties(mappingFile);
            String delimiter = mappingProperties.getProperty(PipeDelimitedDataHelper.DELIMITER_PROPERTY);
            if (delimiter != null) {
                if (delimiter.length() != 1) {
                    throw new OpenClinicaException("Invalid delimiter character", ErrorConstants.INVALID_DELIMITER);
                }
                dataFile = csvFileConverterService.convert(dataFile, delimiter.charAt(0));
            }
        }

        BufferedReader reader = new BufferedReader(new FileReader(dataFile));

        //get original file name
        String orginalFileName = dataFile.getName();
        int pos = orginalFileName.indexOf(".");
        if (pos > 0) {
            orginalFileName = orginalFileName.substring(0, pos);
        }

        //first line
        String columnLine = reader.readLine();
        String line = columnLine;

        File oneFile = new File(importFileDir + orginalFileName + ".txt");
        FileOutputStream fos = new FileOutputStream(oneFile);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        bw.write(columnLine);

        try {
            while (line != null) {
                // read next line
                line = reader.readLine();
                if (line != null) {
                    bw.write("\r");
                    bw.write(line);
                }

            }
            if (bw != null) {
                bw.close();
            }
            reader.close();

        } catch (Exception e) {
            logger.error("Error while accessing the process the data: ", e);
        }

        return oneFile;
    }


    public PipeDelimitedDataHelper getImportDataHelper() {
        if (importDataHelper == null) {
            importDataHelper = new PipeDelimitedDataHelper(this.dataSource, studyBuildService, studyDao);
        }
        return importDataHelper;
    }


    private Properties readMappingProperties(File mappingFile) throws IOException {
        Properties mappingProperties = new Properties();
        mappingProperties.load(new FileReader(mappingFile));
        return mappingProperties;
    }

}
