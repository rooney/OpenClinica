package org.akaza.openclinica.control.submit;

import core.org.akaza.openclinica.bean.core.Role;
import core.org.akaza.openclinica.bean.login.UserAccountBean;
import core.org.akaza.openclinica.bean.rule.FileUploadHelper;
import core.org.akaza.openclinica.bean.rule.XmlSchemaValidationHelper;
import core.org.akaza.openclinica.core.SessionManager;
import core.org.akaza.openclinica.core.form.StringUtil;
import core.org.akaza.openclinica.dao.core.CoreResources;
import core.org.akaza.openclinica.dao.hibernate.StudyDao;
import core.org.akaza.openclinica.dao.hibernate.UserAccountDao;
import core.org.akaza.openclinica.dao.login.UserAccountDAO;
import core.org.akaza.openclinica.i18n.core.LocaleResolver;
import core.org.akaza.openclinica.web.InsufficientPermissionException;
import core.org.akaza.openclinica.web.SQLInitServlet;
import org.akaza.openclinica.control.SpringServletAccess;
import org.akaza.openclinica.control.core.SecureController;
import org.akaza.openclinica.control.form.FormProcessor;
import org.akaza.openclinica.controller.FlatFileImportController;
import org.akaza.openclinica.controller.helper.RestfulServiceHelper;
import org.akaza.openclinica.service.CsvFileConverterServiceImpl;
import org.akaza.openclinica.service.ExcelFileConverterServiceImpl;
import org.akaza.openclinica.service.ImportService;
import org.akaza.openclinica.service.SasFileConverterServiceImpl;
import org.akaza.openclinica.view.Page;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class UploadCRFDataToHttpServerServlet extends SecureController {

    Locale locale;
    private FileUploadHelper uploadHelper = new FileUploadHelper();
    private RestfulServiceHelper restfulServiceHelper;
    private FlatFileImportController flatFileImportController;


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
                        hm = this.getRestfulServiceHelper().getImportDataHelper().validateMappingFile(mappingFile);
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
                        studyOid = this.getRestfulServiceHelper().getImportDataHelper().getStudyOidFromMappingFile(file);
                        break;
                    }

                }
                final String studyOID = studyOid;

                // redirect first
                this.response.sendRedirect("/OpenClinica/UploadCRFData?submitted=true");

                //////////////// Start of heavy thread run/////////////////////
                final HashMap hmIn = hm;
                UserAccountBean userAccountBean = getUtilService().getUserAccountFromRequest(request);
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            getFlatFileImportController().processDataAndStartImportJob(request, files, hmIn, studyOID, userAccountBean);
                        } catch (Exception e) {
                            logger.error("Error sending data row for request: ", e);
                        }
                        ;
                    }
                }).start();

                ///////////////// end of heavy thread run/////////////////////

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
            File file = this.getRestfulServiceHelper().getImportDataHelper().getImportFileByStudyIDParentNm(studyID, parentNm, fileName);
            dowloadFile(file, "text/csv");

        } else if ("delete".equalsIgnoreCase(action)) {
            String studyID = request.getParameter("studyId");
            String parentNm = request.getParameter("parentNm");
            String fileName = request.getParameter("fileId");
            File tempFile = this.getRestfulServiceHelper().getImportDataHelper().getImportFileByStudyIDParentNm(studyID, parentNm, fileName);

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
        boolean foundMappingFile = false;

        List<File> files = uploadHelper.returnFiles(request, context);
        for (File file : files) {
            f = file;
            if (f == null || f.getName() == null) {
                logger.info("file is empty.");

            } else {
                if (f.getName().equals("mapping.txt")) {
                    foundMappingFile = true;
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

    public RestfulServiceHelper getRestfulServiceHelper() {
        if (restfulServiceHelper == null) {
            restfulServiceHelper = new RestfulServiceHelper(this.getSM().getDataSource(), getStudyBuildService(), getStudyDao(), getSasFileConverterService(), getExcelFileConverterService(), getCsvFileConverterService());
        }
        return restfulServiceHelper;
    }

    protected SasFileConverterServiceImpl getSasFileConverterService() {
        return (SasFileConverterServiceImpl) SpringServletAccess.getApplicationContext(context).getBean("sasFileConverterService");
    }

    protected ExcelFileConverterServiceImpl getExcelFileConverterService() {
        return (ExcelFileConverterServiceImpl) SpringServletAccess.getApplicationContext(context).getBean("excelFileConverterService");
    }

    protected CsvFileConverterServiceImpl getCsvFileConverterService() {
        return (CsvFileConverterServiceImpl) SpringServletAccess.getApplicationContext(context).getBean("csvFileConverterService");
    }

    protected UserAccountDao getUserAccountDao() {
        return (UserAccountDao) SpringServletAccess.getApplicationContext(context).getBean("userDaoDomain");
    }

    protected ImportService getImportService() {
        return (ImportService) SpringServletAccess.getApplicationContext(context).getBean("importService");
    }

    public FlatFileImportController getFlatFileImportController() {
        if (flatFileImportController == null) {
            flatFileImportController = new FlatFileImportController(getStudyDao(), getUserAccountDao(), getValidateService(), getUtilService(), getImportService(), getUserService(),
                    getSasFileConverterService(), getExcelFileConverterService(), getCsvFileConverterService(), this.getSM().getDataSource());
        }
        return flatFileImportController;
    }

    public void setRestfulServiceHelper(RestfulServiceHelper restfulServiceHelper) {
        this.restfulServiceHelper = restfulServiceHelper;
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
