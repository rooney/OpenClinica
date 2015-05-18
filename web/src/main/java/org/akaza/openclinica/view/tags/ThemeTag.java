package org.akaza.openclinica.view.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom tag for adding hide stuff class.
 */
@SuppressWarnings({"serial"})
public class ThemeTag extends TagSupport {

	private static final Logger LOGGER = LoggerFactory.getLogger(ThemeTag.class);

	@Override
	public int doStartTag() throws JspException {
		String newThemeColor = (String) pageContext.getSession().getAttribute("newThemeColor");
		newThemeColor = newThemeColor == null || newThemeColor.trim().isEmpty() ? "blue" : newThemeColor;
		String html = "<script type=\"text/JavaScript\" language=\"JavaScript\" src=\""
				.concat(pageContext.getServletContext().getContextPath()).concat("/includes/theme.js\"></script>");
		if (!newThemeColor.equalsIgnoreCase("blue")) {
			html = html.concat("<style class=\"hideStuff\" type=\"text/css\">body {visibility:hidden;}</style>");
		}
		html = html.concat("<link rel=\"stylesheet\" href=\"").concat(pageContext.getServletContext().getContextPath())
				.concat("/includes/css/charts_").concat(newThemeColor).concat(".css\" type=\"text/css\"/>");
		html = html.concat("<link rel=\"stylesheet\" href=\"").concat(pageContext.getServletContext().getContextPath())
				.concat("/includes/css/styles_").concat(newThemeColor).concat(".css\" type=\"text/css\"/>");
		JspWriter writer = pageContext.getOut();
		try {
			writer.write(html);
		} catch (Exception ex) {
			LOGGER.error("Error has occurred.", ex);
		}
		return SKIP_BODY;
	}
}