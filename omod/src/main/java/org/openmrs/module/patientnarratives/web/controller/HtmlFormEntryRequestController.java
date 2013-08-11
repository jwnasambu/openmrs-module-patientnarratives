package org.openmrs.module.patientnarratives.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.htmlformentry.*;
import org.openmrs.web.WebConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

@Controller
public class HtmlFormEntryRequestController {

    Boolean alert = null;
    private String returnUrl;
    public final static String FORM_PATH = "/module/patientnarratives/htmlFormProcess.form";
    protected final Log log = LogFactory.getLog(getClass());

    @RequestMapping(FORM_PATH)
    public ModelAndView handleRequest(HttpServletRequest request) throws Exception {

        returnUrl = request.getContextPath() + "/module/patientnarratives/patientNarrativesForm.form";

        HtmlFormEntryPortletController htmlFormEntryPortletController = new HtmlFormEntryPortletController();
        FormEntrySession session = htmlFormEntryPortletController.getFormEntrySession(request);

        try {
            List<FormSubmissionError> validationErrors = session.getSubmissionController().validateSubmission(session.getContext(), request);
            if (validationErrors != null && validationErrors.size() > 0) {
//                errors.reject("Fix errors");
            }
        } catch (Exception ex) {
            log.error("Exception during form validation", ex);
//            errors.reject("Exception during form validation, see log for more details: " + ex);
        }

//        if (errors.hasErrors()) {
//            return new ModelAndView(FORM_PATH, "command", session);
//        }

        // no form validation errors, proceed with submission

        session.prepareForSubmit();

        if (session.getContext().getMode() == FormEntryContext.Mode.ENTER && session.hasPatientTag() && session.getPatient() == null
                && (session.getSubmissionActions().getPersonsToCreate() == null || session.getSubmissionActions().getPersonsToCreate().size() == 0))
            throw new IllegalArgumentException("This form is not going to create an Patient");

        if (session.getContext().getMode() == FormEntryContext.Mode.ENTER && session.hasEncouterTag() && (session.getSubmissionActions().getEncountersToCreate() == null || session.getSubmissionActions().getEncountersToCreate().size() == 0))
            throw new IllegalArgumentException("This form is not going to create an encounter");

        try {
            session.getSubmissionController().handleFormSubmission(session, request);
            HtmlFormEntryUtil.getService().applyActions(session);
            String successView = session.getReturnUrlWithParameters();

            request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ATTR, "patientnarratives.module.narrative.save.success");

            if (successView == null)
                successView = request.getContextPath() + "/module/patientnarratives/patientNarrativesForm.form";
            if (StringUtils.hasText(request.getParameter("closeAfterSubmission"))) {
                return new ModelAndView(new RedirectView(returnUrl));
//                return new ModelAndView(closeDialogView, "dialogToClose", request.getParameter("closeAfterSubmission"));
            } else {
                return new ModelAndView(new RedirectView(returnUrl));
//                return new ModelAndView(new RedirectView(successView));
            }
        } catch (ValidationException ex) {
            log.error("Invalid input:", ex);
//            errors.reject(ex.getMessage());
        } catch (BadFormDesignException ex) {
            log.error("Bad Form Design:", ex);
//            errors.reject(ex.getMessage());
        } catch (Exception ex) {
            log.error("Exception trying to submit form", ex);
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
//            errors.reject("Exception! " + ex.getMessage() + "<br/>" + sw.toString());
        }


//        if ((alert != null) && (alert == true)) {
//        }
        // if we get here it's because we caught an error trying to submit/apply
//        return new ModelAndView(returnUrl, "command", session);
        return new ModelAndView(new RedirectView(returnUrl));
    }

}