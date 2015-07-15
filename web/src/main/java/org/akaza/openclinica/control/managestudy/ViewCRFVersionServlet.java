/*
 * OpenClinica is distributed under the
 * GNU Lesser General Public License (GNU LGPL).

 * For details see: http://www.openclinica.org/license
 * copyright 2003-2005 Akaza Research
 */
package org.akaza.openclinica.control.managestudy;

import org.akaza.openclinica.bean.admin.CRFBean;
import org.akaza.openclinica.bean.core.Role;
import org.akaza.openclinica.bean.submit.CRFVersionBean;
import org.akaza.openclinica.bean.submit.DisplaySectionBean;
import org.akaza.openclinica.bean.submit.ItemBean;
import org.akaza.openclinica.bean.submit.ItemFormMetadataBean;
import org.akaza.openclinica.bean.submit.ItemGroupBean;
import org.akaza.openclinica.bean.submit.ItemGroupMetadataBean;
import org.akaza.openclinica.bean.submit.SectionBean;
import org.akaza.openclinica.control.core.SecureController;
import org.akaza.openclinica.control.form.FormProcessor;
import org.akaza.openclinica.dao.admin.CRFDAO;
import org.akaza.openclinica.dao.submit.CRFVersionDAO;
import org.akaza.openclinica.dao.submit.ItemDAO;
import org.akaza.openclinica.dao.submit.ItemFormMetadataDAO;
import org.akaza.openclinica.dao.submit.ItemGroupDAO;
import org.akaza.openclinica.dao.submit.ItemGroupMetadataDAO;
import org.akaza.openclinica.dao.submit.SectionDAO;
import org.akaza.openclinica.view.Page;
import org.akaza.openclinica.web.InsufficientPermissionException;
import org.akaza.openclinica.control.SpringServletAccess;
import org.akaza.openclinica.dao.hibernate.SCDItemMetadataDao;
import org.akaza.openclinica.bean.managestudy.StudyBean;
import org.akaza.openclinica.bean.login.UserAccountBean;
import org.akaza.openclinica.dao.submit.EventCRFDAO;
import org.akaza.openclinica.dao.submit.ItemDataDAO;
import org.akaza.openclinica.dao.managestudy.StudyEventDAO;
import org.akaza.openclinica.dao.managestudy.EventDefinitionCRFDAO;
import org.akaza.openclinica.domain.SourceDataVerification;
import org.akaza.openclinica.bean.core.SubjectEventStatus;
import org.akaza.openclinica.util.DAOWrapper;
import org.akaza.openclinica.dao.managestudy.StudyDAO;
import org.akaza.openclinica.dao.managestudy.StudySubjectDAO;
import org.akaza.openclinica.dao.managestudy.DiscrepancyNoteDAO;
import org.akaza.openclinica.bean.managestudy.StudyEventBean;
import org.akaza.openclinica.util.SubjectEventStatusUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * @author jxu
 *
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class ViewCRFVersionServlet extends SecureController {
    private static final String TYPE = "ViewCRFVersionServlet";
    private static final String YES = "yes";
    private static final String STORED_ATTRIBUTES = "RememberLastPage_storedAttributes";
    private static final String STUDY = "study";
    /**
     * Checks whether the user has the right permission to proceed function
     */
    @Override
    public void mayProceed() throws InsufficientPermissionException {
        if (ub.isSysAdmin()) {
            return;
        }
        if (currentRole.getRole().equals(Role.STUDYDIRECTOR) || currentRole.getRole().equals(Role.COORDINATOR)) {
            return;
        }

        addPageMessage(respage.getString("no_have_correct_privilege_current_study") + " " + respage.getString("change_study_contact_sysadmin"));
        throw new InsufficientPermissionException(Page.MENU_SERVLET, resexception.getString("not_director"), "1");

    }

    @Override
    public void processRequest() throws Exception {

        CRFVersionDAO cvdao = new CRFVersionDAO(sm.getDataSource());
        ItemDAO idao = new ItemDAO(sm.getDataSource());
        ItemFormMetadataDAO ifmdao = new ItemFormMetadataDAO(sm.getDataSource());
        FormProcessor fp = new FormProcessor(request);
        // checks which module the requests are from
        String module = fp.getString(MODULE);
        request.setAttribute(MODULE, module);

        if (request.getMethod().equalsIgnoreCase("post")) {
            Map<Integer, Boolean> metadata = new HashMap<Integer, Boolean>();
            for (int i = 1; i <= fp.getInt("totalItems"); i++) {
                metadata.put(fp.getInt("itemFormMetaId_".concat(Integer.toString(i))),
                        fp.getInt("sdvRequired_".concat(Integer.toString(i))) == 1);
            }
            processChangedCrfVersionMetadata((StudyBean) request.getSession().getAttribute(STUDY), (UserAccountBean) request.getSession().getAttribute(USER_BEAN_NAME),
                    fp.getInt("crfVersionId"), metadata);
            addPageMessage(respage.getString("data_was_saved_successfully"));
            Map storedAttributes = new HashMap();
            storedAttributes.put(PAGE_MESSAGE, request.getAttribute(PAGE_MESSAGE));
            request.getSession().setAttribute(STORED_ATTRIBUTES, storedAttributes);
            response.sendRedirect(request.getContextPath().concat("/ViewCRF?crfId=")
                    .concat(request.getParameter("crfId")));
        } else {

            int crfVersionId = fp.getInt("id");

            if (crfVersionId == 0) {
                addPageMessage(respage.getString("please_choose_a_crf_to_view_details"));
                forwardPage(Page.CRF_LIST_SERVLET);
            } else {
                CRFVersionBean version = (CRFVersionBean) cvdao.findByPK(crfVersionId);
                // tbh
                CRFDAO crfdao = new CRFDAO(sm.getDataSource());
                CRFBean crf = (CRFBean) crfdao.findByPK(version.getCrfId());
                CRFVersionMetadataUtil metadataUtil = new CRFVersionMetadataUtil(sm.getDataSource());
                ArrayList<DisplaySectionBean> sections = metadataUtil.retrieveFormMetadata(version, TYPE, (SCDItemMetadataDao) SpringServletAccess.getApplicationContext(context).getBean("scdItemMetadataDao")); 

                request.setAttribute("sections", sections);
                request.setAttribute("version", version);
                // tbh
                request.setAttribute("crf", crf);
                // tbh
                forwardPage(Page.VIEW_CRF_VERSION);

            }
        }
    }

    public void processChangedCrfVersionMetadata(StudyBean currentStudy, UserAccountBean userAccountBean,
            int crfVersionId, Map<Integer, Boolean> metadata) throws Exception {
        EventCRFDAO eventCrfDao = new EventCRFDAO(sm.getDataSource());
        ItemDataDAO itemDataDao = new ItemDataDAO(sm.getDataSource());
        StudyEventDAO studyEventDao = new StudyEventDAO(sm.getDataSource());
        CRFVersionDAO crfVersionDao = new CRFVersionDAO(sm.getDataSource());
        ItemFormMetadataDAO itemFormMetadataDao = new ItemFormMetadataDAO(sm.getDataSource());
        EventDefinitionCRFDAO eventDefinitionCrfDao = new EventDefinitionCRFDAO(sm.getDataSource());
        boolean dataChanged = false;
        boolean unSdvEventCrfBeans = false;
        for (Integer itemFormMetaId : metadata.keySet()) {
            boolean sdvRequired = metadata.get(itemFormMetaId);
            ItemFormMetadataBean itemFormMetadataBean = (ItemFormMetadataBean) itemFormMetadataDao
                    .findByPK(itemFormMetaId);
            if (itemFormMetadataBean.isShowItem() && itemFormMetadataBean.isSdvRequired() != sdvRequired) {
                dataChanged = true;
                if (!itemFormMetadataBean.isSdvRequired()) {
                    unSdvEventCrfBeans = true;
                }
            }
            itemFormMetadataBean.setSdvRequired(sdvRequired);
            itemFormMetadataDao.update(itemFormMetadataBean);
        }
        if (dataChanged) {
            eventDefinitionCrfDao.updateEDCThatHasItemsToSDV(crfVersionId, SourceDataVerification.PARTIALREQUIRED);
            itemDataDao.unsdvItemDataWhenCRFMetadataWasChanged(crfVersionId);
            if (unSdvEventCrfBeans) {
                eventCrfDao.unsdvEventCRFsWhenCRFMetadataWasChanged(crfVersionId, userAccountBean.getId());
            } else {
                eventCrfDao.sdvEventCRFsWhenCRFMetadataWasChangedAndAllItemsAreSDV(crfVersionId,
                        userAccountBean.getId(), currentStudy.getStudyParameterConfig().getAllowSdvWithOpenQueries()
                                .equalsIgnoreCase(YES));
            }
            SubjectEventStatusUtil.determineSubjectEventStates(studyEventDao.findStudyEventsByCrfVersionAndSubjectEventStatus(crfVersionId, unSdvEventCrfBeans
                            ? SubjectEventStatus.SOURCE_DATA_VERIFIED
                            : SubjectEventStatus.COMPLETED), userAccountBean, new DAOWrapper(new StudyDAO(sm.getDataSource()),
                    crfVersionDao, studyEventDao, new StudySubjectDAO(sm.getDataSource()), eventCrfDao, eventDefinitionCrfDao,
                    new DiscrepancyNoteDAO(sm.getDataSource())), null);
        }
    }
}
