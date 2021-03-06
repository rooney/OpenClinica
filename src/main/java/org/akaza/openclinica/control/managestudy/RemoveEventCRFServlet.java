/*
 * OpenClinica is distributed under the
 * GNU Lesser General Public License (GNU LGPL).

 * For details see: http://www.openclinica.org/license
 * copyright 2003-2005 Akaza Research
 */
package org.akaza.openclinica.control.managestudy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import core.org.akaza.openclinica.bean.admin.CRFBean;
import core.org.akaza.openclinica.bean.core.ResolutionStatus;
import core.org.akaza.openclinica.bean.core.Role;
import core.org.akaza.openclinica.bean.core.Status;
import core.org.akaza.openclinica.bean.login.UserAccountBean;
import core.org.akaza.openclinica.bean.managestudy.DiscrepancyNoteBean;
import core.org.akaza.openclinica.bean.managestudy.EventDefinitionCRFBean;
import core.org.akaza.openclinica.bean.managestudy.StudyEventBean;
import core.org.akaza.openclinica.bean.managestudy.StudyEventDefinitionBean;
import core.org.akaza.openclinica.bean.managestudy.StudySubjectBean;
import core.org.akaza.openclinica.bean.submit.*;
import core.org.akaza.openclinica.dao.hibernate.StudyDao;
import core.org.akaza.openclinica.domain.datamap.EventDefinitionCrf;
import core.org.akaza.openclinica.domain.datamap.Study;
import core.org.akaza.openclinica.core.LockInfo;
import core.org.akaza.openclinica.dao.admin.CRFDAO;
import core.org.akaza.openclinica.dao.hibernate.EventCrfDao;
import core.org.akaza.openclinica.dao.login.UserAccountDAO;
import core.org.akaza.openclinica.dao.managestudy.DiscrepancyNoteDAO;
import core.org.akaza.openclinica.dao.managestudy.EventDefinitionCRFDAO;
import core.org.akaza.openclinica.dao.managestudy.StudyEventDAO;
import core.org.akaza.openclinica.dao.managestudy.StudyEventDefinitionDAO;
import core.org.akaza.openclinica.dao.managestudy.StudySubjectDAO;
import core.org.akaza.openclinica.dao.submit.EventCRFDAO;
import core.org.akaza.openclinica.dao.submit.FormLayoutDAO;
import core.org.akaza.openclinica.dao.submit.ItemDataDAO;
import core.org.akaza.openclinica.domain.datamap.EventCrf;
import core.org.akaza.openclinica.web.InsufficientPermissionException;
import org.akaza.openclinica.control.SpringServletAccess;
import org.akaza.openclinica.control.core.SecureController;
import org.akaza.openclinica.control.form.FormProcessor;
import org.akaza.openclinica.domain.enumsupport.StudyEventWorkflowStatusEnum;
import org.akaza.openclinica.view.Page;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Removes an Event CRF
 * 
 * @author jxu
 * 
 */
public class RemoveEventCRFServlet extends SecureController {

    private StudyEventDAO studyEventDAO;
    private EventCRFDAO eventCRFDAO;
    /**
     * 
     */
    @Override
    public void mayProceed() throws InsufficientPermissionException {
        checkStudyLocked(Page.LIST_STUDY_SUBJECTS, respage.getString("current_study_locked"));
        checkStudyFrozen(Page.LIST_STUDY_SUBJECTS, respage.getString("current_study_frozen"));

        if (ub.isSysAdmin()) {
            return;
        }

        if (!currentRole.getRole().equals(Role.MONITOR) ){
            return;
        }
        addPageMessage(respage.getString("no_have_correct_privilege_current_study") + respage.getString("change_study_contact_sysadmin"));
        throw new InsufficientPermissionException(Page.MENU_SERVLET, resexception.getString("not_study_director"), "1");

    }

    @Override
    public void processRequest() throws Exception {
        FormProcessor fp = new FormProcessor(request);
        studyEventDAO = (StudyEventDAO) SpringServletAccess.getApplicationContext(context).getBean("studyEventJDBCDao");
        int eventCRFId = fp.getInt("eventCrfId");// eventCRFId
        int studySubId = fp.getInt("studySubId");// studySubjectId
        checkStudyLocked("ViewStudySubject?id" + studySubId, respage.getString("current_study_locked"));
        String originatingPage = request.getParameter(ORIGINATING_PAGE);
        request.setAttribute(ORIGINATING_PAGE, originatingPage);
        StudySubjectDAO subdao = new StudySubjectDAO(sm.getDataSource());
        EventCrfDao eventCrfDao = (EventCrfDao) SpringServletAccess.getApplicationContext(context).getBean("eventCrfDao");
        eventCRFDAO = (EventCRFDAO) SpringServletAccess.getApplicationContext(context).getBean("eventCRFJDBCDao");

        if (eventCRFId == 0) {
            addPageMessage(respage.getString("please_choose_an_event_CRF_to_remove"));
            request.setAttribute("id", new Integer(studySubId).toString());
            forwardPage(Page.VIEW_STUDY_SUBJECT_SERVLET);
        } else {
            EventCRFBean eventCRF = (EventCRFBean) eventCRFDAO.findByPK(eventCRFId);
            final EventCrf ec = eventCrfDao.findById(eventCRFId);

            if (hasFormAccess(ec) != true) {
                forwardPage(Page.NO_ACCESS);
                return;
            }
            StudySubjectBean studySub = (StudySubjectBean) subdao.findByPK(studySubId);
            request.setAttribute("studySub", studySub);

            // construct info needed on view event crf page
            CRFDAO cdao = new CRFDAO(sm.getDataSource());
            FormLayoutDAO fldao = new FormLayoutDAO(sm.getDataSource());

            int formLayoutId = eventCRF.getFormLayoutId();
            CRFBean cb = cdao.findByLayoutId(formLayoutId);
            eventCRF.setCrf(cb);

            FormLayoutBean flb = (FormLayoutBean) fldao.findByPK(formLayoutId);
            eventCRF.setFormLayout(flb);

            // then get the definition so we can call
            // DisplayEventCRFBean.setFlags
            int studyEventId = eventCRF.getStudyEventId();

            StudyEventBean event = (StudyEventBean) studyEventDAO.findByPK(studyEventId);

            int studyEventDefinitionId = studyEventDAO.getDefinitionIdFromStudyEventId(studyEventId);
            StudyEventDefinitionDAO seddao = new StudyEventDefinitionDAO(sm.getDataSource());
            StudyEventDefinitionBean sed = (StudyEventDefinitionBean) seddao.findByPK(studyEventDefinitionId);
            event.setStudyEventDefinition(sed);
            request.setAttribute("event", event);

            EventDefinitionCRFDAO edcdao = new EventDefinitionCRFDAO(sm.getDataSource());

            Study study = (Study) getStudyDao().findByPK(studySub.getStudyId());
            EventDefinitionCRFBean edc = edcdao.findByStudyEventDefinitionIdAndCRFId(study, studyEventDefinitionId, cb.getId());

            DisplayEventCRFBean dec = new DisplayEventCRFBean();
            dec.setEventCRF(eventCRF);
            dec.setFlags(eventCRF, ub, currentRole, edc.isDoubleEntry());

            // find all item data
            ItemDataDAO iddao = new ItemDataDAO(sm.getDataSource());

            ArrayList itemData = iddao.findAllByEventCRFId(eventCRF.getId());

            request.setAttribute("items", itemData);
            String action = request.getParameter("action");

            if (getEventCrfLocker().isLocked(currentPublicStudy.getSchemaName()
                    + eventCRF.getStudyEventId() + eventCRF.getFormLayoutId(), ub.getId(), request.getSession().getId())) {

                LockInfo lockInfo = getEventCrfLocker().getLockOwner(currentPublicStudy.getSchemaName()
                        + eventCRF.getStudyEventId() + eventCRF.getFormLayoutId());
                UserAccountDAO uDAO = new UserAccountDAO(sm.getDataSource());
                UserAccountBean userAccountBean = (UserAccountBean) uDAO.findByPK(lockInfo.getUserId());
                request.setAttribute("errorData", "This form is currently unavailable for this action.\\n " +
                        "User " + userAccountBean.getName() +" is currently entering data.\\n " +
                        resword.getString("CRF_perform_action") +"\\n");
                if ("confirm".equalsIgnoreCase(action)) {
                    request.setAttribute("id", new Integer(studySubId).toString());
                    forwardPage(Page.VIEW_STUDY_SUBJECT_SERVLET);
                    return;
                } else {
                    request.setAttribute("displayEventCRF", dec);
                    forwardPage(Page.REMOVE_EVENT_CRF);
                    return;
                }
            }
            if ("confirm".equalsIgnoreCase(action)) {

                request.setAttribute("displayEventCRF", dec);

                forwardPage(Page.REMOVE_EVENT_CRF);
            } else {
                logger.info("submit to remove the event CRF from study");

                eventCRF.setRemoved(Boolean.TRUE);
                eventCRF.setUpdater(ub);
                eventCRF.setUpdatedDate(new Date());
                eventCRFDAO.update(eventCRF);

                if (event.isSigned()) {
                    event.setSigned(Boolean.FALSE);
                    event.setUpdater(ub);
                    event.setUpdatedDate(new Date());
                    studyEventDAO.update(event);
                }
                if(studySub.getStatus().equals(Status.SIGNED)){
                    studySub.setStatus(Status.AVAILABLE);
                    studySub.setUpdater(ub);
                    studySub.setUpdatedDate(new Date());
                    subdao.update(studySub);
                }

                // remove all the item data
                for (int a = 0; a < itemData.size(); a++) {
                    ItemDataBean item = (ItemDataBean) itemData.get(a);

                        DiscrepancyNoteDAO dnDao = new DiscrepancyNoteDAO(sm.getDataSource());
                        List dnNotesOfRemovedItem = dnDao.findParentNotesOnlyByItemData(item.getId());
                        if (!dnNotesOfRemovedItem.isEmpty()) {
                            DiscrepancyNoteBean itemParentNote = null;
                            for (Object obj : dnNotesOfRemovedItem) {
                                if (((DiscrepancyNoteBean) obj).getParentDnId() == 0) {
                                    itemParentNote = (DiscrepancyNoteBean) obj;
                                }
                            }
                            DiscrepancyNoteBean dnb = new DiscrepancyNoteBean();
                            if (itemParentNote != null) {
                                dnb.setParentDnId(itemParentNote.getId());
                                dnb.setDiscrepancyNoteTypeId(itemParentNote.getDiscrepancyNoteTypeId());
                                dnb.setThreadUuid(itemParentNote.getThreadUuid());
                            }
                            dnb.setResolutionStatusId(ResolutionStatus.CLOSED_MODIFIED.getId()); // set to closed-modified
                            dnb.setStudyId(currentStudy.getStudyId());
                            dnb.setAssignedUserId(ub.getId());
                            dnb.setOwner(ub);
                            dnb.setEntityType(DiscrepancyNoteBean.ITEM_DATA);
                            dnb.setEntityId(item.getId());
                            dnb.setColumn("value");
                            dnb.setCreatedDate(new Date());
                            String detailedNotes="The item has been removed, this Query has been Closed.";
                            dnb.setDetailedNotes(detailedNotes);
                            dnDao.create(dnb);
                            dnDao.createMapping(dnb);
                            itemParentNote.setResolutionStatusId(ResolutionStatus.CLOSED_MODIFIED.getId());  // set to closed-modified
                            itemParentNote.setDetailedNotes(detailedNotes);
                            dnDao.update(itemParentNote);
                        }

                }

                boolean isRequiredCrf = false;
                for (EventDefinitionCrf defCrf: ec.getStudyEvent().getStudyEventDefinition().getEventDefinitionCrfs()) {
                    if (defCrf.getCrf().getOcOid().equals(ec.getFormLayout().getCrf().getOcOid())) {
                        isRequiredCrf = defCrf.getRequiredCrf();
                        break;
                    }
                }
                if (isRequiredCrf && ec.getStudyEvent().getWorkflowStatus() == StudyEventWorkflowStatusEnum.COMPLETED) {
                    ec.getStudyEvent().setWorkflowStatus(StudyEventWorkflowStatusEnum.DATA_ENTRY_STARTED);
                }

                request.setAttribute("id", new Integer(studySubId).toString());
                forwardPage(Page.VIEW_STUDY_SUBJECT_SERVLET);
            }
        }
    }

}
