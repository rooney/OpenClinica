package org.akaza.openclinica.view.tags;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.akaza.openclinica.bean.managestudy.StudyBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import org.akaza.openclinica.i18n.core.LocaleResolver;

/**
 * Tag that will generate icon for calendar.
 */
@SuppressWarnings("serial")
public class CalendarIconTag extends TagSupport {

	private static final Logger LOGGER = LoggerFactory.getLogger(CalendarIconTag.class);

	private String alt = "";
	private String title = "";
	private String dateFormat = "";
	private String linkName = "";
	private String linkId = "";
	private String onClickSelector;
	private String imageId = "";
	private boolean checkIfShowYear = false;

	private void reset() {
		alt = "";
		title = "";
		linkId = "";
		imageId = "";
		linkName = "";
		dateFormat = "";
		onClickSelector = null;
		checkIfShowYear = false;
	}

	@Override
	public int doStartTag() throws JspException {

		if (onClickSelector != null) {
			WebApplicationContext webApplicationContext = WebApplicationContextUtils
					.getRequiredWebApplicationContext(((PageContext) pageContext.getAttribute(PageContext.PAGECONTEXT))
							.getServletContext());
			StudyBean currentStudy = (StudyBean) pageContext.getSession().getAttribute("study");
			MessageSource messageSource = (MessageSource) webApplicationContext.getBean("messageSource");
			Locale locale = LocaleResolver.getLocale((HttpServletRequest) pageContext.getRequest());

			alt = alt.isEmpty() ? messageSource.getMessage("show_calendar", null, locale) : alt;
			title = title.isEmpty() ? messageSource.getMessage("show_calendar", null, locale) : title;
			dateFormat = dateFormat.isEmpty()
					? messageSource.getMessage("date_format_calender", null, locale)
					: dateFormat;

			String html = "";

			html = html.concat("<a href='#!' onclick=\"$(" + onClickSelector + ")")
					.concat(".datepicker({ dateFormat: '" + dateFormat + "', ").concat("showOn: 'none'");

			boolean showYears = !checkIfShowYear
					|| currentStudy.getStudyParameterConfig().getShowYearsInCalendar().equalsIgnoreCase("yes");

			if (showYears) {
				html = html.concat(", changeYear: true,changeMonth : true,\n").concat(
						"onChangeMonthYear: function (year, month, inst) {\n" + "\t var date = $(this).val();\n"
								+ "\t if ($.trim(date) != '') {\n"
								+ "\t\t var newDate = month + '/' + inst.currentDay + '/' + year;\n"
								+ "\t\t $(this).val($.datepicker.formatDate('" + dateFormat
								+ "', new Date(newDate)));\n" + "\t}\n},\n yearRange: 'c-20:c+10'");
			}
			html = html.concat("}).datepicker('show');\" ");
			if (!linkId.isEmpty()) {
				html = html.concat("id='" + linkId + "' ");
			}
			if (!linkName.isEmpty()) {
				html = html.concat("name='" + linkName + "'");
			}
			html = html.concat(">\n \t<img src='images/bt_Calendar.gif' alt='" + alt + "' ").concat(
					"title='" + title + "' border='0'");
			if (!imageId.isEmpty()) {
				html = html.concat("id='" + imageId + "'");
			}
			html = html.concat("/>\n" + "</a>");

			JspWriter writer = pageContext.getOut();
			try {
				writer.write(html);
			} catch (Exception ex) {
				LOGGER.error("Error has occurred.", ex);
			}
		}
		reset();
		return SKIP_BODY;
	}

	public String getAlt() {
		return alt;
	}

	public void setAlt(String alt) {
		this.alt = alt;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public String getLinkName() {
		return linkName;
	}

	public void setLinkName(String linkName) {
		this.linkName = linkName;
	}

	public String getLinkId() {
		return linkId;
	}

	public void setLinkId(String linkId) {
		this.linkId = linkId;
	}

	public String getOnClickSelector() {
		return onClickSelector;
	}

	public void setOnClickSelector(String onClickSelector) {
		this.onClickSelector = onClickSelector;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public boolean getCheckIfShowYear() {
		return checkIfShowYear;
	}

	public void setCheckIfShowYear(boolean checkIfShowYear) {
		this.checkIfShowYear = checkIfShowYear;
	}

	@Override
	public void release() {
		reset();
		super.release();
	}
}
