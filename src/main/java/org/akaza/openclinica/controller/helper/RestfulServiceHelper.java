package org.akaza.openclinica.controller.helper;

import core.org.akaza.openclinica.bean.core.Role;
import core.org.akaza.openclinica.bean.login.StudyUserRoleBean;
import core.org.akaza.openclinica.bean.login.UserAccountBean;
import core.org.akaza.openclinica.dao.core.CoreResources;
import core.org.akaza.openclinica.dao.hibernate.StudyDao;
import core.org.akaza.openclinica.dao.login.UserAccountDAO;
import core.org.akaza.openclinica.domain.datamap.Study;
import core.org.akaza.openclinica.exception.OpenClinicaException;
import core.org.akaza.openclinica.exception.OpenClinicaSystemException;
import core.org.akaza.openclinica.logic.importdata.FlatFileImportDataHelper;
import core.org.akaza.openclinica.service.StudyBuildService;
import org.akaza.openclinica.control.SpringServletAccess;
import org.akaza.openclinica.service.CsvFileConverterServiceImpl;
import org.akaza.openclinica.service.ExcelFileConverterServiceImpl;
import org.akaza.openclinica.service.SasFileConverterServiceImpl;
import org.akaza.openclinica.web.restful.errors.ErrorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Scanner;

@Configurable
@Service("serviceHelper")
public class RestfulServiceHelper {

    private final static Logger log = LoggerFactory.getLogger("RestfulServiceHelper");

    //CSV file header
    private static final String SAS_FILE_EXTENSION = "sas7bdat";
    private static final String XLSX_FILE_EXTENSION = "xlsx";
    private static final String CSV_FILE_EXTENSION = "csv";
    private static final String TXT_FILE_EXTENSION = "txt";

    private DataSource dataSource;
    private StudyDao studyDao;
    private StudyBuildService studyBuildService;
    private UserAccountDAO userAccountDAO;
    private FlatFileImportDataHelper importDataHelper;
    private MessageLogger messageLogger;
    private SasFileConverterServiceImpl sasFileConverterService;
    private ExcelFileConverterServiceImpl excelFileConverterService;
    private CsvFileConverterServiceImpl csvFileConverterService;

    public RestfulServiceHelper() {

    }

    public RestfulServiceHelper(DataSource dataSource, StudyBuildService studyBuildService, StudyDao studyDao) {
        this.dataSource = dataSource;
        this.studyBuildService = studyBuildService;
        this.studyDao = studyDao;
    }

    public RestfulServiceHelper(DataSource dataSource,
                                StudyBuildService studyBuildService,
                                StudyDao studyDao,
                                SasFileConverterServiceImpl sasFileConverterService,
                                ExcelFileConverterServiceImpl excelFileConverterService,
                                CsvFileConverterServiceImpl csvFileConverterService) {
        this.dataSource = dataSource;
        this.studyBuildService = studyBuildService;
        this.studyDao = studyDao;
        this.sasFileConverterService = sasFileConverterService;
        this.excelFileConverterService = excelFileConverterService;
        this.csvFileConverterService = csvFileConverterService;
    }

    /**
     * @param studyOid
     * @param request
     * @return
     * @throws Exception
     */
    public Study setSchema(String studyOid, HttpServletRequest request) throws OpenClinicaSystemException {
        // first time, the default DB schema for restful service is public
        Study study = studyDao.findPublicStudy(studyOid);

        Connection con;
        String schemaNm = "";

        if (study == null) {
            throw new OpenClinicaSystemException("errorCode.studyNotExist", "The study identifier you provided:" + studyOid + " is not valid.");

        } else {
            schemaNm = study.getSchemaName();
        }
        request.setAttribute("requestSchema", schemaNm);
        // get correct study from the right DB schema
        study = studyDao.findByOcOID(studyOid);

        return study;
    }

    public boolean verifyRole(String userName, String study_oid,
                              String site_oid, Errors e) {

        boolean hasRolePermission = true;
        // check for site role & user permission if ok -> return yes,
        //if no-> check for study permissions & role
        String studyOid = study_oid;
        String siteOid = site_oid;

        StudyUserRoleBean studyLevelRole = this.getUserAccountDAO().findTheRoleByUserNameAndStudyOid(userName, studyOid);
        if (studyLevelRole == null) {
            if (siteOid != null) {

                StudyUserRoleBean siteLevelRole = this.getUserAccountDAO().findTheRoleByUserNameAndStudyOid(userName, siteOid);
                if (siteLevelRole == null) {
                    e.reject(ErrorConstants.ERR_NO_ROLE_SETUP, "No role configured for user" + userName + " in Site " + siteOid + ".");
                    hasRolePermission = false;
                } else if (siteLevelRole.getId() == 0 || siteLevelRole.getRole().equals(Role.MONITOR)) {
                    e.reject(ErrorConstants.ERR_NO_SUFFICIENT_PRIVILEGES, "User account does not have sufficient privileges to perform this data import.");
                    hasRolePermission = false;
                }

            } else {
                e.reject(ErrorConstants.ERR_NO_ROLE_SETUP, "No role configured for user " + userName + " in study " + studyOid);
                hasRolePermission = false;
            }

        } else {
            if (studyLevelRole.getId() == 0 || studyLevelRole.getRole().equals(Role.MONITOR)) {
                e.reject(ErrorConstants.ERR_NO_SUFFICIENT_PRIVILEGES, "User account does not have sufficient privileges to perform this data import.");
                hasRolePermission = false;
            }
        }


        return hasRolePermission;

    }

    /**
     * @param userName
     * @param study_oid
     * @param site_oid
     * @return
     */
    public String verifyRole(String userName, String study_oid,
                             String site_oid) {

        String studyOid = study_oid;
        String siteOid = site_oid;
        String err_msg = null;

        StudyUserRoleBean studyLevelRole = this.getUserAccountDAO().findTheRoleByUserNameAndStudyOid(userName, studyOid);
        if (studyLevelRole == null) {
            if (siteOid != null) {

                StudyUserRoleBean siteLevelRole = this.getUserAccountDAO().findTheRoleByUserNameAndStudyOid(userName, siteOid);
                if (siteLevelRole == null) {
                    err_msg = ErrorConstants.ERR_NO_ROLE_SETUP + " You do not have any role set up for user " + userName + " in study site " + siteOid;
                } else if (siteLevelRole.getId() == 0 || siteLevelRole.getRole().equals(Role.MONITOR)) {
                    err_msg = ErrorConstants.ERR_NO_SUFFICIENT_PRIVILEGES + " You do not have sufficient privileges to proceed with this operation.";
                }

            } else {
                err_msg = ErrorConstants.ERR_NO_ROLE_SETUP + " You do not have any role set up for user " + userName + " in study " + studyOid;

            }

        } else {
            if (studyLevelRole.getId() == 0 || studyLevelRole.getRole().equals(Role.MONITOR)) {
                err_msg = ErrorConstants.ERR_NO_SUFFICIENT_PRIVILEGES + " You do not have sufficient privileges to proceed with this operation.";

            }
        }


        return err_msg;

    }

    /**
     * Helper Method to get the user account
     * @return UserAccountBean
     */
    public UserAccountBean getUserAccount(HttpServletRequest request) {
        UserAccountBean userBean;

        if (request.getSession() != null && request.getSession().getAttribute("userBean") != null) {
            userBean = (UserAccountBean) request.getSession().getAttribute("userBean");

        } else {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = null;
            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else {
                username = principal.toString();
            }

            String schema = CoreResources.getRequestSchema();
            CoreResources.setRequestSchemaToPublic();
            UserAccountDAO userAccountDAO = new UserAccountDAO(dataSource);
            userBean = (UserAccountBean) userAccountDAO.findByUserName(username);
            CoreResources.setRequestSchema(schema);

        }

        return userBean;

    }

    public UserAccountDAO getUserAccountDAO() {
        userAccountDAO = userAccountDAO != null ? userAccountDAO : new UserAccountDAO(dataSource);
        return userAccountDAO;
    }


    public FlatFileImportDataHelper getImportDataHelper() {
        if (importDataHelper == null) {
            importDataHelper = new FlatFileImportDataHelper(this.dataSource, studyBuildService, studyDao);
        }
        return importDataHelper;
    }

    /**
     * @param dateTimeStr: yyyy-MM-dd
     * @return
     */
    public Date getDateTime(String dateTimeStr) throws OpenClinicaException {
        String dataFormat = "yyyy-MM-dd";
        Date result = null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataFormat);
            LocalDate parsedDate = LocalDate.parse(dateTimeStr, formatter);

            result = Date.from(parsedDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        } catch (DateTimeParseException e) {
            String errMsg = "The input date(" + dateTimeStr + ") can't be parsed, please use the correct format " + dataFormat;
            throw new OpenClinicaException(errMsg, ErrorConstants.ERR_PARSE_DATE);
        }

        return result;
    }

}
