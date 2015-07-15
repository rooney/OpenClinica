package org.akaza.openclinica.view.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.akaza.openclinica.dao.core.CoreResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.akaza.openclinica.i18n.core.LocaleResolver;

/**
 * Custom tag for adding calendar js scripts.
 */
@SuppressWarnings({"serial"})
public class CalendarTag extends TagSupport {

	private static final Logger LOGGER = LoggerFactory.getLogger(CalendarTag.class);

	@Override
	public int doStartTag() throws JspException {
		String locale = LocaleResolver.getLocale((HttpServletRequest) pageContext.getRequest()).toString();
		String language = LocaleResolver.getLocale((HttpServletRequest) pageContext.getRequest()).getLanguage();
		locale = CoreResources.CALENDAR_LOCALES.contains(locale) ? locale : (CoreResources.CALENDAR_LOCALES
				.contains(language) ? language : "");
		String contextPath = pageContext.getServletContext().getContextPath();
		String html = "";
		if (locale.isEmpty())
			locale = "en";

		if (!locale.isEmpty()) {
			html = html.concat("<script type=\"text/javascript\" src=\"").concat(contextPath)
					.concat("/includes/calendar/locales/datepicker-").concat(locale).concat(".js\"></script>");
		}

		String color = (String) pageContext.getSession().getAttribute("newThemeColor");
		color = color == null ? "blue" : color;
		html = html.concat("<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"").concat(contextPath);
		if (color.equals("violet")) {
			html = html.concat("/includes/calendar/css/calendar_violet.css\"/>");
		} else if (color.equals("green")) {
			html = html.concat("/includes/calendar/css/calendar_green.css\"/>");
		} else {
			html = html.concat("/includes/calendar/css/calendar_blue.css\"/>");
		}

		JspWriter writer = pageContext.getOut();
		try {
			writer.write(html);
		} catch (Exception ex) {
			LOGGER.error("Error has occurred.", ex);
		}
		return SKIP_BODY;
	}
}