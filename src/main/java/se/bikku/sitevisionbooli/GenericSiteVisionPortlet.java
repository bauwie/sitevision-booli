package se.bikku.sitevisionbooli;

import org.apache.commons.io.FileUtils;
import org.apache.portals.bridges.velocity.GenericVelocityPortlet;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import senselogic.sitevision.api.Utils;
import senselogic.sitevision.api.security.PermissionUtil;

import javax.portlet.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Locale;

/**
 * This class adds default behavior for processAction and custom SiteVision specific mode: config.
 * Uses Velocity templates for rendering.
 * The config mode has the possibility to change the view template.
 */
public class GenericSiteVisionPortlet extends GenericVelocityPortlet
{
  /**
   * Adds support for SiteVision specific portlet mode.
   *
   * * doConfig for handling config requests
   *
   * Only dispatch to config if the current user has write permission. 
   *
   * @param renderRequest
   * @param renderResponse
   * @throws PortletException
   * @throws java.io.IOException
   */
  @Override
  protected final void doDispatch(RenderRequest renderRequest, RenderResponse renderResponse)
    throws PortletException, IOException
  {
    // Handle the custom portlet mode CONFIG that SiteVision uses.
    if ("config".equals(renderRequest.getPortletMode().toString()))
    {
      if(!hasWritePermission(renderRequest)) return;
      doConfig(renderRequest, renderResponse);
    }
    else
    {
      super.doDispatch(renderRequest, renderResponse);
    }
  }

  /**
   * Helper method to save all preferences. Returns to portlet mode view.
   *
   * Only saves if the current user has writer permisson.
   *
   * @param actionRequest
   * @param actionResponse
   * @throws PortletException
   * @throws java.io.IOException
   */
  @Override
  public void processAction(ActionRequest actionRequest, ActionResponse actionResponse)
    throws PortletException, IOException
  {
    if(hasWritePermission(actionRequest))
    {
      PortletPreferences prefs = actionRequest.getPreferences();
      for (Enumeration enm = prefs.getNames(); enm.hasMoreElements();)
      {
        String name = (String) enm.nextElement();
        String newValue = actionRequest.getParameter(name);
        if (newValue != null)
        {
          prefs.setValue(name, newValue);
        }
      }

      prefs.store();
      actionResponse.setPortletMode(PortletMode.VIEW);
    }
  }

  /**
   * Helper method to serve up the config mode.
   * Reads in the common properties useCustomTemplate and customTemplate.
   * Defaults to the view template if no custom template is selected
   * This is a custom SiteVision specific portlet mode.
   *
   * @param renderRequest
   * @param renderResponse
   * @throws PortletException
   * @throws java.io.IOException
   */
  public void doConfig(RenderRequest renderRequest, RenderResponse renderResponse)
    throws PortletException, IOException
  {
    PortletPreferences prefs = renderRequest.getPreferences();
    Boolean useCustomTemplate = ("true".equals(prefs.getValue("useCustomTemplate", "false")));
    String template = "";

    Locale locale = renderRequest.getLocale();
    //ResourceBundle bundle = ResourceBundle.getBundle("se.bikku.language", locale);

    if (useCustomTemplate)
    {
      template = prefs.getValue("customTemplate", "tes");
    }
    else
    {
      PortletConfig config = getPortletConfig();
      String viewPage = config.getInitParameter("ViewPage");
      String filename = getPortletContext().getRealPath(viewPage);
      template = FileUtils.readFileToString(new File(filename));
    }

    String configjsPath = renderRequest.getContextPath() + "/js/config.js";
    String jqueryPath = renderRequest.getContextPath() + "/js/jquery.js";
    configjsPath = renderResponse.encodeURL(configjsPath);
    jqueryPath= renderResponse.encodeURL(jqueryPath);

    Context context = getContext(renderRequest);
    context.put("useCustomTemplate", useCustomTemplate);
    context.put("customTemplate", template);
    context.put("configjsPath", configjsPath);
    context.put("jqueryPath", jqueryPath);
    //context.put("language", bundle);

    super.doCustom(renderRequest, renderResponse);
  }

  /**
   * Adds the language bundle to the context
   * 
   * @param renderRequest
   * @param renderResponse
   * @throws PortletException
   * @throws java.io.IOException
   */
  @Override
  public void doView(RenderRequest renderRequest, RenderResponse renderResponse) throws PortletException, IOException
  {
    Locale locale = renderRequest.getLocale();
    //ResourceBundle bundle = ResourceBundle.getBundle("se.hj.language", locale);

    Context context = getContext(renderRequest);
    //context.put("language", bundle);

    PrintWriter writer = renderResponse.getWriter();
    PortletPreferences prefs = renderRequest.getPreferences();

    Boolean useCustomTemplate = ("true".equals(prefs.getValue("useCustomTemplate", "false")));
    if (useCustomTemplate)
    {
      String templateStr = prefs.getValue("customTemplate", "");
      Velocity.evaluate(context, writer, "customTemplate", templateStr);
    }
    else
    {
      super.doView(renderRequest, renderResponse);
    }
  }

  /**
   * Checks if current user has write permission on current page (write permissions == permissions to write/update shared portlet preferences).
   *
   * Note! This method uses SiteVision Public API functionality only available in SiteVision 2.6 and later.
   * If executed in previous versions (SiteVision 2.5 etc.) an AbstractMethodError or such will occur.
   *
   * @param portletRequest current request (RenderRequest or ActionRequest)
   * @return true if current user has read permissions on current page, false otherwise
   */
  protected final boolean hasWritePermission(PortletRequest portletRequest)
  {
    Utils siteVisionUtils = (Utils) portletRequest.getAttribute("sitevision.utils");
    PermissionUtil permissionUtil = siteVisionUtils.getPermissionUtil();

    return permissionUtil.hasWritePermission();
  }
}
