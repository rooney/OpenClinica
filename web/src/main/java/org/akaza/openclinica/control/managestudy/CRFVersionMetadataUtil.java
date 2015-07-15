package org.akaza.openclinica.control.managestudy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.akaza.openclinica.bean.submit.CRFVersionBean;
import org.akaza.openclinica.bean.submit.DisplayItemBean;
import org.akaza.openclinica.bean.submit.DisplaySectionBean;
import org.akaza.openclinica.bean.submit.ItemBean;
import org.akaza.openclinica.bean.submit.ItemFormMetadataBean;
import org.akaza.openclinica.bean.submit.ItemGroupBean;
import org.akaza.openclinica.bean.submit.ItemGroupMetadataBean;
import org.akaza.openclinica.bean.submit.ResponseOptionBean;
import org.akaza.openclinica.bean.submit.SectionBean;
import org.akaza.openclinica.dao.hibernate.SCDItemMetadataDao;
import org.akaza.openclinica.dao.submit.ItemDAO;
import org.akaza.openclinica.dao.submit.ItemFormMetadataDAO;
import org.akaza.openclinica.dao.submit.ItemGroupDAO;
import org.akaza.openclinica.dao.submit.ItemGroupMetadataDAO;
import org.akaza.openclinica.dao.submit.SectionDAO;
import org.akaza.openclinica.domain.crfdata.SCDItemMetadataBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class with method for retrieving the metadata for a CRFVersion.
 */
public class CRFVersionMetadataUtil {

	private DataSource dataSource = null;
    private SCDItemMetadataDao scdItemMetadataDao;

    private static final String TYPE1 = "ViewCRFVersionServlet";
    private static final String TYPE2 = "OpenRosaXmlGenerator";

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	public CRFVersionMetadataUtil(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}
	/**
	 * Builds and returns an ArrayList of SectionBeans that comprise the metadata of a CRFVersion.
	 */
    public ArrayList retrieveFormMetadata(CRFVersionBean version, String type, SCDItemMetadataDao scdItemMetadataDao) throws Exception {

        ItemDAO idao = new ItemDAO(dataSource);
        ItemFormMetadataDAO ifmdao = new ItemFormMetadataDAO(dataSource);

        // tbh, 102007
        SectionDAO sdao = new SectionDAO(dataSource);
        ItemGroupDAO igdao = new ItemGroupDAO(dataSource);
        ItemGroupMetadataDAO igmdao = new ItemGroupMetadataDAO(dataSource);
        ArrayList<SectionBean> sections = (ArrayList<SectionBean>) sdao.findByVersionId(version.getId());
        HashMap versionMap = new HashMap();
        for (SectionBean section : sections) {
            versionMap.put(new Integer(section.getId()), section.getItems());
            // YW 08-21-2007, add group metadata
            ArrayList<ItemGroupBean> igs = (ArrayList<ItemGroupBean>) igdao.findGroupBySectionId(section.getId());
            for (ItemGroupBean igb : igs) {
                ArrayList<ItemGroupMetadataBean> igms =
                    (ArrayList<ItemGroupMetadataBean>) igmdao.findMetaByGroupAndSection(igb.getId(), section.getCRFVersionId(), section.getId());
                if (!igms.isEmpty()) {
                    // Note, the following logic has been adapted here -
                    // "for a given crf version,
                    // all the items in the same group have the same group
                    // metadata
                    // so we can get one of them and set metadata for the
                    // group"
                    igb.setMeta(igms.get(0));
                    igb.setItemGroupMetaBeans(igms);
                }
            }
            ((SectionBean) section).setGroups(igs);
            // YW >>
        }
        ArrayList<ItemBean> items = idao.findAllItemsByVersionId(version.getId());
        // YW 08-22-2007, if this crf_version_id doesn't exist in
        // item_group_metadata table,
        // items in this crf_version will not exist in item_group_metadata,
        // then different query will be used
        if (igmdao.versionIncluded(version.getId())) {
            for (ItemBean item : items) {
                ItemFormMetadataBean ifm = ifmdao.findByItemIdAndCRFVersionId(item.getId(), version.getId());

                item.setItemMeta(ifm);
                // logger.info("option******" +
                // ifm.getResponseSet().getOptions().size());
                // if (new Integer(ifm.getSectionId()) != 0) {
                    ArrayList its = (ArrayList) versionMap.get(new Integer(ifm.getSectionId()));
                    its.add(item);
                // }
            }
        } else {
            for (ItemBean item : items) {
                ItemFormMetadataBean ifm = ifmdao.findByItemIdAndCRFVersionIdNotInIGM(item.getId(), version.getId());

                item.setItemMeta(ifm);
                // logger.info("option******" +
                // ifm.getResponseSet().getOptions().size());
                ArrayList its = (ArrayList) versionMap.get(new Integer(ifm.getSectionId()));
                its.add(item);
            }
        }

        ArrayList ret = new ArrayList();
        switch(type) {
            case TYPE1 :
                ArrayList<DisplaySectionBean> displaySectionBeanList = new ArrayList<DisplaySectionBean>();
                for (SectionBean section : sections) {
                    ArrayList<DisplayItemBean> displayItemBeanList = new ArrayList<DisplayItemBean>();
                    DisplaySectionBean displaySectionBean = new DisplaySectionBean();
                    for (ItemBean itemBean : (ArrayList<ItemBean>) versionMap.get(new Integer(section.getId()))) {
                        DisplayItemBean displayItemBean = new DisplayItemBean();
                        displayItemBean.setItem(itemBean);
                        displayItemBean.setMetadata(itemBean.getItemMeta());
                        displayItemBeanList.add(displayItemBean);
                    }
                    displaySectionBean.setItems(displayItemBeanList);
                    displaySectionBean.setSection(section);
                    initConditionalDisplays(displaySectionBean, scdItemMetadataDao);
                    displaySectionBeanList.add(displaySectionBean);
                }
                ret = displaySectionBeanList;
                break;
            case TYPE2 :
                for (SectionBean section : sections) {
                    section.setItems((ArrayList) versionMap.get(new Integer(section.getId())));
                }
                ret = sections;
                break;
        }
        return ret;
    }

    public DisplaySectionBean initConditionalDisplays(DisplaySectionBean displaySection, SCDItemMetadataDao scdItemMetadataDao) {
        int sectionId = displaySection.getSection().getId();
        Set<Integer> showSCDItemIds = displaySection.getShowSCDItemIds();
        // scdItemMetadataDao = (SCDItemMetadataDao) context().getBean("scdItemMetadataDao");
        // (SCDItemMetadataDao) getContext().getBean("scdItemMetadataDao");
        // scdItemMetadataDao = (SCDItemMetadataDao) SpringServletAccess.getApplicationContext(context).getBean("scdItemMetadataDao");
        // scdItemMetadataDao = new SCDItemMetadataDao();
        ArrayList<SCDItemMetadataBean> cds = scdItemMetadataDao.findAllBySectionId(sectionId);
        if (cds == null) {
            logger.info("SCDItemMetadataDao.findAllBySectionId with sectionId=" + sectionId + " returned null.");
        } else if (cds.size() > 0) {
            ArrayList<DisplayItemBean> displayItems = initSCDItems(displaySection.getItems(), cds, showSCDItemIds);
            HashMap<Integer, ArrayList<SCDItemMetadataBean>> scdPairMap = getControlMetaIdAndSCDSetMap(sectionId, cds);
            if (scdPairMap == null) {
                logger.info("CRFVersionMetadataUtil.getControlMetaIdAndSCDSetMap returned null.");
            } else {
                for (DisplayItemBean displayItem : displayItems) {
                    if (scdPairMap.containsKey(displayItem.getMetadata().getId())) {
                        // displayItem is control item
                        ArrayList<SCDItemMetadataBean> sets = scdPairMap.get(displayItem.getMetadata().getId());
                        displayItem.getScdData().setScdSetsForControl(sets);
                        for (SCDItemMetadataBean scd : sets) {
                            if (initConditionalDisplayToBeShown(displayItem, scd)) {
                                showSCDItemIds.add(scd.getScdItemId());
                            }
                        }
                    }
                    // control item is ahead of its scd item(s)
                    if (displayItem.getScdData().getScdItemMetadataBean().getScdItemFormMetadataId() > 0) {
                        // displayItem is scd item
                        displayItem.setIsSCDtoBeShown(showSCDItemIds.contains(displayItem.getMetadata().getItemId()));
                    }

                    if (displayItem.getChildren().size() > 0) {
                        ArrayList<DisplayItemBean> cs = displayItem.getChildren();
                        for (DisplayItemBean c : cs) {
                            if (scdPairMap.containsKey(c.getMetadata().getId())) {
                                // c is control item
                                ArrayList<SCDItemMetadataBean> sets = scdPairMap.get(c.getMetadata().getId());
                                c.getScdData().setScdSetsForControl(sets);
                                for (SCDItemMetadataBean scd : sets) {
                                    if (initConditionalDisplayToBeShown(c, scd)) {
                                        showSCDItemIds.add(scd.getScdItemId());
                                    }
                                }
                            }
                            // control item is ahead of its scd item(s)
                            if (c.getScdData().getScdItemMetadataBean().getScdItemFormMetadataId() > 0) {
                                // c is scd item
                                c.setIsSCDtoBeShown(showSCDItemIds.contains(c.getMetadata().getItemId()));
                            }
                        }
                    }
                }
            }
        }
        return displaySection;
    }

    public ArrayList<DisplayItemBean> initSCDItems(ArrayList<DisplayItemBean> displayItems,
            ArrayList<SCDItemMetadataBean> cds, Set<Integer> showSCDItemIds) {
        ArrayList<DisplayItemBean> dis = displayItems;
        HashMap<Integer, SCDItemMetadataBean> scds = (HashMap<Integer, SCDItemMetadataBean>) getSCDMetaIdAndSCDSetMap(cds);
        for (DisplayItemBean displayItem : dis) {
            ItemFormMetadataBean meta = displayItem.getMetadata();
            if (scds.containsKey(meta.getId())) {
                SCDItemMetadataBean scdItemMetadataBean = scds.get(meta.getId());
                scdItemMetadataBean.setScdItemId(meta.getItemId());
                displayItem.getScdData().setScdItemMetadataBean(scdItemMetadataBean);
            }
            if (meta.getParentId() < 1) {
                ArrayList<DisplayItemBean> cs = displayItem.getChildren();
                for (DisplayItemBean c : cs) {
                    ItemFormMetadataBean cmeta = c.getMetadata();
                    if (scds.containsKey(cmeta.getId())) {
                        SCDItemMetadataBean scdItemMetadataBean = scds.get(cmeta.getId());
                        scdItemMetadataBean.setScdItemId(cmeta.getItemId());
                        c.getScdData().setScdItemMetadataBean(scdItemMetadataBean);
                    }
                }
            }
        }
        return dis;
    }

    public Map<Integer, SCDItemMetadataBean> getSCDMetaIdAndSCDSetMap(ArrayList<SCDItemMetadataBean> scdSets) {
        Map<Integer, SCDItemMetadataBean> map = new HashMap<Integer, SCDItemMetadataBean>();
        if (scdSets == null) {
            logger.info("CRFVersionMetadataUtil.getSCDMetaIdAndSCDSetMap() ArrayList<SCDItemMetadataBean> parameter is null.");
        } else {
            for (SCDItemMetadataBean scd : scdSets) {
                map.put(scd.getScdItemFormMetadataId(), scd);
            }
        }
        return map;
    }

    public HashMap<Integer, ArrayList<SCDItemMetadataBean>> getControlMetaIdAndSCDSetMap(int sectionId,
            ArrayList<SCDItemMetadataBean> scdSets) {
        HashMap<Integer, ArrayList<SCDItemMetadataBean>> cdPairMap = new HashMap<Integer, ArrayList<SCDItemMetadataBean>>();
        if (scdSets == null) {
            logger.info("CRFVersionMetadataUtil.getControlMetaIdAndSCDSetMap() ArrayList<SCDItemMetadataBean> parameter is null.");
        } else {
            for (SCDItemMetadataBean scd : scdSets) {
                Integer conId = scd.getControlItemFormMetadataId();
                ArrayList<SCDItemMetadataBean> conScds = cdPairMap.containsKey(conId)
                        ? cdPairMap.get(conId)
                        : new ArrayList<SCDItemMetadataBean>();
                conScds.add(scd);
                cdPairMap.put(conId, conScds);
            }
        }
        return cdPairMap;
    }

    public boolean initConditionalDisplayToBeShown(DisplayItemBean controlItem, SCDItemMetadataBean cd) {
        String chosenOption = controlItem.getData().getValue();
        if (chosenOption != null && chosenOption.length() > 0) {
            if (chosenOption.equals(cd.getOptionValue())) {
                return true;
            }
        } else {
            chosenOption = controlItem.getMetadata().getDefaultValue();
            if (chosenOption != null && chosenOption.length() > 0) {
                if (chosenOption.equals(cd.getOptionValue())) {
                    return true;
                }
            } else {
                if (controlItem.getMetadata().getResponseSet().getResponseTypeId() == 6) {
                    // single-select
                    chosenOption = ((ResponseOptionBean) controlItem.getMetadata().getResponseSet().getOptions().get(0))
                            .getValue();
                    if (chosenOption != null && chosenOption.length() > 0) {
                        if (chosenOption.equals(cd.getOptionValue())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
