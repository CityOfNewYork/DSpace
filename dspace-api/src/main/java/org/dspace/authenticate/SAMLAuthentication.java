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

    /**
     *
     * @param context
     *  DSpace context
     *
     * @param request
     *  HTTP request, in case it's needed. May be null.
     *
     * @param username
     *  Username, if available.  May be null.
     *
     * @return true
     *
     * @throws SQLException
     */
    @Override
    public boolean canSelfRegister(Context context,
                                   HttpServletRequest request,
                                   String username)
        throws SQLException {
        return true;
    }

    /**
     * Nothing here, initialization is done when auto-registering.
     *
     * @throws SQLException if database error
     */
    @Override
    public void initEPerson(Context context,
                            HttpServletRequest request,
                            EPerson eperson)
        throws SQLException {
        // We don't do anything because all our work is done in authenticate
    }

    /**
     * We never allow the user to change their password.
     *
     * @throws SQLException if database error
     */
    @Override
    public boolean allowSetPassword(Context context,
                                    HttpServletRequest request,
                                    String username)
        throws SQLException {
        return false;
    }

    /**
     * This is an explicit method, since it needs username and password
     * from some source.
     *
     * @return false
     */
    @Override
    public boolean isImplicit() {
        return false;
    }

    /**
     * login.specialgroup property is not set for this class so we just
     * return an empty List.
     *
     * @return empty List
     */
    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request) {
        return ListUtils.EMPTY_LIST;
    }

    @Override
    public String loginPageTitle(Context context) {
        return "org.dspace.authenticate.SAMLAuthentication.title";
    }

    /**
     * Authenticate the user in dspace after successful SAML login.
     * SAML entities will be stored in SAMLCredential object inside Authentication.
     *
     * Query database if eperson exists by guid and userType.
     * If eperson does not exist, call registerNewEPerson method to create
     * new eperson object.
     * If eperson exist, call updateEPerson to update user attributes.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
     *
     * @param username
     *  null, we get username (or email) from Security Context.
     *
     * @param password
     *  Password for explicit auth, or null for implicit method.
     *
     * @param realm
     *  Realm is an extra parameter used by some authentication methods, leave null if
     *  not applicable.
     *
     * @param request
     *  The HTTP request that started this operation, or null if not applicable.
     *
     * @return One of:
     *   SUCCESS, NO_SUCH_USER, BAD_ARGS
     * <p>Meaning:
     * <br>SUCCESS         - authenticated OK.
     * <br>NO_SUCH_USER    - no EPerson with matching email address.
     * <br>BAD_ARGS        - user matched but cannot login.
     * @throws SQLException if database error
     */
    @Override
    public int authenticate(Context context,
                            String username,
                            String password,
                            String realm,
                            HttpServletRequest request)
        throws SQLException {
        // Get credential object for user attributes
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SAMLCredential credential = (SAMLCredential) authentication.getCredentials();

        // Check if eperson exists by guid and userType
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
            log.error("Unable to successfully authenticate using SAML because of an exception.", e);
            context.setCurrentUser(null);
            return NO_SUCH_USER;
        }
    }


    /**
     * When user accesses a authenticated required page, return to 404 page instead
     * of a login page. This method prevents users from accessing a login page.
     *
     * @param context
     *  DSpace context.
     *
     * @param request
     *  The HTTP request that started this operation, or null if not applicable.
     *
     * @param response
     *  The HTTP response from the servlet method.
     *
     * @return fully-qualified URL to /error/404.jsp
     */
    @Override
    public String loginPageURL (Context context,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        return response.encodeRedirectURL(request.getContextPath() + "/error/404.jsp");
    }

    /**
     * Register a new eperson object. This method is called when no existing user was
     * found for the guid and userType and autoregister is enabled. When these conditions
     * are met this method will create a new eperson object.
     *
     *
     *
     * @param context
     *  The current DSpace database context
     *
     * @param credential
     *  The object that contains the SAML entities.
     *
     * @param request
     *  The HTTP request that started this operation.
     *
     * @return eperson
     * @throws SQLException if database error
     * @throws AuthorizeException
     */
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

    /**
     * Update specified eperson with the information provided from IDP as the
     * attributes may have changed since the last time they logged in.
     *
     * @param context
     *  The current DSpace database context.
     *
     * @param credential
     *  The object that contains the SAML entities.
     *
     * @param eperson
     *  The eperson object to update.
     *
     * @throws SQLException if database error
     * @throws AuthorizeException
     */
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
