package org.dspace.authenticate;

import org.apache.commons.collections.ListUtils;
import org.apache.log4j.Logger;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.SAMLCredential;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.List;

public class SAMLAuthentication implements AuthenticationMethod {

    /** log4j category */
    private static Logger log = Logger.getLogger(SAMLAuthentication.class);

    private final transient AuthenticationService authenticationService
            = AuthenticateServiceFactory.getInstance().getAuthenticationService();
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    @Override
    public boolean canSelfRegister(Context context,
                                   HttpServletRequest request,
                                   String username)
        throws SQLException {
        return true;
    }

    @Override
    public void initEPerson(Context context,
                            HttpServletRequest request,
                            EPerson eperson)
        throws SQLException {
        // We don't do anything because all our work is done in authenticate
    }

    @Override
    public boolean allowSetPassword(Context context,
                                    HttpServletRequest request,
                                    String username)
        throws SQLException {
        return false;
    }

    @Override
    public boolean isImplicit() {
        return false;
    }

    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request) {
        return ListUtils.EMPTY_LIST;
    }

    @Override
    public String loginPageTitle(Context context) {
        return "org.dspace.authenticate.SAMLAuthentication.title";
    }

    @Override
    public int authenticate(Context context,
                            String username,
                            String password,
                            String realm,
                            HttpServletRequest request)
        throws SQLException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SAMLCredential credential = (SAMLCredential) authentication.getCredentials();

        EPerson eperson = null;
        eperson = ePersonService.findByGuidAndUserType(context,
                credential.getAttributeAsString("GUID"),
                credential.getAttributeAsString("userType"));

        try {
            if (eperson != null) {
                if (!eperson.canLogIn()) {
                    return BAD_ARGS;
                } else {
                    updateEPerson(context, credential, eperson);
                }
            } else {
                eperson = registerNewEPerson(context, credential, request);
            }

            context.setCurrentUser(eperson);
            return SUCCESS;

        } catch (AuthorizeException e) {
            return NO_SUCH_USER;
        }
    }


    @Override
    public String loginPageURL (Context context,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        return response.encodeRedirectURL(request.getContextPath() + "/error/404.jsp");
    }

    private EPerson registerNewEPerson(Context context, SAMLCredential credential, HttpServletRequest request)
            throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();

        EPerson eperson = ePersonService.create(context);
        eperson.setEmail(credential.getAttributeAsString("mail"));
        eperson.setGuid(context, credential.getAttributeAsString("GUID"));
        eperson.setFirstName(context, credential.getAttributeAsString("givenName"));
        eperson.setLastName(context, credential.getAttributeAsString("sn"));
        eperson.setUserType(context, credential.getAttributeAsString("userType"));
        eperson.setCanLogIn(true);
        authenticationService.initEPerson(context, request, eperson);
        ePersonService.update(context, eperson);
        context.dispatchEvents();
        context.restoreAuthSystemState();
        return eperson;
    }

    private void updateEPerson(Context context, SAMLCredential credential, EPerson eperson)
            throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();

        eperson.setGuid(context, credential.getAttributeAsString("GUID"));
        eperson.setFirstName(context, credential.getAttributeAsString("givenName"));
        eperson.setLastName(context, credential.getAttributeAsString("sn"));
        eperson.setUserType(context, credential.getAttributeAsString("userType"));

        ePersonService.update(context, eperson);
        context.dispatchEvents();
        context.restoreAuthSystemState();
    }
}
