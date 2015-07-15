package org.akaza.openclinica.controller;

import java.util.HashMap;
import java.util.Map;

import org.akaza.openclinica.bean.login.UserAccountBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.dbcp.BasicDataSource;
import org.akaza.openclinica.control.form.FormProcessor;
import org.akaza.openclinica.util.ItemSDVServiceImpl;
import org.akaza.openclinica.util.CrfShortcutsAnalyzer;

/**
 * SDVItemController.
 */
@Controller
@RequestMapping("/sdvItem")
public class SDVItemController {

	public static final String ACTION = "action";
	public static final String SECTION_ID = "sectionId";
	public static final String ITEM_DATA_ID = "itemDataId";
	public static final String EVENT_DEFINITION_CRF_ID = "eventDefinitionCrfId";
	public static final String USER_BEAN_NAME = "userBean";
	public static final String DOMAIN_NAME = "domain_name";

	private ItemSDVServiceImpl itemSDVServiceImpl;

	@Autowired
	@Qualifier("dataSource")
	private BasicDataSource dataSource;
	/**
	 * Main http get method.
	 * 
	 * @param request
	 *            HttpServletRequest
	 * @return String JSON object
	 * @throws Exception
	 *             an Exception
	 */
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public String mainGet(HttpServletRequest request) throws Exception {
		String action = request.getParameter(ACTION);
		int sectionId = Integer.parseInt(request.getParameter(SECTION_ID));
		int itemDataId = Integer.parseInt(request.getParameter(ITEM_DATA_ID));
		int eventDefinitionCrfId = Integer.parseInt(request.getParameter(EVENT_DEFINITION_CRF_ID));

		UserAccountBean userAccountBean = (UserAccountBean) request.getSession().getAttribute(USER_BEAN_NAME);
		itemSDVServiceImpl = new ItemSDVServiceImpl(dataSource);
		CrfShortcutsAnalyzer crfShortcutsAnalyzer = getCrfShortcutsAnalyzer(request, itemSDVServiceImpl, false);

		String temp = itemSDVServiceImpl.sdvItem(itemDataId, sectionId, eventDefinitionCrfId, action, userAccountBean,
				crfShortcutsAnalyzer);
		return temp;
	}

	private CrfShortcutsAnalyzer getCrfShortcutsAnalyzer(HttpServletRequest request,
            ItemSDVServiceImpl itemSDVService, boolean recreate) {
        CrfShortcutsAnalyzer crfShortcutsAnalyzer = (CrfShortcutsAnalyzer) request
                .getAttribute(CrfShortcutsAnalyzer.CRF_SHORTCUTS_ANALYZER);
        if (crfShortcutsAnalyzer == null || recreate) {
            FormProcessor fp = new FormProcessor(request);
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put(CrfShortcutsAnalyzer.EXIT_TO, fp.getString(CrfShortcutsAnalyzer.EXIT_TO, true));
            attributes.put(CrfShortcutsAnalyzer.CW, fp.getRequest().getParameter(CrfShortcutsAnalyzer.CW));
            attributes.put(CrfShortcutsAnalyzer.SECTION_ID, fp.getInt(CrfShortcutsAnalyzer.SECTION_ID, true));
            attributes.put(CrfShortcutsAnalyzer.SECTION, fp.getRequest().getAttribute(CrfShortcutsAnalyzer.SECTION));
            attributes.put(CrfShortcutsAnalyzer.USER_ROLE,
                    request.getSession().getAttribute(CrfShortcutsAnalyzer.USER_ROLE));
            attributes.put(CrfShortcutsAnalyzer.SERVLET_PATH, fp.getString(CrfShortcutsAnalyzer.SERVLET_PATH).isEmpty()
                    ? fp.getRequest().getServletPath()
                    : fp.getString(CrfShortcutsAnalyzer.SERVLET_PATH));

            crfShortcutsAnalyzer = new CrfShortcutsAnalyzer(request.getScheme(), request.getMethod(),
                    request.getRequestURI(), request.getServletPath(), (String) request.getSession().getAttribute(
                            DOMAIN_NAME), attributes, itemSDVService);

            crfShortcutsAnalyzer.getInterviewerDisplayItemBean().setField(CrfShortcutsAnalyzer.INTERVIEWER_NAME);
            crfShortcutsAnalyzer.getInterviewDateDisplayItemBean().setField(CrfShortcutsAnalyzer.DATE_INTERVIEWED);

            request.setAttribute(CrfShortcutsAnalyzer.CRF_SHORTCUTS_ANALYZER, crfShortcutsAnalyzer);
        }
        return crfShortcutsAnalyzer;
    }
}