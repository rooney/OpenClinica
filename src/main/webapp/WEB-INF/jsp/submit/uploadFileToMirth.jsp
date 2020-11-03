<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="core.org.akaza.openclinica.logic.importdata.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="java.net.URLEncoder" %>

<fmt:setBundle basename="org.akaza.openclinica.i18n.notes" var="restext"/>
<fmt:setBundle basename="org.akaza.openclinica.i18n.words" var="resword"/>
<fmt:setBundle basename="org.akaza.openclinica.i18n.workflow" var="resworkflow"/>
<fmt:setBundle basename="org.akaza.openclinica.i18n.terms" var="resterms"/>
<c:choose>
<c:when test="${userBean.sysAdmin && module=='admin'}">
 <c:import url="../include/admin-header.jsp"/>
</c:when>
<c:otherwise>
 <c:import url="../include/submit-header.jsp"/>
</c:otherwise>
</c:choose>


<!-- move the alert message to the sidebar-->
<jsp:include page="../include/sideAlert.jsp"/>

<!-- then instructions-->
<tr id="sidebar_Instructions_open" style="display: all">
	<td class="sidebar_tab">
		<a href="javascript:leftnavExpand('sidebar_Instructions_open'); leftnavExpand('sidebar_Instructions_closed');">
			<span class="icon icon-caret-down gray"></span>
		</a>
		<fmt:message key="instructions" bundle="${restext}"/>
		<div class="sidebar_tab_content">
			<fmt:message key="upload_side_bar_instructions" bundle="${restext}"/>
		</div>
	</td>
</tr>

<tr id="sidebar_Instructions_closed" style="display: none">
	<td class="sidebar_tab">
		<a href="javascript:leftnavExpand('sidebar_Instructions_open'); leftnavExpand('sidebar_Instructions_closed');">
			<span class="icon icon-caret-right gray"></span>
		</a>
		<fmt:message key="instructions" bundle="${restext}"/>
	</td>
</tr>



<jsp:include page="../include/sideInfo.jsp"/>


<jsp:useBean scope='session' id='version' class='core.org.akaza.openclinica.bean.submit.CRFVersionBean'/>
<jsp:useBean scope='session' id='userBean' class='core.org.akaza.openclinica.bean.login.UserAccountBean'/>
<jsp:useBean scope='session' id='crfName' class='java.lang.String'/>

 <c:out value="${crfName}"/>

<c:choose>
	<c:when test="${userBean.sysAdmin && module=='admin'}">
		<h1><span class="title_manage">
	</c:when>
	<c:otherwise>
		<h1>
		<span class="title_submit">
	</c:otherwise>
</c:choose>

<fmt:message key="import_tabular_crf_data" bundle="${resworkflow}"/>
<a href="javascript:openDocWindow('https://docs.openclinica.com/3.1/openclinica-user-guide/submit-data-module-overview/import-data')">
    <span class=""></span>
</a></h1>
<p><fmt:message key="upload_instructions" bundle="${restext}"/></p>



<form action="UploadCRFData?action=confirm&crfId=<c:out value="${version.crfId}"/>&name=<c:out value="${version.name}"/>" method="post" ENCTYPE="multipart/form-data">
<div style="width: 400px">

<div class="box_T"><div class="box_L"><div class="box_R"><div class="box_B"><div class="box_TL"><div class="box_TR"><div class="box_BL"><div class="box_BR">
<div class="textbox_center">
<table border="0" cellpadding="0" cellspacing="0">

<tr>
	<td class="formlabel"><!--<fmt:message key="xml_file_to_upload" bundle="${resterms}"/>:--></td>
	<td>
		<div class="formfieldFile_BG">
			<input type="file" name="xml_file" multiple="multiple">

		</div>
		<br><jsp:include page="../showMessage.jsp"><jsp:param name="key" value="xml_file"/></jsp:include>
	</td>
</tr>
<input type="hidden" name="crfId" value="<c:out value="${version.crfId}"/>">


</table>
</div>
</div></div></div></div></div></div></div></div>
</div>

<br clear="all">
<input type="submit" value="<fmt:message key="submit" bundle="${resword}"/>" class="button_long" onclick="javascript:alert('After submit,you can come back later to check the status and details in log file')">
<input type="button" onclick="goBack()"  name="cancel" value="<fmt:message key="cancel" bundle="${resword}"/>" class="button_medium"/>

</form>
<br/>
<div class="homebox_bullets">
    <a href="ImportRule?action=downloadUploadMappingTemplate"><b><fmt:message key="download_mapping_template" bundle="${resword}"/></b></a>
    <fmt:message key="open_in_text_editor_after_downloading" bundle="${resword}"/>
</div>
<br/>
<div class="homebox_bullets">
    <a href="Jobs"><b><fmt:message key="view_bulk_action_log" bundle="${resword}"/></b></a>
</div>
<br/>

<div class="homebox_bullets" id="view_logs">
    <a href="#" onclick="toggle_visibility('old-logs-table');"><b>View old logs</a>
</div>
<div class="homebox_bullets" id="hide_logs" style="display:none;">
    <a href="#" onclick="toggle_visibility('old-logs-table');"><b>Hide old logs</a>
</div>

<div id="old-logs-table" style="display:none;">
<%

    final Logger logger = LoggerFactory.getLogger(getClass().getName());
    ImportDataHelper importDataHelper = new ImportDataHelper();
	  String [] fileNames = null;
	  String fname=null;
    ArrayList<File> fileList = importDataHelper.getPersonalImportLogFile(request,null);

    boolean logsExist = false;
    if (fileList.size()>0) {
      logsExist = true;
    }


	BufferedReader readReport;
	int i = 0;
    int num=0;

    {
        %>
        <table name="reports">
        <th width=12.5% align="center" bgcolor="gray">Log File Name</th>
        <th width=12.5% align="center" bgcolor="gray">File Size</th>
        <th width=12.5% align="center" bgcolor="gray">Updated Time</th>
        <th width=12.5% align="center" bgcolor="gray">Action</th>
        <%
    }

    for (File file:fileList)
    {

        if(!file.isDirectory())
            {
            fname = file.getName();
            int pos = file.getParent().indexOf("import");
			String path = file.getParent().substring(pos+7);

			pos = path.lastIndexOf(File.separatorChar);
            String parentNm = pos < 0 || pos == path.length() ? "" : path.substring(pos + 1);
			String studyId = pos < 0 || pos == path.length() ? "" : path.substring(0,pos);


            if(fname.endsWith("_log.csv"))
            {

                    {
                        %>
                            <tr bgcolor="lightgray">
                                <td width=12.5% align="center">
                                    <%=fname%>
                                </td>

                                <td width=12.5% align="center">
                                        <%=file.length()%>
                                </td>

                                <td width=12.5%  align="center">
                                        <%=new java.util.Date(file.lastModified())%>
                                </td>

                                <td width=12.5%  align="center">
                                    <%
                                    long passTime = System.currentTimeMillis() - file.lastModified();

                                    if(passTime >= 5000){

                                    %>
                                    <a target="_new" href="UploadCRFData?fromUrl=UploadCRFData&action=download&fileId=<%=URLEncoder.encode(fname,"UTF-8")%>&studyId=<%=studyId%>&parentNm=<%=parentNm%>">
                                        <span name="bt_Download1" class="icon icon-download" border="0" align="left" hspace="6"
                                             alt="<fmt:message key="download" bundle="${resword}"/>" title="<fmt:message key="download" bundle="${resword}"/>">
                                    </a>
                                    <a href="UploadCRFData?fromUrl=UploadCRFData&action=delete&fileId=<%=URLEncoder.encode(fname,"UTF-8")%>&studyId=<%=studyId%>&parentNm=<%=parentNm%>">
                                        <span name="bt_Delete1" class="icon icon-trash red" border="0" alt="<fmt:message key="delete" bundle="${resword}"/>"
                                             title="<fmt:message key="delete" bundle="${resword}"/>" align="left" hspace="6"
                                             onClick='return confirm("Please confirm that you want to delete log file <%=fname%>");'>
                                    </a>
                                    <%
                                    } else{
                                    %>
                                    processing...
                                    <%}%>
                                </td>
                            </tr>
                        <%
                    }
                }
        }
    }
    {%></table> <%}
%>
</div>


<jsp:include page="../include/footer.jsp"/>

<script type="text/javascript" language="javascript">
    logsExist();
    function logsExist() {
      var oldLogsExist = "<%=logsExist%>";
      if (oldLogsExist == "false") {
        document.getElementById('view_logs').hide();
      }
    }
    function toggle_visibility(id) {
       var e = document.getElementById(id);
       if (e.style.display == 'none') {
          e.style.display = 'block';
          document.getElementById('hide_logs').show();
          document.getElementById('view_logs').hide();
       } else {
          e.style.display = 'none';
          document.getElementById('hide_logs').hide();
          document.getElementById('view_logs').show();
       }

    }
</script>
