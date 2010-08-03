package org.atricore.idbus.capabilities.samlr2.support.federation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.kernel.main.federation.IdentityMapper;
import org.atricore.idbus.kernel.main.federation.SubjectNameID;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

/**
 *  The mapped subject contains remote sujbect information 
 *
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class RemoteSubjectIdentityMapper implements IdentityMapper {

    private static final Log logger = LogFactory.getLog(RemoteSubjectIdentityMapper.class);

    private boolean useLocalId = false;

    public boolean isUseLocalId() {
        return useLocalId;
    }

    public void setUseLocalId(boolean useLocalId) {
        this.useLocalId = useLocalId;
    }

    public Subject map(Subject remoteSubject, Subject localSubject) {

        Subject federatedSubject = null;
        Set<Principal> merged = new HashSet<Principal>();

        if (useLocalId) {
            Set<SubjectNameID> subjectNameID = localSubject.getPrincipals(SubjectNameID.class);
            // federated subject is identified using local account name identifier
            for (SubjectNameID sc : subjectNameID) {
                merged.add(sc);
            }

        } else {
            Set<SubjectNameID> subjectNameID = remoteSubject.getPrincipals(SubjectNameID.class);
            // federated subject is identified using local account name identifier
            for (SubjectNameID sc : subjectNameID) {
                merged.add(sc);
            }
        }

        Set<Principal> principals = remoteSubject.getPrincipals();
        for (Principal p : principals) {
            if (p instanceof SubjectNameID)
                continue;

            merged.add(p);
        }

        federatedSubject = new Subject(true, merged,
                localSubject.getPublicCredentials(),
                localSubject.getPrivateCredentials());

        return federatedSubject;
    }
}
