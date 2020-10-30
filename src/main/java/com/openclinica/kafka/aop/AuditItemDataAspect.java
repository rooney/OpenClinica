package com.openclinica.kafka.aop;

import com.openclinica.kafka.KafkaService;
import core.org.akaza.openclinica.bean.managestudy.StudyEventBean;
import core.org.akaza.openclinica.bean.submit.ItemDataBean;
import core.org.akaza.openclinica.dao.hibernate.EventCrfDao;
import core.org.akaza.openclinica.dao.hibernate.ItemDataDao;
import core.org.akaza.openclinica.dao.hibernate.StudySubjectDao;
import core.org.akaza.openclinica.domain.datamap.EventCrf;
import core.org.akaza.openclinica.domain.datamap.ItemData;
import core.org.akaza.openclinica.domain.datamap.StudyEvent;
import core.org.akaza.openclinica.domain.datamap.StudySubject;
import core.org.akaza.openclinica.service.managestudy.StudySubjectService;
import org.akaza.openclinica.controller.openrosa.SubmissionContainer;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditItemDataAspect {
    protected final Logger log = LoggerFactory.getLogger(getClass().getName());
    private KafkaService kafkaService;
    private StudySubjectService studySubjectService;
    private EventCrfDao eventCrfDao;
    private ItemDataDao itemDataDao;

    public AuditItemDataAspect(KafkaService kafkaService, StudySubjectService studySubjectService, EventCrfDao eventCrfDao, ItemDataDao itemDataDao){
        this.kafkaService = kafkaService;
        this.studySubjectService = studySubjectService;
        this.eventCrfDao = eventCrfDao;
        this.itemDataDao = itemDataDao;
    }

    @AfterReturning("execution(* core.org.akaza.openclinica.service.ItemDataService.saveOrUpdate(..))")
    public void onItemDataDaoSaveOrUpdate(JoinPoint joinPoint) {
        log.info("AoP: onItemDataDaoSaveOrUpdate triggered");
        try {
            ItemData itemData = (ItemData) joinPoint.getArgs()[0];
            kafkaService.sendItemDataChangeMessage(itemData);
            updateStudySubjectLastModifiedDetails(itemData);
        } catch (Exception e) {
            log.error("Could not send kafka message triggered by ItemDataDao.auditedSaveOrUpdate: ", e);
        }
    }

    // Determine if we need to cover these ItemDataDAOs...
    @AfterReturning("execution(* core.org.akaza.openclinica.dao.submit.ItemDataDAO.create(..))")
    public void onItemDataDAOCreate(JoinPoint joinPoint) {
        ItemDataBean itemDataBean = (ItemDataBean) joinPoint.getArgs()[0];
        log.info("AoP: onItemDataDAOCreate triggered");
        try {
            kafkaService.sendItemDataChangeMessage(itemDataBean);
            updateStudySubjectLastModifiedDetails(itemDataBean);
        } catch (Exception e) {
            log.error("Could not send kafka message triggered by ItemDataDAO.create: ", e);
        }
    }

    @AfterReturning("execution(* core.org.akaza.openclinica.dao.submit.ItemDataDAO.update(..))")
    public void onItemDataDAOUpdate(JoinPoint joinPoint) {
        ItemDataBean itemDataBean = (ItemDataBean) joinPoint.getArgs()[0];
        log.info("AoP: onItemDataDAOUpdate triggered");
        try {
            kafkaService.sendItemDataChangeMessage(itemDataBean);
            updateStudySubjectLastModifiedDetails(itemDataBean);
        } catch (Exception e) {
            log.error("Could not send kafka message triggered by ItemDataDAO.update: ", e);
        }
    }

    @AfterReturning("execution(* core.org.akaza.openclinica.dao.submit.ItemDataDAO.delete(..))")
    public void onItemDataDAODelete(JoinPoint joinPoint) {
        ItemDataBean itemDataBean = (ItemDataBean) joinPoint.getArgs()[0];
        log.info("AoP: onItemDataDAODelete triggered");
        try {
            kafkaService.sendItemDataChangeMessage(itemDataBean);
            updateStudySubjectLastModifiedDetails(itemDataBean);
        } catch (Exception e) {
            log.error("Could not send kafka message triggered by ItemDataDAO.delete: ", e);
        }
    }

    //If discrepancy note created for new itemData, the itemData is created with empty string
    private Boolean isEmptyItemDataCreated(Integer itemDataId,String value) {
        if(StringUtils.isEmpty(value)) {
            if (itemDataId == null || itemDataId == 0)
                return true;
            Session session = itemDataDao.getSessionFactory().openSession();
            ItemData existingItemData = session.find(ItemData.class, itemDataId);
            session.close();
            if(existingItemData == null)
                return true;
        }
        return false;
    }

    private void updateStudySubjectLastModifiedDetails(ItemData itemData) {
        if(!isEmptyItemDataCreated(itemData.getItemDataId(), itemData.getValue())) {
            StudySubject studySubject = itemData.getEventCrf().getStudySubject();
            if (itemData.getUpdateId() != null && itemData.getUpdateId() > 0)
                studySubjectService.updateStudySubject(studySubject, itemData.getUpdateId());
            else
                studySubjectService.updateStudySubject(studySubject, itemData.getUserAccount().getUserId());
        }
    }

    private void updateStudySubjectLastModifiedDetails(ItemDataBean itemDataBean){
        if(!isEmptyItemDataCreated(itemDataBean.getId(), itemDataBean.getValue())) {
            EventCrf eventCrf = eventCrfDao.findById(itemDataBean.getEventCRFId());
            if (itemDataBean.getUpdater() != null && itemDataBean.getUpdater().getId() > 0)
                studySubjectService.updateStudySubject(eventCrf.getStudySubject(), itemDataBean.getUpdater().getId());
            else
                studySubjectService.updateStudySubject(eventCrf.getStudySubject(), itemDataBean.getOwnerId());
        }
    }

}
