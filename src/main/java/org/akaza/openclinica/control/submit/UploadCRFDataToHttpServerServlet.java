package org.akaza.openclinica.control.submit;

import core.org.akaza.openclinica.bean.core.Role;
import core.org.akaza.openclinica.bean.login.StudyUserRoleBean;
import core.org.akaza.openclinica.bean.login.UserAccountBean;
import core.org.akaza.openclinica.bean.rule.FileUploadHelper;
import core.org.akaza.openclinica.core.SessionManager;
import core.org.akaza.openclinica.core.form.StringUtil;
import core.org.akaza.openclinica.dao.core.CoreResources;
import core.org.akaza.openclinica.dao.hibernate.UserAccountDao;
import core.org.akaza.openclinica.domain.datamap.JobDetail;
import core.org.akaza.openclinica.domain.datamap.Study;
import core.org.akaza.openclinica.domain.enumsupport.JobType;
import core.org.akaza.openclinica.domain.user.UserAccount;
import core.org.akaza.openclinica.i18n.core.LocaleResolver;
import core.org.akaza.openclinica.logic.importdata.FlatFileImportDataHelper;
import core.org.akaza.openclinica.service.CustomParameterizedException;
import core.org.akaza.openclinica.web.InsufficientPermissionException;
import core.org.akaza.openclinica.web.SQLInitServlet;
import org.akaza.openclinica.control.SpringServletAccess;
import org.akaza.openclinica.control.core.SecureController;
import org.akaza.openclinica.control.form.FormProcessor;
import org.akaza.openclinica.service.ImportService;
import org.akaza.openclinica.view.Page;
import org.akaza.openclinica.web.restful.errors.ErrorConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;


public class UploadCRFDataToHttpServerServlet extends SecureController {

    Locale locale;
    private FileUploadHelper uploadHelper = new FileUploadHelper();
    private FlatFileImportDataHelper flatFileImportDataHelper;


    /**
     *
     */
    @Override
    public void mayProceed() throws InsufficientPermissionException {
        checkStudyLocked(Page.MENU_SERVLET, respage.getString("current_study_locked"));
        checkStudyFrozen(Page.MENU_SERVLET, respage.getString("current_study_frozen"));

        locale = LocaleResolver.getLocale(request);
        if (ub.isSysAdmin()) {
            return;
        }

        Role r = currentRole.getRole();
        if (r.equals(Role.STUDYDIRECTOR) || r.equals(Role.COORDINATOR) || r.equals(Role.INVESTIGATOR) || r.equals(Role.RESEARCHASSISTANT)
                || r.equals(Role.RESEARCHASSISTANT2)) {
            return;
        }

        addPageMessage(respage.getString("no_have_correct_privilege_current_study") + respage.getString("change_study_contact_sysadmin"));
        throw new InsufficientPermissionException(Page.MENU_SERVLET, resexception.getString("may_not_submit_data"), "1");
    }

    @Override
    public void processRequest() throws Exception {
        resetPanel();
        panel.setStudyInfoShown(false);
        panel.setOrderedData(true);

        FormProcessor fp = new FormProcessor(request);
        // checks which module the requests are from
        String module = fp.getString(MODULE);
        // keep the module in the session
        session.setAttribute(MODULE, module);

        String action = request.getParameter("action");
        HashMap hm;

        String submitted = (String) request.getParameter("submitted");
        if (submitted != null && submitted.equals("true")) {
            request.removeAttribute("submitted");

            String message = "The Application is processing your files, you can come back later to check the status and with detail in the bulk action logs page";
            this.addPageMessage(message);
            forwardPage(Page.UPLOAD_CRF_DATA_TO_MIRTH);

            return;
        }

        if (StringUtil.isBlank(action)) {
            logger.info("action is blank");

            forwardPage(Page.UPLOAD_CRF_DATA_TO_MIRTH);
        } else if ("confirm".equalsIgnoreCase(action)) {
            String dir = SQLInitServlet.getField("filePath");
            if (!new File(dir).exists()) {
                logger.info("The filePath in datainfo.properties is invalid " + dir);
                addPageMessage(respage.getString("system_configuration_filepath_not_valid"));
                forwardPage(Page.UPLOAD_CRF_DATA_TO_MIRTH);
            }
            // All the uploaded files will be saved in filePath/crf/original/
            String theDir = dir + "import" + File.separator + "original" + File.separator;
            if (!new File(theDir).isDirectory()) {
                new File(theDir).mkdirs();
                logger.info("Made the directory " + theDir);
            }

            List<File> files;
            try {
                // here process all uploaded files
                files = uploadFiles(theDir);
                File mappingFile = null;
                boolean foundMappingFile = false;

                for (File file : files) {

                    if (file == null || file.getName() == null) {
                        logger.info("file is empty.");

                    } else {
                        if (file.getName().toLowerCase().lastIndexOf(".properties") > -1) {
                            mappingFile = file;
                            foundMappingFile = true;
                            logger.info("Found mapping file *.properties uploaded");

                            break;
                        }
                    }
                }

                if (files.size() < 2) {
                    String message = "errorCode.notCorrectFileNumber: Please upload one data file and one mapping file with .properties file extension.";
                    this.addPageMessage(message);
                    removeFiles(files);
                    forwardPage(Page.UPLOAD_CRF_DATA_TO_MIRTH);
                    return;
                }

                if (!foundMappingFile) {
                    String message = "errorCode.noMappingfileFound: Please upload one data file and one mapping file with .properties file extension.";
                    this.addPageMessage(message);
                    removeFiles(files);
                    forwardPage(Page.UPLOAD_CRF_DATA_TO_MIRTH);
                    return;
                } else {
                    try {
                        hm = this.getFlatFileImportDataHelper().validateMappingFile(mappingFile);
                    } catch (Exception e) {
                        String message = e.getMessage();
                        this.addPageMessage(message);
                        removeFiles(files);
                        forwardPage(Page.UPLOAD_CRF_DATA_TO_MIRTH);
                        return;
                    }

                }

                //MockHttpServletRequest requestMock = getMockRequest(request);
                String studyOid = null;
                for (File file : files) {
                    if (file.getName().toLowerCase().endsWith(".properties")) {
                        studyOid = this.getFlatFileImportDataHelper().getStudyOidFromMappingFile(file);
                        break;
                    }

                }
                final String studyOID = studyOid;

                // redirect first
                this.response.sendRedirect("/OpenClinica/UploadCRFData?submitted=true");

                //////////////// Start of heavy thread run/////////////////////
                final HashMap hmIn = hm;
                UserAccountBean userAccountBean = getUtilService().getUserAccountFromRequest(request);

                try {
                    validateStudyOidRolesAndStartImportJob(request, files, hmIn, studyOID, userAccountBean);
                } catch (Exception e) {
                    logger.error("Error sending data row for request: ", e);
                }
                return;


            } catch (Exception e) {
                logger.error("*** Found exception during file upload***", e);

                String message = "Please selected correct files to resubmit.";
                this.addPageMessage(message);

                forwardPage(Page.UPLOAD_CRF_DATA_TO_MIRTH);
            }


        } else if ("download".equalsIgnoreCase(action)) {
            String studyID = request.getParameter("studyId");
            String parentNm = request.getParameter("parentNm");
            String fileName = request.getParameter("fileId");
            File file = this.getFlatFileImportDataHelper().getImportFileByStudyIDParentNm(studyID, parentNm, fileName);
            dowloadFile(file, "text/csv");

        } else if ("delete".equalsIgnoreCase(action)) {
            String studyID = request.getParameter("studyId");
            String parentNm = request.getParameter("parentNm");
            String fileName = request.getParameter("fileId");
            File tempFile = this.getFlatFileImportDataHelper().getImportFileByStudyIDParentNm(studyID, parentNm, fileName);

            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }

            String fromUrl = request.getParameter("fromUrl");
            if (fromUrl.equals("UploadCRFData")) {
                forwardPage(Page.UPLOAD_CRF_DATA_TO_MIRTH);
            } else if (fromUrl.equals("listLog")) {
                RequestDispatcher dis = request.getRequestDispatcher("/pages/Log/listFiles");
                dis.forward(request, response);
            } else {
                RequestDispatcher dis = request.getRequestDispatcher("/pages/Log/listFiles");
                dis.forward(request, response);
            }
        }
    }

    /**
     * Check study oid, site oid, user role permissions and start import.
     * @param request request
     * @param files the mapping file and the txt file
     * @param hm mapping file hashmap
     * @param studyOID studyoid
     * @param userAccountBean userAccountBean
     * @return job uuid
     */
    public ResponseEntity<Object> validateStudyOidRolesAndStartImportJob(HttpServletRequest request, List<File> files, HashMap hm,
                                                                         String studyOID, UserAccountBean userAccountBean) {
        String accessToken = (String) request.getSession().getAttribute("accessToken");
        String fileNm = getFlatFileImportDataHelper().getFileName(files);
        studyOID = studyOID.toUpperCase();
        Study publicStudy = getStudyDao().findPublicStudy(studyOID);
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

        getUtilService().setSchemaFromStudyOid(studyOid);
        String schema = CoreResources.getRequestSchema();

        ArrayList<StudyUserRoleBean> userRoles = userAccountBean.getRoles();

        if (!getValidateService().isStudyAvailable(studyOid)) {
            return new ResponseEntity(ErrorConstants.ERR_STUDY_NOT_AVAILABLE, HttpStatus.OK);
        }

        if (siteOid != null && !getValidateService().isSiteAvailable(siteOid)) {
            return new ResponseEntity(ErrorConstants.ERR_SITE_NOT_AVAILABLE, HttpStatus.OK);
        }

        if (!getValidateService().isStudyOidValid(studyOid)) {
            return new ResponseEntity(ErrorConstants.ERR_STUDY_NOT_EXIST, HttpStatus.OK);
        }

        // If the import is performed by a system user, then we can skip the roles check.
        boolean isSystemUserImport = getValidateService().isUserSystemUser(request);
        if (!isSystemUserImport) {
            if (siteOid != null) {
                if (!getValidateService().isUserHasAccessToSite(userRoles, siteOid)) {
                    return new ResponseEntity(ErrorConstants.ERR_NO_ROLE_SETUP, HttpStatus.OK);
                } else if (!getValidateService().isUserHas_CRC_INV_DM_DEP_DS_RoleInSite(userRoles, siteOid)) {
                    return new ResponseEntity(ErrorConstants.ERR_NO_SUFFICIENT_PRIVILEGES, HttpStatus.OK);
                }
            } else {
                if (!getValidateService().isUserHasAccessToStudy(userRoles, studyOid)) {
                    return new ResponseEntity(ErrorConstants.ERR_NO_ROLE_SETUP, HttpStatus.OK);
                } else if (!getValidateService().isUserHas_DM_DEP_DS_RoleInStudy(userRoles, studyOid)) {
                    return new ResponseEntity(ErrorConstants.ERR_NO_SUFFICIENT_PRIVILEGES, HttpStatus.OK);
                }
            }
        }

        String uuid;
        try {
            uuid = startImportJob(files, hm, studyOid, siteOid, userAccountBean, fileNm, schema, isSystemUserImport, accessToken);
            return new ResponseEntity("Job uuid: " + uuid, org.springframework.http.HttpStatus.OK);
        } catch (CustomParameterizedException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public String startImportJob(List<File> files, HashMap hm, String studyOid, String siteOid,
                                 UserAccountBean userAccountBean, String fileNm, String schema, boolean isSystemUserImport, String accessToken) {
        getUtilService().setSchemaFromStudyOid(studyOid);

        Study site = getStudyDao().findByOcOID(siteOid);
        Study study = getStudyDao().findByOcOID(studyOid);
        UserAccount userAccount = getUserAccountDao().findById(userAccountBean.getId());
        if (isSystemUserImport) {
            // For system level imports, instead of running import as an asynchronous job, run it synchronously
            logger.debug("Running import synchronously");
            try {
                getImportService().validateAndProcessFlatFileDataImport(files, hm, studyOid, siteOid, userAccountBean, isSystemUserImport, null, schema, accessToken);
            } catch (Exception e) {
                throw new CustomParameterizedException(ErrorConstants.ERR_IMPORT_FAILED);
            }
            return null;
        } else {
            JobDetail jobDetail = getUserService().persistJobCreated(study, site, userAccount, JobType.FLAT_FILE_IMPORT, fileNm);
            CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                try {
                    getImportService().validateAndProcessFlatFileDataImport(files, hm, studyOid, siteOid, userAccountBean, isSystemUserImport, jobDetail, schema, accessToken);
                    //importService.validateAndProcessFlatFileDataImport(odmContainer, studyOid, siteOid, userAccountBean, schema, jobDetail, isSystemUserImport);
                } catch (Exception e) {
                    logger.error("Exception is thrown while processing dataImport: " + e);
                    getUserService().persistJobFailed(jobDetail, fileNm);
                }
                return null;

            });
            return jobDetail.getUuid();
        }
    }

    /**
     * @param files
     */
    private void removeFiles(List<File> files) {
        // remove temporary uploaded files
        for (File file : files) {
            if (file.exists()) {
                file.delete();
            }
        }
    }


    private List<File> getUploadedFiles() {
        File f = null;

        List<File> files = uploadHelper.returnFiles(request, context);
        for (File file : files) {
            f = file;
            if (f == null || f.getName() == null) {
                logger.info("file is empty.");

            } else {
                if (f.getName().equals("mapping.txt")) {
                    logger.info("Found mapping.txt uploaded");
                    break;
                }
            }
        }
        return files;
    }

    public List<File> uploadFiles(String theDir) throws Exception {

        return getUploadedFiles();
    }


    @Override
    protected String getAdminServlet() {
        if (ub.isSysAdmin()) {
            return SecureController.ADMIN_SERVLET_CODE;
        } else {
            return "";
        }
    }

    public FlatFileImportDataHelper getFlatFileImportDataHelper() {
        if (flatFileImportDataHelper == null) {
            flatFileImportDataHelper = new FlatFileImportDataHelper(this.getSM().getDataSource(), getStudyBuildService(), getStudyDao());
        }
        return flatFileImportDataHelper;
    }

    protected UserAccountDao getUserAccountDao() {
        return (UserAccountDao) SpringServletAccess.getApplicationContext(context).getBean("userDaoDomain");
    }

    protected ImportService getImportService() {
        return (ImportService) SpringServletAccess.getApplicationContext(context).getBean("importService");
    }

    public SessionManager getSM() {
        UserAccountBean ub = (UserAccountBean) session.getAttribute(USER_BEAN_NAME);
        String userName = request.getRemoteUser();

        if (this.sm == null) {
            try {
                sm = new SessionManager(ub, userName, SpringServletAccess.getApplicationContext(context));
            } catch (SQLException e) {
                logger.error("Session Manager is not initializing properly: ", e);
            }
        }

        return sm;
    }
}
