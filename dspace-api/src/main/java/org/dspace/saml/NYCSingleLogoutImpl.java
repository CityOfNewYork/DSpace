package org.dspace.saml;

import org.joda.time.DateTime;
import org.opensaml.common.SAMLException;
import org.opensaml.common.SAMLObject;
import org.opensaml.saml2.core.*;
import org.opensaml.xml.encryption.DecryptionException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.SAMLStatusException;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.util.SAMLUtil;
import org.springframework.security.saml.websso.SingleLogoutProfileImpl;

import java.util.Iterator;

public class NYCSingleLogoutImpl extends SingleLogoutProfileImpl {

    @Override
    public boolean processLogoutRequest(SAMLMessageContext context, SAMLCredential credential) throws SAMLException {
        SAMLObject message = context.getInboundSAMLMessage();
        if (message != null && message instanceof LogoutRequest) {
            LogoutRequest logoutRequest = (LogoutRequest)message;
            if (!context.isInboundSAMLMessageAuthenticated() && context.getLocalExtendedMetadata().isRequireLogoutRequestSigned()) {
                throw new SAMLStatusException("urn:oasis:names:tc:SAML:2.0:status:RequestDenied", "LogoutRequest is required to be signed by the entity policy");
            } else {
                try {
                    this.verifyEndpoint(context.getLocalEntityEndpoint(), logoutRequest.getDestination());
                } catch (SAMLException var14) {
                    throw new SAMLStatusException("urn:oasis:names:tc:SAML:2.0:status:RequestDenied", "Destination of the LogoutRequest does not match any of the single logout endpoints");
                }

                try {
                    if (logoutRequest.getIssuer() != null) {
                        Issuer issuer = logoutRequest.getIssuer();
                        this.verifyIssuer(issuer, context);
                    }
                } catch (SAMLException var13) {
                    throw new SAMLStatusException("urn:oasis:names:tc:SAML:2.0:status:RequestDenied", "Issuer of the LogoutRequest is unknown");
                }

                DateTime time = logoutRequest.getIssueInstant();
                if (!SAMLUtil.isDateTimeSkewValid(this.getResponseSkew(), time)) {
                    throw new SAMLStatusException("urn:oasis:names:tc:SAML:2.0:status:Requester", "LogoutRequest issue instant is either too old or with date in the future");
                } else if (credential == null) {
                    throw new SAMLStatusException("urn:oasis:names:tc:SAML:2.0:status:UnknownPrincipal", "No user is logged in");
                } else {
                    boolean indexFound = false;
                    if (logoutRequest.getSessionIndexes() != null && logoutRequest.getSessionIndexes().size() > 0) {
                        Iterator var7 = credential.getAuthenticationAssertion().getAuthnStatements().iterator();

                        label71:
                        while(true) {
                            String statementIndex;
                            do {
                                if (!var7.hasNext()) {
                                    break label71;
                                }

                                AuthnStatement statement = (AuthnStatement)var7.next();
                                statementIndex = statement.getSessionIndex();
                            } while(statementIndex == null);

                            Iterator var10 = logoutRequest.getSessionIndexes().iterator();

                            while(var10.hasNext()) {
                                SessionIndex index = (SessionIndex)var10.next();
                                if (statementIndex.equals(index.getSessionIndex())) {
                                    indexFound = true;
                                }
                            }
                        }
                    } else {
                        indexFound = true;
                    }

                    if (!indexFound) {
                        return true;
                    } else {
                        try {
                            NameID nameID = this.getNameID(context, logoutRequest);
                            if (nameID != null && this.equalsNameID(credential.getNameID(), nameID)) {
                                return true;
                            } else {
                                throw new SAMLStatusException("urn:oasis:names:tc:SAML:2.0:status:UnknownPrincipal", "The requested NameID is invalid");
                            }
                        } catch (DecryptionException var12) {
                            throw new SAMLStatusException("urn:oasis:names:tc:SAML:2.0:status:Responder", "The NameID can't be decrypted", var12);
                        }
                    }
                }
            }
        } else {
            throw new SAMLException("Message is not of a LogoutRequest object type");
        }
    }

    private boolean equalsNameID(NameID a, NameID b) {
        boolean equals = !this.differ(a.getSPProvidedID(), b.getSPProvidedID());
        equals = equals && !this.differ(a.getValue(), b.getValue());
        equals = equals && !this.differ(a.getFormat(), b.getFormat());
        equals = equals && !this.differ(a.getNameQualifier(), b.getNameQualifier());
        equals = equals && !this.differ(a.getSPNameQualifier(), b.getSPNameQualifier());
        equals = equals && !this.differ(a.getSPProvidedID(), b.getSPProvidedID());
        return equals;
    }

    private boolean differ(Object a, Object b) {
        if (a == null) {
            return b != null;
        } else {
            return !a.equals(b);
        }
    }
}
